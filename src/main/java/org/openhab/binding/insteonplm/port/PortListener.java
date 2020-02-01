package org.openhab.binding.insteonplm.port;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMHandler;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonDevice;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonDevice.DeviceStatus;
import org.openhab.binding.smartenitzbplm.internal.driver.DriverListener;
import org.openhab.binding.smartenitzbplm.internal.driver.ModemDBEntry;
import org.openhab.binding.smartenitzbplm.internal.driver.Poller;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.Driver;
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
public class PortListener implements MsgListener, DriverListener {
	private final Logger logger = LoggerFactory.getLogger(PortListener.class);
	
	private Driver driver;
	private ConcurrentHashMap<InsteonAddress, InsteonDevice> devices;
	private int x10HouseUnit = -1;
	
	public PortListener(Driver driver, ConcurrentHashMap<InsteonAddress, InsteonDevice> devices) {
		this.driver = driver;
		this.devices = devices;
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public void msg(Msg msg, String fromPort) {
        if (msg.isEcho() || msg.isPureNack()) {
            return;
        }
        logger.debug("got msg: {}", msg);
        if (msg.isX10()) {
            handleX10Message(msg, fromPort);
        } else {
            handleInsteonMessage(msg, fromPort);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void driverCompletelyInitialized() {
        HashMap<InsteonAddress, ModemDBEntry> dbes = driver.lockModemDBEntries();
        logger.info("modem database has {} entries!", dbes.size());
        if (dbes.isEmpty()) {
            logger.warn("the modem link database is empty!");
        }
        for (InsteonAddress k : dbes.keySet()) {
            logger.debug("modem db entry: {}", k);
        }
        HashSet<InsteonAddress> addrs = new HashSet<InsteonAddress>();
        for (InsteonDevice dev : devices.values()) {
            InsteonAddress a = dev.getAddress();
            if (!dbes.containsKey(a)) {
                if (!a.isX10()) {
                    logger.warn("device {} not found in the modem database. Did you forget to link?", a);
                }
            } else {
                if (!dev.hasModemDBEntry()) {
                    addrs.add(a);
                    logger.info("device {} found in the modem database and {}.", a, getLinkInfo(dbes, a));
                    dev.setHasModemDBEntry(true);
                }
                if (dev.getStatus() != DeviceStatus.POLLING) {
                    Poller.s_instance().startPolling(dev, dbes.size());
                }
            }
        }
        for (InsteonAddress k : dbes.keySet()) {
            if (!addrs.contains(k) && !k.equals(dbes.get(k).getPort().getAddress())) {
                logger.info("device {} found in the modem database, but is not configured as an item and {}.", k,
                        getLinkInfo(dbes, k));
            }
        }
        driver.unlockModemDBEntries();
    }

    public String getLinkInfo(HashMap<InsteonAddress, ModemDBEntry> dbes, InsteonAddress a) {
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

    private void handleInsteonMessage(Msg msg, String fromPort) {
        InsteonAddress toAddr = msg.getAddr("toAddress");
        if (!msg.isBroadcast() && !driver.isMsgForUs(toAddr)) {
            // not for one of our modems, do not process
            return;
        }
        InsteonAddress fromAddr = msg.getAddr("fromAddress");
        if (fromAddr == null) {
            logger.debug("invalid fromAddress, ignoring msg {}", msg);
            return;
        }
        handleMessage(fromPort, fromAddr, msg);
    }

    private void handleX10Message(Msg msg, String fromPort) {
        try {
            int x10Flag = msg.getByte("X10Flag") & 0xff;
            int rawX10 = msg.getByte("rawX10") & 0xff;
            if (x10Flag == 0x80) { // actual command
                if (x10HouseUnit != -1) {
                    InsteonAddress fromAddr = new InsteonAddress((byte) x10HouseUnit);
                    handleMessage(fromPort, fromAddr, msg);
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

    private void handleMessage(String fromPort, InsteonAddress fromAddr, Msg msg) {
        InsteonDevice dev = null;//getDevice(fromAddr);
        if (dev == null) {
            logger.debug("dropping message from unknown device with address {}", fromAddr);
        } else {
            dev.handleMessage(fromPort, msg);
        }
    }
}

