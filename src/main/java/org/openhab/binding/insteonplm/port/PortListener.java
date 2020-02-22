package org.openhab.binding.insteonplm.port;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonDevice;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ModemDBEntry;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgListener;
import org.openhab.binding.smartenitzbplm.internal.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles messages that come in from the ports.
 * Will only process one message at a time.
 */
public class PortListener implements MsgListener {
	private final Logger logger = LoggerFactory.getLogger(PortListener.class);
	
	private ConcurrentHashMap<InsteonAddress, InsteonDevice> devices;
	private Map<DeviceAddress, ModemDBEntry> modemDBEntries = new ConcurrentHashMap<>();
	
	private int x10HouseUnit = -1;
	
	public PortListener( ConcurrentHashMap<InsteonAddress, InsteonDevice> devices) {
		this.devices = devices;
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public void msg(Msg msg, ZBPLMHandler handler) {
        if (msg.isEcho() || msg.isPureNack()) {
            return;
        }
        logger.debug("got msg: {}", msg);
        if (msg.isX10()) {
            handleX10Message(msg, handler);
        } else {
            handleInsteonMessage(msg, handler);
        }

    }

   
    public String getLinkInfo(Map<InsteonAddress, ModemDBEntry> dbes, InsteonAddress a) {
        ModemDBEntry dbe = dbes.get(a);
        ArrayList<Byte> controls = dbe.getControls();
        ArrayList<Byte> responds = dbe.getRespondsTo();

        StringBuffer buf = new StringBuffer("the modem");
        if (!controls.isEmpty()) {
            buf.append(" controls groups [");
            buf.append(toGroupString(controls));
            buf.append("]");
        }

        if (!responds.isEmpty()) {
            if (!controls.isEmpty()) {
                buf.append(" and");
            }

            buf.append(" responds to groups [");
            buf.append(toGroupString(responds));
            buf.append("]");
        }

        return buf.toString();
    }

    private String toGroupString(ArrayList<Byte> group) {
        ArrayList<Byte> sorted = new ArrayList<Byte>(group);
        Collections.sort(sorted);

        StringBuffer buf = new StringBuffer();
        for (Byte b : sorted) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append("0x");
            buf.append(Utils.getHexString(b));
        }

        return buf.toString();
    }

    private void handleInsteonMessage(Msg msg, ZBPLMHandler handler) {
        DeviceAddress toAddr = msg.getAddr("toAddress");
        if (!msg.isBroadcast()) {
            // not for one of our modems, do not process
            return;
        }
        DeviceAddress fromAddr = msg.getAddr("fromAddress");
        if (fromAddr == null) {
            logger.debug("invalid fromAddress, ignoring msg {}", msg);
            return;
        }
        handleMessage(handler, fromAddr, msg);
    }

    private void handleX10Message(Msg msg, ZBPLMHandler handler) {
        try {
            int x10Flag = msg.getByte("X10Flag") & 0xff;
            int rawX10 = msg.getByte("rawX10") & 0xff;
            if (x10Flag == 0x80) { // actual command
                if (x10HouseUnit != -1) {
                    InsteonAddress fromAddr = new InsteonAddress((byte) x10HouseUnit);
                    handleMessage(handler, fromAddr, msg);
                }
            } else if (x10Flag == 0) {
                // what unit the next cmd will apply to
                x10HouseUnit = rawX10 & 0xFF;
            }
        } catch (FieldException e) {
            logger.error("got bad X10 message: {}", msg, e);
            return;
        }
    }

    private void handleMessage(ZBPLMHandler handler, DeviceAddress fromAddr, Msg msg) {
    	// TODO: JWP figure ot how to get device.. or delete
        InsteonDevice dev = null;//getDevice(fromAddr);
        if (dev == null) {
            logger.debug("dropping message from unknown device with address {}", fromAddr);
        } else {
            dev.handleMessage(msg);
        }
    }
}

