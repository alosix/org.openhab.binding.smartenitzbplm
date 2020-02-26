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

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;

import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes data coming from the serial port and turns it
 * into an message. For that, it has to figure out the length of the
 * message from the header, and read enough bytes until it hits the
 * message boundary. The code is tricky, partly because the Insteon protocol is.
 * Most of the time the command code (second byte) is enough to determine the length
 * of the incoming message, but sometimes one has to look deeper into the message
 * to determine if it is a standard or extended message (their lengths differ).
 *
 * @author Bernd Pfrommer
 * @since 1.5.0
 */
public class MsgFactory {
    private static final Logger logger = LoggerFactory.getLogger(MsgFactory.class);
    // no idea what the max msg length could be, but
    // I doubt it'll ever be larger than 4k
    private final static int MAX_MSG_LEN = 16384;
    private PipedInputStream pipedInputStream = new PipedInputStream(MAX_MSG_LEN);
    private BufferedInputStream buffer = new BufferedInputStream(pipedInputStream);
    private PipedOutputStream pipedOutputStream = null;
    
    /**
     * Constructor
     */
    public MsgFactory() {
    	try {
			pipedOutputStream = new PipedOutputStream(pipedInputStream);
		} catch (IOException e) {
			logger.error("Unable to create Piped Output stream", e);
		}
    }

    /**
     * Adds incoming data to the data buffer. First call addData(), then call processData()
     * 
     * @param data data to be added
     * @param len length of data to be added
     */
    public void addData(byte[] data, int length) {
    	logger.info("added {} bytes to the stream", length);
    	try {
			pipedOutputStream.write(data,  0, length);
		} catch (IOException e) {
			logger.error("Unable to write data to the piped stream");
		}
    }

    /**
     * After data has been added, this method processes it.
     * processData() needs to be called until it returns null, indicating that no
     * more messages can be formed from the data buffer.
     * 
     * @return a valid message, or null if the message is not complete
     * @throws IOException if data was received with unknown command codes
     */
    public Msg processData() throws IOException {
    	logger.info("processing data");
    	// Save our position before we start pulling things off so we can reset if needed
    	// and wait for more data
    	buffer.mark(64); // we don't have any messages near this long yet

    	// handle the case where we get a pure nack
    	if(buffer.available() < 2) {
    		// 2 is the smallest message and if the data's not there we'll hang on the read.. lame I know
    		return null;
    	}
    	byte header = (byte) buffer.read();
        if (header == 0x15) {
            logger.trace("got pure nack!");
            try {
                Msg m = Msg.makeMessage("PureNACK");
                return m;
            } catch (IOException e) {
                return null;
            }
        }
        // drain the buffer until the first byte is 0x02 (start of insteon/x10 messages
        if (header != 0x02) {
        	// drain till the next message or the end
        	// the next call will pick that up
        	drainBuffer();
        	return null;
        }
        // Now see if we have enough data for a complete message.
        // If not, we return null, and expect this method to be called again
        // when more data has come in.
        int msgLen = -1;
        boolean isExtended = false;
        if(buffer.available() > 0) {
            // we have some data, but do we have enough to read the entire header?
        	byte command = (byte) buffer.read();
            int headerLength = Msg.getHeaderLength(command);
            
            if(headerLength < 0) {
            	logger.warn("Got unknown command code {}", Integer.toHexString(command));
            	drainBuffer();
            	return null;
            }
            
            if(buffer.available() < (headerLength-2)) { // -2 since we've already pulled 2 of the header bytes out
            	// Reset to the mark before the header so we can wait for more data
            	logger.info("Not enough data yet for the header, returning null");
            	buffer.reset();
            	return null;
            }
            
            byte [] headerBytes = new byte[headerLength];
            headerBytes[0] = header;
            headerBytes[1] = command;
            // and then get the rest of the header
            buffer.read(headerBytes, 2, headerLength-2);
            
            isExtended = Msg.isExtended(headerBytes);
            logger.info("header length expected: {} extended: {}", headerLength, isExtended);

            int messageLength = Msg.getMessageLength(command, isExtended);
            
            if(messageLength < 0) {
            	logger.warn("Unable to find length for command {} isExtended {}", command, isExtended);
            	drainBuffer();
            	return null;
            }
            
            if(buffer.available() < (messageLength - headerLength)) {
            	logger.info("Not enough data yet to read the message");
            	buffer.reset();
            	return null;
            }
            
            byte [] messageBytes = new byte[messageLength];
            // reset the buffer back to where we started, then grab the whole thing
            buffer.reset();
            buffer.read(messageBytes);
            
            Msg msg = Msg.createMessage(messageBytes, messageLength, isExtended);
            logger.info("created a message!! {}" , msg);
            return msg;
        }
        
        return null;

    }

//    private void bail(String txt) {
//        drainBuffer(); // this will drain until end or it finds the next 0x02
//        logger.warn(txt);
//        //throw new IOException(txt);
//    }

