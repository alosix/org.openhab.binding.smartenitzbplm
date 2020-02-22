/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.smartenitzbplm.internal.message;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddress;
import org.openhab.binding.smartenitzbplm.internal.utils.Utils;
import org.openhab.binding.smartenitzbplm.internal.utils.Utils.ParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains an Insteon Message consisting of the raw data, and the message definition.
 * For more info, see the public Insteon Developer's Guide, 2nd edition,
 * and the Insteon Modem Developer's Guide.
 *
 * @author Bernd Pfrommer
 * @author Daniel Pfrommer
 * @since 1.5.0
 */

public class Msg {
    private static final Logger logger = LoggerFactory.getLogger(Msg.class);

    /**
     * Represents the direction of the message from the host's view.
     * The host is the machine to which the modem is attached.
     */
    public enum Direction {
        TO_MODEM("TO_MODEM"),
        FROM_MODEM("FROM_MODEM");

        private static HashMap<String, Direction> s_map = new HashMap<String, Direction>();

        private String m_directionString;

        static {
            s_map.put(TO_MODEM.getDirectionString(), TO_MODEM);
            s_map.put(FROM_MODEM.getDirectionString(), FROM_MODEM);
        }

        Direction(String dirString) {
            m_directionString = dirString;
        }

        public String getDirectionString() {
            return m_directionString;
        }

        public static Direction s_getDirectionFromString(String dir) {
            return s_map.get(dir);
        }
    }

    // has the structure of all known messages
    private static final HashMap<String, Msg> msgMap = new HashMap<String, Msg>();
    // maps between command number and the length of the header
    private static final HashMap<Integer, Integer> headerMap = new HashMap<Integer, Integer>();
    // has templates for all message from modem to host
    private static final HashMap<Integer, Msg> replyMap = new HashMap<Integer, Msg>();

    private int m_headerLength = -1;
    private byte[] m_data = null;
    private MsgDefinition m_definition = new MsgDefinition();
    private Direction m_direction = Direction.TO_MODEM;
    private long m_quietTime = 0;
    private String name = null;

    /**
     * Constructor
     * 
     * @param headerLength length of message header (in bytes)
     * @param data byte array with message
     * @param dataLength length of byte array data (in bytes)
     * @param dir direction of the message (from/to modem)
     */
    public Msg(int headerLength, byte[] data, int dataLength, Direction dir) {
        m_headerLength = headerLength;
        m_direction = dir;
        initialize(data, 0, dataLength);
    }

    /**
     * Copy constructor, needed to make a copy of the templates when
     * generating messages from them.
     * 
     * @param m the message to make a copy of
     */
    public Msg(Msg m) {
    	this.name = m.name;
        m_headerLength = m.m_headerLength;
        m_data = m.m_data.clone();
        // the message definition usually doesn't change, but just to be sure...
        m_definition = new MsgDefinition(m.m_definition);
        m_direction = m.m_direction;
    }

    static {
        // Use xml msg loader to load configs
        try {
            InputStream stream = Msg.class.getResourceAsStream("/msg_definitions.xml");
            if (stream != null) {
                Map<String, Msg> msgs = XMLMessageReader.readMessageDefinitions(stream);
                msgMap.putAll(msgs);
            } else {
                logger.error("could not get message definition resource!");
            }
        } catch (IOException e) {
            logger.error("i/o error parsing xml insteon message definitions", e);
        } catch (ParsingException e) {
            logger.error("parse error parsing xml insteon message definitions", e);
        } catch (FieldException e) {
            logger.error("got field exception while parsing xml insteon message definitions", e);
        }
        buildHeaderMap();
        buildLengthMap();
    }

    //
    // ------------------ simple getters and setters -----------------
    //

    /**
     * Experience has shown that if Insteon messages are sent in close succession,
     * only the first one will make it. The quiet time parameter says how long to
     * wait after a message before the next one can be sent.
     * 
     * @return the time (in milliseconds) to pause after message has been sent
     */
    public long getQuietTime() {
        return m_quietTime;
    }

    public byte[] getData() {
        return m_data;
    }

    public int getLength() {
        return m_data.length;
    }

    public int getHeaderLength() {
        return m_headerLength;
    }

    public Direction getDirection() {
        return m_direction;
    }

    public MsgDefinition getDefinition() {
        return m_definition;
    }

    public byte getCommandNumber() {
        return ((m_data == null || m_data.length < 2) ? -1 : m_data[1]);
    }

