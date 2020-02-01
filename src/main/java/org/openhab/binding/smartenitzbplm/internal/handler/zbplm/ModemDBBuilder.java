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
package org.openhab.binding.smartenitzbplm.internal.handler.zbplm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgListener;
import org.openhab.binding.smartenitzbplm.internal.utils.Utils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the modem database from incoming link record messages
 *
 * @author Bernd Pfrommer
 * @since 1.5.0
 */

public class ModemDBBuilder implements MsgListener, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ModemDBBuilder.class);
    private boolean isComplete = false;
    private Port port = null;
    private Thread writeThread = null;
    private int timeoutMillis = 120000;

    
    public ModemDBBuilder(Port port) {
    	this.port = port;
	}

    public void setRetryTimeout(int timeout) {
        this.timeoutMillis = timeout;
    }

    protected void start() {
        //port.addListener(this);
        writeThread = new Thread(this);
        writeThread.setName("DBBuilder");
        writeThread.start();
        logger.debug("querying port for first link record");
    }

    public void startDownload() {
        logger.info("starting modem database download");
        port.clearModemDB();
        getFirstLinkRecord();
    }

    public synchronized boolean isComplete() {
        return (isComplete);
    }

    @Override
    public void run() {
        logger.trace("starting modem db builder thread");
        while (!isComplete()) {
            startDownload();
            try {
                Thread.sleep(timeoutMillis); // wait for download to complete
            } catch (InterruptedException e) {
                logger.warn("modem db builder thread interrupted");
                break;
            }
            if (!isComplete()) {
                logger.warn("modem database download unsuccessful, restarting!");
            }
        }
        logger.trace("exiting modem db builder thread");
    }

    private void getFirstLinkRecord() {
        try {
            port.writeMessage(Msg.s_makeMessage("GetFirstALLLinkRecord"));
        } catch (IOException e) {
            logger.error("error sending link record query ", e);
        }

    }

    /**
     * processes link record messages from the modem to build database
     * and request more link records if not finished.
     * {@inheritDoc}
     */
    @Override
    public void msg(Msg msg, String fromPort) {
        if (msg.isPureNack()) {
            return;
        }
        try {
            if (msg.getByte("Cmd") == 0x69 || msg.getByte("Cmd") == 0x6a) {
                // If the flag is "ACK/NACK", a record response
                // will follow, so we do nothing here.
                // If its "NACK", there are none
                if (msg.getByte("ACK/NACK") == 0x15) {
                    logger.debug("got all link records.");
                    done();
                }
            } else if (msg.getByte("Cmd") == 0x57) {
                // we got the link record response
                updateModemDB(msg.getAddress("LinkAddr"), port, msg);
                port.writeMessage(Msg.s_makeMessage("GetNextALLLinkRecord"));
            }
        } catch (FieldException e) {
            logger.debug("bad field handling link records {}", e);
        } catch (IOException e) {
            logger.debug("got IO exception handling link records {}", e);
        } catch (IllegalStateException e) {
            logger.debug("got exception requesting link records {}", e);
        }
    }

    private synchronized void done() {
        isComplete = true;
        logModemDB();
        port.removeListener(this);
        port.modemDBComplete();
    }

    private void logModemDB() {
        try {
            logger.debug("MDB ------- start of modem link records ------------------");
            HashMap<InsteonAddress, ModemDBEntry> dbes = port.getDriver().lockModemDBEntries();
            for (Entry<InsteonAddress, ModemDBEntry> db : dbes.entrySet()) {
                ArrayList<Msg> lrs = db.getValue().getLinkRecords();
                for (Msg m : lrs) {
                    int recordFlags = m.getByte("RecordFlags") & 0xff;
                    String ms = ((recordFlags & (0x1 << 6)) != 0) ? "CTRL" : "RESP";
                    logger.info("MDB {}: {} group: {} data1: {} data2: {} data3: {}", db.getKey(), ms,
                            toHex(m.getByte("ALLLinkGroup")), toHex(m.getByte("LinkData1")),
                            toHex(m.getByte("LinkData2")), toHex(m.getByte("LinkData2")));
                }
                logger.debug("MDB -----");
            }
            logger.debug("MDB ---------------- end of modem link records -----------");
        } catch (FieldException e) {
            logger.error("cannot access field:", e);
        } finally {
            port.getDriver().unlockModemDBEntries();
        }
    }

    public static String toHex(byte b) {
        return Utils.getHexString(b);
    }

    public void updateModemDB(InsteonAddress linkAddr, Port port, Msg m) {
        HashMap<InsteonAddress, ModemDBEntry> dbes = port.getDriver().lockModemDBEntries();
        ModemDBEntry dbe = dbes.get(linkAddr);
        if (dbe == null) {
            dbe = new ModemDBEntry(linkAddr);
            dbes.put(linkAddr, dbe);
        }
        dbe.setPort(port);
        if (m != null) {
            dbe.addLinkRecord(m);
            try {
                byte group = m.getByte("ALLLinkGroup");
                int recordFlags = m.getByte("RecordFlags") & 0xff;
                if ((recordFlags & (0x1 << 6)) != 0) {
                    dbe.addControls(group);
                } else {
                    dbe.addRespondsTo(group);
                }
            } catch (FieldException e) {
                logger.error("cannot access field:", e);
            }
        }
        port.getDriver().unlockModemDBEntries();
    }
}
