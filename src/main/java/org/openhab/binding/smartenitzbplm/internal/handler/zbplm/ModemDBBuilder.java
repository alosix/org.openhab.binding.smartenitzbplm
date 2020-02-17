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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgListener;
import org.openhab.binding.smartenitzbplm.internal.utils.Utils;
import org.openhab.binding.smartenitzbplm.thing.listener.InsteonMsgListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the modem database from incoming link record messages
 *
 * @author Bernd Pfrommer
 * @since 1.5.0
 */

public class ModemDBBuilder implements InsteonMsgListener, Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ModemDBBuilder.class);
	private boolean isComplete = false;
	private Port port = null;
	private int timeoutMillis = 120000;
	private ZBPLMHandler handler;

	public ModemDBBuilder(ZBPLMHandler handler) {
		logger.info("DB Builder created");
		this.port = handler.getPort();
		this.handler = handler;
	}

	public void setRetryTimeout(int timeout) {
		this.timeoutMillis = timeout;
	}

	protected void start() {

		ForkJoinPool.commonPool().execute(this);
		logger.debug("querying port for first link record");
	}

	public boolean isComplete() {
		return (isComplete);
	}

	@Override
	public void run() {
		logger.info("starting modem database download");
		handler.addInsteonMsgListener(this);
		logger.info("clearing the db");
		port.clearModemDB();
		getFirstLinkRecord();
	}

	private void getFirstLinkRecord() {
		try {
			logger.info("Writting first link message");
			port.writeMessage(Msg.makeMessage("GetFirstALLLinkRecord"));
		} catch (IOException e) {
			logger.error("error sending link record query ", e);
		}

	}
	private void getNextLinkRecord() {
		try {
			port.writeMessage(Msg.makeMessage("GetNextALLLinkRecord"));
		} catch (IOException e) {
			logger.error("error sending link record query ", e);
		}

	}

	
	/**
	 * processes link record messages from the modem to build database and request
	 * more link records if not finished. {@inheritDoc}
	 */
	@Override
	public void onMessage(Msg msg) {
		logger.info("Got Message:" + msg.isPureNack());
		if (msg.isPureNack()) {
			
			return;
		}
		try {
			if (msg.getByte("Cmd") == 0x69 || msg.getByte("Cmd") == 0x6a) {
				// If the flag is "ACK/NACK", a record response
				// will follow, so we do nothing here.
				// If its "NACK", there are none
				if (msg.getByte("ACK/NACK") == 0x15) {
					done();
					return;
				}
			} else if (msg.getByte("Cmd") == 0x57) {
				// we got the link record response
				updateModemDB(msg.getAddress("LinkAddr"), port, msg);
				getNextLinkRecord();
			}
		} catch (FieldException e) {
			logger.error("bad field handling link records {}", e);
		} catch (IllegalStateException e) {
			logger.error("got exception requesting link records {}", e);
		}
	}

	private void done() {
		isComplete = true;
		port.modemDBComplete();
		//port.removeListener(this);
		logModemDB();
		logger.info("Modem db download completed");
	}

	private void logModemDB() {
		if (!logger.isDebugEnabled()) {
			return;
		}
		try {
			logger.debug("MDB ------- start of modem link records ------------------");
			Map<InsteonAddress, ModemDBEntry> dbes = port.getModemDBEntries();
			for (Entry<InsteonAddress, ModemDBEntry> db : dbes.entrySet()) {
				ArrayList<Msg> lrs = db.getValue().getLinkRecords();
				for (Msg m : lrs) {
					int recordFlags = m.getByte("RecordFlags") & 0xff;
					String ms = ((recordFlags & (0x1 << 6)) != 0) ? "CTRL" : "RESP";
					logger.debug("MDB {}: {} group: {} data1: {} data2: {} data3: {}", db.getKey(), ms,
							toHex(m.getByte("ALLLinkGroup")), toHex(m.getByte("LinkData1")),
							toHex(m.getByte("LinkData2")), toHex(m.getByte("LinkData2")));
				}
				logger.debug("MDB -----");
			}
			logger.debug("MDB ---------------- end of modem link records -----------");
		} catch (FieldException e) {
			logger.error("cannot access field:", e);
		}
	}

	public static String toHex(byte b) {
		return Utils.getHexString(b);
	}

	public void updateModemDB(InsteonAddress linkAddr, Port port, Msg m) {
		Map<InsteonAddress, ModemDBEntry> dbes = port.getModemDBEntries();
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
	}

	@Override
	public InsteonAddress getAddress() {
		// TODO Auto-generated method stub
		return new InsteonAddress("M0.DE.AA");
	}
}