    public boolean isPureNack() {
        return (m_data.length == 2 && m_data[1] == 0x15);
    }

    public boolean isExtended() {
        if (m_data == null || getLength() < 2) {
            return false;
        }
        if (m_definition == null) {
            return false;
        }
        if (!m_definition.containsField("messageFlags")) {
            return (false);
        }
        try {
            byte flags = getByte("messageFlags");
            return ((flags & 0x10) == 0x10);
        } catch (FieldException e) {
            // do nothing
        }
        return false;
    }

    public boolean isUnsolicited() {
        // if the message has an ACK/NACK, it is in response to our message,
        // otherwise it is out-of-band, i.e. unsolicited
        return (m_definition != null) && (!m_definition.containsField("ACK/NACK"));
    }

    public boolean isEcho() {
        return isPureNack() || !isUnsolicited();
    }

    public boolean isOfType(MsgType mt) {
        try {
            MsgType t = MsgType.s_fromValue(getByte("messageFlags"));
            return (t == mt);
        } catch (FieldException e) {
            return false;
        }
    }

    public boolean isBroadcast() {
        return isOfType(MsgType.ALL_LINK_BROADCAST) || isOfType(MsgType.BROADCAST);
    }

    public boolean isCleanup() {
        return isOfType(MsgType.ALL_LINK_CLEANUP);
    }

    public boolean isAllLink() {
        return isOfType(MsgType.ALL_LINK_BROADCAST) || isOfType(MsgType.ALL_LINK_CLEANUP);
    }

    public boolean isAckOfDirect() {
        return isOfType(MsgType.ACK_OF_DIRECT);
    }

    public boolean isAllLinkCleanupAckOrNack() {
        return isOfType(MsgType.ALL_LINK_CLEANUP_ACK) || isOfType(MsgType.ALL_LINK_CLEANUP_NACK);
    }

    public boolean isX10() {
        try {
            int cmd = getByte("Cmd") & 0xff;
            if (cmd == 0x63 || cmd == 0x52) {
                return true;
            }
        } catch (FieldException e) {
        }
        return false;
    }

    public void setDefinition(MsgDefinition d) {
        m_definition = d;
    }

    public void setQuietTime(long t) {
        m_quietTime = t;
    }

    public void addField(Field f) {
        if (m_definition == null) {
            return;
        }
        m_definition.addField(f);
    }

    public DeviceAddress getAddr(String name) {
        if (m_definition == null) {
            return null;
        }
        try {
            return m_definition.getField(name).getAddress(m_data);
        } catch (FieldException e) {
            // do nothing, we'll return null
        }
        return null;
    }

    public int getHopsLeft() throws FieldException {
        int hops = (getByte("messageFlags") & 0x0c) >> 2;
        return hops;
    }

    /**
     * Will initialize the message with a byte[], an offset, and a length
     * 
     * @param data the src byte array
     * @param offset the offset in the src byte array
     * @param len the length to copy from the src byte array
     */
    private void initialize(byte[] data, int offset, int len) {
        m_data = new byte[len];
        if (offset >= 0 && offset < data.length) {
            System.arraycopy(data, offset, m_data, 0, len);
        } else {
            logger.error("intialize(): Offset out of bounds!");
        }
    }

    /**
     * Will put a byte at the specified key
     * 
     * @param key the string key in the message definition
     * @param value the byte to put
     */
    public void setByte(String key, byte value) throws FieldException {
        Field f = m_definition.getField(key);
        f.setByte(m_data, value);
    }

    /**
     * Will put an int at the specified field key
     * 
     * @param key the name of the field
     * @param value the int to put
     */
    public void setInt(String key, int value) throws FieldException {
        Field f = m_definition.getField(key);
        f.setInt(m_data, value);
    }

    /**
     * Will put address bytes at the field
     * 
     * @param key the name of the field
     * @param adr the address to put
     */
    public void setAddress(String key, DeviceAddress adr) throws FieldException {
        Field f = m_definition.getField(key);
        f.setAddress(m_data, adr);
    }

    /**
     * Will fetch a byte
     * 
     * @param key the name of the field
     * @return the byte
     */
    public byte getByte(String key) throws FieldException {
        if (m_definition == null) {
            throw new FieldException("no msg definition!");
        }
        return (m_definition.getField(key).getByte(m_data));
    }