    private void drainBuffer() throws IOException {
    	byte current = (byte) 0x00;
    	while(buffer.available() > 0 && current != 0x02) {
    		buffer.mark(2);
    		current = (byte) buffer.read();
    	}
    	buffer.reset();    	
    	
    }


    
    /**
	 * Helper method to make standard message
	 * 
	 * @param flags
	 * @param cmd1
	 * @param cmd2
	 * @return standard message
	 * @throws FieldException
	 * @throws IOException
	 */
	public static  Msg makeStandardMessage(DeviceAddress address, byte flags, byte cmd1, byte cmd2) throws FieldException, IOException {
		return (makeStandardMessage(address, flags, cmd1, cmd2, -1));
	}

	/**
	 * Helper method to make standard message, possibly with group
	 * 
	 * @param flags
	 * @param cmd1
	 * @param cmd2
	 * @param group (-1 if not a group message)
	 * @return standard message
	 * @throws FieldException
	 * @throws IOException
	 */
	public static Msg makeStandardMessage(DeviceAddress address, byte flags, byte cmd1, byte cmd2, int group) throws FieldException, IOException {
		Msg m = Msg.makeMessage(SEND_STANDARD_MESSAGE);
		DeviceAddress addr = null;
		if (group != -1) {
			flags |= 0xc0; // mark message as group message
			// and stash the group number into the address
			addr = new InsteonAddress((byte) 0, (byte) 0, (byte) (group & 0xff));
		} else {
			addr = address;
		}
		m.setAddress(TO_ADDRESS, addr);
		m.setByte(MESSAGE_FLAGS, flags);
		m.setByte(COMMAND_1, cmd1);
		m.setByte(COMMAND_2, cmd2);
		return m;
	}

	public static Msg makeX10Message(byte rawX10, byte X10Flag) throws FieldException, IOException {
		Msg m = Msg.makeMessage("SendX10Message");
		m.setByte("rawX10", rawX10);
		m.setByte("X10Flag", X10Flag);
		m.setQuietTime(300L);
		return m;
	}

	/**
	 * Helper method to make extended message
	 * 
	 * @param flags
	 * @param cmd1
	 * @param cmd2
	 * @return extended message
	 * @throws FieldException
	 * @throws IOException
	 */
	public static Msg makeExtendedMessage(DeviceAddress address, byte flags, byte cmd1, byte cmd2) throws FieldException, IOException {
		return makeExtendedMessage(address, flags, cmd1, cmd2, new byte[] {});
	}

	/**
	 * Helper method to make extended message
	 * 
	 * @param flags
	 * @param cmd1
	 * @param cmd2
	 * @param data  array with userdata
	 * @return extended message
	 * @throws FieldException
	 * @throws IOException
	 */
	public static Msg makeExtendedMessage(DeviceAddress address, byte flags, byte cmd1, byte cmd2, byte[] data) throws FieldException, IOException {
		Msg m = Msg.makeMessage(SEND_EXTENDED_MESSAGE);
		m.setAddress(TO_ADDRESS, address);
		m.setByte(MESSAGE_FLAGS, (byte) (((flags & 0xff) | 0x10) & 0xff));
		m.setByte(COMMAND_1, cmd1);
		m.setByte(COMMAND_2, cmd2);
		m.setUserData(data);
		m.setCRC();
		return m;
	}

	/**
	 * Helper method to make extended message, but with different CRC calculation
	 * 
	 * @param flags
	 * @param cmd1
	 * @param cmd2
	 * @param data  array with user data
	 * @return extended message
	 * @throws FieldException
	 * @throws IOException
	 */
	public static  Msg makeExtendedMessageCRC2(DeviceAddress address, byte flags, byte cmd1, byte cmd2, byte[] data)
			throws FieldException, IOException {
		Msg m = Msg.makeMessage(SEND_EXTENDED_MESSAGE);
		m.setAddress(TO_ADDRESS, address);
		m.setByte(MESSAGE_FLAGS, (byte) (((flags & 0xff) | 0x10) & 0xff));
		m.setByte(COMMAND_1, cmd1);
		m.setByte(COMMAND_2, cmd2);
		m.setUserData(data);
		m.setCRC2();
		return m;
	}
	
	
}