    /**
     * Will fetch a byte array starting at a certain field
     * 
     * @param key the name of the first field
     * @param number of bytes to get
     * @return the byte array
     */
    public byte[] getBytes(String key, int numBytes) throws FieldException {
        if (m_definition == null) {
            throw new FieldException("no msg definition!");
        }
        int offset = m_definition.getField(key).getOffset();
        if (offset < 0 || offset + numBytes > m_data.length) {
            throw new FieldException("data index out of bounds! key:" + key);
        }
        byte[] section = new byte[numBytes];
        System.arraycopy(m_data, offset, section, 0, numBytes);
        return section;
    }

    /**
     * Will fetch address from field
     * 
     * @param field the filed name to fetch
     * @return the address
     */
    public DeviceAddress getAddress(String field) throws FieldException {
        if (m_definition == null) {
            throw new FieldException("no msg definition!");
        }
        return (m_definition.getField(field).getAddress(m_data));
    }

    /**
     * Fetch 3-byte (24bit) from message
     * 
     * @param key1 the key of the msb
     * @param key2 the key of the second msb
     * @param key3 the key of the lsb
     * @return the integer
     */
    public int getInt24(String key1, String key2, String key3) throws FieldException {
        if (m_definition == null) {
            throw new FieldException("no msg definition!");
        }
        int i = (m_definition.getField(key1).getByte(m_data) << 16) & (m_definition.getField(key2).getByte(m_data) << 8)
                & m_definition.getField(key3).getByte(m_data);
        return i;
    }

    public String toHexString() {
        if (m_data != null) {
            return Utils.getHexString(m_data);
        }
        return super.toString();
    }

    /**
     * Sets the userData fields from a byte array
     * 
     * @param data
     */
    public void setUserData(byte[] arg) {
        byte[] data = Arrays.copyOf(arg, 14); // appends zeros if short
        try {
            setByte("userData1", data[0]);
            setByte("userData2", data[1]);
            setByte("userData3", data[2]);
            setByte("userData4", data[3]);
            setByte("userData5", data[4]);
            setByte("userData6", data[5]);
            setByte("userData7", data[6]);
            setByte("userData8", data[7]);
            setByte("userData9", data[8]);
            setByte("userData10", data[9]);
            setByte("userData11", data[10]);
            setByte("userData12", data[11]);
            setByte("userData13", data[12]);
            setByte("userData14", data[13]);
        } catch (FieldException e) {
            logger.error("got field exception on msg {}:", e);
        }
    }

    /**
     * Calculate and set the CRC with the older 1-byte method
     * 
     * @return the calculated crc
     */
    public int setCRC() {
        int crc;
        try {
            crc = getByte("command1") + getByte("command2");
            byte[] bytes = getBytes("userData1", 13); // skip userData14!
            for (byte b : bytes) {
                crc += b;
            }
            crc = ((~crc) + 1) & 0xFF;
            setByte("userData14", (byte) (crc & 0xFF));
        } catch (FieldException e) {
            logger.error("got field exception on msg {}:", this, e);
            crc = 0;
        }
        return crc;
    }

    /**
     * Calculate and set the CRC with the newer 2-byte method
     * 
     * @return the calculated crc
     */
    public int setCRC2() {
        int crc = 0;
        try {
            byte[] bytes = getBytes("command1", 14);
            for (int loop = 0; loop < bytes.length; loop++) {
                int b = bytes[loop] & 0xFF;
                for (int bit = 0; bit < 8; bit++) {
                    int fb = b & 0x01;
                    if ((crc & 0x8000) == 0) {
                        fb = fb ^ 0x01;
                    }
                    if ((crc & 0x4000) == 0) {
                        fb = fb ^ 0x01;
                    }
                    if ((crc & 0x1000) == 0) {
                        fb = fb ^ 0x01;
                    }
                    if ((crc & 0x0008) == 0) {
                        fb = fb ^ 0x01;
                    }
                    crc = ((crc << 1) | fb) & 0xFFFF;
                    b = b >> 1;
                }
            }
            setByte("userData13", (byte) ((crc >> 8) & 0xFF));
            setByte("userData14", (byte) (crc & 0xFF));
        } catch (FieldException e) {
            logger.error("got field exception on msg {}:", this, e);
            crc = 0;
        }
        return crc;
    }

    @Override
    public String toString() {
    	
        String s = (m_direction == Direction.TO_MODEM) ? "OUT:" : "IN:";
        if (m_definition == null || m_data == null) {
            return toHexString();
        }
        // need to first sort the fields by offset
        Comparator<Field> cmp = new Comparator<Field>() {
            @Override
            public int compare(Field f1, Field f2) {
                return f1.getOffset() - f2.getOffset();
            }
        };
        TreeSet<Field> fields = new TreeSet<Field>(cmp);
        for (Field f : m_definition.getFields().values()) {
            fields.add(f);
        }
        for (Field f : fields) {
            if (f.getName().equals("messageFlags")) {
                byte b;
                try {
                    b = f.getByte(m_data);
                    MsgType t = MsgType.s_fromValue(b);
                    s += f.toString(m_data) + "=" + t.toString() + ":" + (b & 0x03) + ":" + ((b & 0x0c) >> 2) + "|";
                } catch (FieldException e) {
                    logger.error("toString error: ", e);
                } catch (IllegalArgumentException e) {
                    logger.error("toString msg type error: ", e);
                }
            } else {
                s += f.toString(m_data) + "|";
            }
        }
        return name + "|" + s;
    }

    /**
     * Factory method to create Msg from raw byte stream received from the
     * serial port.
     * 
     * @param m_buf the raw received bytes
     * @param msgLen length of received buffer
     * @param isExtended whether it is an extended message or not
     * @return message, or null if the Msg cannot be created
     */
    public static Msg s_createMessage(byte[] m_buf, int msgLen, boolean isExtended) {
        if (m_buf == null || m_buf.length < 2) {
            return null;
        }
        Msg template = replyMap.get(cmdToKey(m_buf[1], isExtended));
        if (template == null) {
            return null; // cannot find lookup map
        }
        if (msgLen != template.getLength()) {
            logger.error("expected msg {} len {}, got {}", template.getCommandNumber(), template.getLength(), msgLen);
            return null;
        }
        Msg msg = new Msg(template.getHeaderLength(), m_buf, msgLen, Direction.FROM_MODEM);
        msg.setDefinition(template.getDefinition());
        msg.setName(template.getName());
        return (msg);
    }

    /**
     * Finds the header length from the insteon command in the received message
     * 
     * @param cmd the insteon command received in the message
     * @return the length of the header to expect
     */
    public static int getHeaderLength(byte cmd) {
        Integer len = headerMap.get(new Integer(cmd));
        if (len == null) {
            return (-1); // not found
        }
        return len;
    }

    /**
     * Tries to determine the length of a received Insteon message.
     * 
     * @param b Insteon message command received
     * @param isExtended flag indicating if it is an extended message
     * @return message length, or -1 if length cannot be determined
     */
    public static int getMessageLength(byte b, boolean isExtended) {
        int key = cmdToKey(b, isExtended);
        Msg msg = replyMap.get(key);
        if (msg == null) {
            return -1;
        }
        return msg.getLength();
    }

    /**
     * From bytes received thus far, tries to determine if an Insteon
     * message is extended or standard.
     * 
     * @param buf the received bytes
     * @param len the number of bytes received so far
     * @param headerLength the known length of the header
     * @return true if it is definitely extended, false if cannot be
     *         determined or if it is a standard message
     */
    public static boolean isExtended(byte[] buf, int len, int headerLength) {
        if (headerLength <= 2) {
            return false;
        } // extended messages are longer
        if (len < headerLength) {
            return false;
        } // not enough data to tell if extended
        byte flags = buf[headerLength - 1]; // last byte says flags
        boolean isExtended = (flags & 0x10) == 0x10; // bit 4 is the message
        return (isExtended);
    }

    /**
     * Creates Insteon message (for sending) of a given type
     * 
     * @param type the type of message to create, as defined in the xml file
     * @return reference to message created
     * @throws IOException if there is no such message type known
     */
    public static Msg makeMessage(String type) throws IOException {
        Msg m = msgMap.get(type);
        if (m == null) {
            throw new IOException("unknown message type: " + type);
        }
        return new Msg(m);
    }

    private static int cmdToKey(byte cmd, boolean isExtended) {
        return (cmd + (isExtended ? 256 : 0));
    }

    private static void buildHeaderMap() {
        for (Msg m : msgMap.values()) {
            if (m.getDirection() == Direction.FROM_MODEM) {
                headerMap.put(new Integer(m.getCommandNumber()), m.getHeaderLength());
            }
        }
    }

    private static void buildLengthMap() {
        for (Msg m : msgMap.values()) {
            if (m.getDirection() == Direction.FROM_MODEM) {
                Integer key = new Integer(cmdToKey(m.getCommandNumber(), m.isExtended()));
                replyMap.put(key, m);
            }
        }
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
