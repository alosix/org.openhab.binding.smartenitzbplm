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
package org.openhab.binding.smartenitzbplm.internal.device;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMConfiguration;
import org.openhab.binding.smartenitzbplm.internal.device.DeviceType.FeatureGroup;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * The InsteonDevice class holds known per-device state of a single Insteon device,
 * including the address, what port(modem) to reach it on etc.
 * Note that some Insteon devices de facto consist of two devices (let's say
 * a relay and a sensor), but operate under the same address. Such devices will
 * be represented just by a single InsteonDevice. Their different personalities
 * will then be represented by DeviceFeatures.
 *
 * @author Bernd Pfrommer
 * @since 1.5.0
 */
public class InsteonDevice {
	private static final Logger logger = LoggerFactory.getLogger(InsteonDevice.class);

	public static enum DeviceStatus {
		INITIALIZED, POLLING
	}

	/** need to wait after query to avoid misinterpretation of duplicate replies */
	private static final int QUIET_TIME_DIRECT_MESSAGE = 2000;
	/** how far to space out poll messages */
	private static final int TIME_BETWEEN_POLL_MESSAGES = 1500;
	
	private DeviceAddress address = null;
	private long pollInterval = -1L; // in milliseconds
	private HashMap<String, DeviceFeature> features = new HashMap<String, DeviceFeature>();
	private String productKey = null;
	private Long lastTimePolled = 0L;
	private Long lastMsgReceived = 0L;
	private boolean isModem = false;
	private PriorityQueue<QEntry> requestQueue = new PriorityQueue<QEntry>();
	private DeviceFeature featureQueried = null;

	private long lastQueryTime = 0L;
	private boolean hasModemDBEntry = false;
	private DeviceStatus status = DeviceStatus.INITIALIZED;
	private ZBPLMHandler handler;

	/**
	 * Constructor
	 */
	public InsteonDevice() {
		lastMsgReceived = System.currentTimeMillis();
	}

	// --------------------- simple getters -----------------------------

	public boolean hasProductKey() {
		return productKey != null;
	}

	public String getProductKey() {
		return productKey;
	}

	public boolean hasModemDBEntry() {
		return hasModemDBEntry;
	}

	public DeviceStatus getStatus() {
		return status;
	}

	public DeviceAddress getAddress() {
		return (address);
	}


	public boolean hasValidPorts() {
		return (handler != null);
	}

	public long getPollInterval() {
		return pollInterval;
	}

	public boolean isModem() {
		return isModem;
	}

	public DeviceFeature getFeature(String f) {
		return features.get(f);
	}

	public HashMap<String, DeviceFeature> getFeatures() {
		return features;
	}



	public boolean hasProductKey(String key) {
		return productKey != null && productKey.equals(key);
	}

	public boolean hasValidPollingInterval() {
		return (pollInterval > 0);
	}

	public long getPollOverDueTime() {
		return (lastTimePolled - lastMsgReceived);
	}

	public ZBPLMHandler getHandler()  {
		return (this.handler);
	}
	
	public void setHandler(ZBPLMHandler handler) {
		this.handler = handler;
	}

	public boolean hasAnyListeners() {
		synchronized (features) {
			for (DeviceFeature f : features.values()) {
				if (f.hasListeners()) {
					return true;
				}
			}
		}
		return false;
	}
	// --------------------- simple setters -----------------------------

	public void setStatus(DeviceStatus aI) {
		status = aI;
	}

	public void setHasModemDBEntry(boolean b) {
		hasModemDBEntry = b;
	}

	public void setAddress(DeviceAddress ia) {
		address = ia;
	}


	public void setIsModem(boolean f) {
		isModem = f;
	}

	public void setProductKey(String pk) {
		productKey = pk;
	}

	public void setPollInterval(long pi) {
		logger.trace("setting poll interval for {} to {} ", address, pi);
		if (pi > 0) {
			pollInterval = pi;
		}
	}

	public void setFeatureQueried(DeviceFeature f) {
		synchronized (requestQueue) {
			featureQueried = f;
		}
	};

	public DeviceFeature getFeatureQueried() {
		synchronized (requestQueue) {
			return (featureQueried);
		}
	};

	

	/**
	 * Removes feature listener from this device
	 * 
	 * @param aItemName name of the feature listener to remove
	 * @return true if a feature listener was successfully removed
	 */
	public boolean removeFeatureListener(String aItemName) {
		boolean removedListener = false;
		synchronized (features) {
			for (Iterator<Entry<String, DeviceFeature>> it = features.entrySet().iterator(); it.hasNext();) {
				DeviceFeature f = it.next().getValue();
				if (f.removeListener(aItemName)) {
					removedListener = true;
				}
			}
		}
		return removedListener;
	}

	/**
	 * Invoked to process an openHAB command
	 * 
	 * @param driver  The driver to use
	 * @param c       The item configuration
	 * @param command The actual command to execute
	 */
//	public void processCommand(Driver driver, SmartenItZBPLMConfiguration c, Command command) {
		//logger.debug("processing command {} features: {}", command, m_features.size());
		// TODO: jwp check neeed
//        synchronized (m_features) {
//            for (DeviceFeature i : m_features.values()) {
//                if (i.isReferencedByItem(c.getItemName())) {
//                    i.handleCommand(c, command);
//                }
//            }
//        }
	//}

	/**
	 * Execute poll on this device: create an array of messages, add them to the
	 * request queue, and schedule the queue for processing.
	 * 
	 * @param delay scheduling delay (in milliseconds)
	 */
	public void doPoll(long delay) {
		long now = System.currentTimeMillis();
		ArrayList<QEntry> l = new ArrayList<QEntry>();
		synchronized (features) {
			int spacing = 0;
			for (DeviceFeature i : features.values()) {
				if (i.hasListeners()) {
					Msg m = i.makePollMsg();
					if (m != null) {
						l.add(new QEntry(i, m, now + delay + spacing));
						spacing += TIME_BETWEEN_POLL_MESSAGES;
					}
				}
			}
		}
		if (l.isEmpty()) {
			return;
		}
		synchronized (requestQueue) {
			for (QEntry e : l) {
				requestQueue.add(e);
			}
		}
		RequestQueueManager.s_instance().addQueue(this, now + delay);

		if (!l.isEmpty()) {
			synchronized (lastTimePolled) {
				lastTimePolled = now;
			}
		}
	}

	/**
	 * Handle incoming message for this device by forwarding it to all features that
	 * this device supports
	 * 
	 * @param msg      the incoming message
	 */
	public void handleMessage(Msg msg) {
		synchronized (lastMsgReceived) {
			lastMsgReceived = System.currentTimeMillis();
		}
		synchronized (features) {
			// first update all features that are
			// not status features
			for (DeviceFeature f : features.values()) {
				if (!f.isStatusFeature()) {
					logger.debug("----- applying message to feature: {}", f.getName());
					if (f.handleMessage(msg, handler)) {
						// handled a reply to a query,
						// mark it as processed
						logger.trace("handled reply of direct: {}", f);
						setFeatureQueried(null);
						break;
					}
				}
			}
			// then update all the status features,
			// e.g. when the device was last updated
			for (DeviceFeature f : features.values()) {
				if (f.isStatusFeature()) {
					f.handleMessage(msg, handler);
				}
			}
		}
	}

	

	/**
	 * Called by the RequestQueueManager when the queue has expired
	 * 
	 * @param timeNow
	 * @return time when to schedule the next message (timeNow + quietTime)
	 */
	public long processRequestQueue(long timeNow) {
		synchronized (requestQueue) {
			if (requestQueue.isEmpty()) {
				return 0L;
			}
			if (featureQueried != null) {
				// A feature has been queried, but
				// the response has not been digested yet.
				// Must wait for the query to be processed.
				long dt = timeNow - (lastQueryTime + featureQueried.getDirectAckTimeout());
				if (dt < 0) {
					logger.debug("still waiting for query reply from {} for another {} usec", address, -dt);
					return (timeNow + 2000L); // retry soon
				} else {
					logger.debug("gave up waiting for query reply from device {}", address);
				}
			}
			QEntry qe = requestQueue.poll(); // take it off the queue!
			if (!qe.getMsg().isBroadcast()) {
				logger.debug("qe taken off direct: {} {}", qe.getFeature(), qe.getMsg());
				lastQueryTime = timeNow;
				// mark feature as pending
				qe.getFeature().setQueryStatus(DeviceFeature.QueryStatus.QUERY_PENDING);
				// also mark this queue as pending so there is no doubt
				featureQueried = qe.getFeature();
			} else {
				logger.debug("qe taken off bcast: {} {}", qe.getFeature(), qe.getMsg());
			}
			long quietTime = qe.getMsg().getQuietTime();
			qe.getMsg().setQuietTime(500L); // rate limiting downstream!
			try {
				writeMessage(qe.getMsg());
			} catch (IOException e) {
				logger.error("message write failed for msg {}", qe.getMsg(), e);
			}
			// figure out when the request queue should be checked next
			QEntry qnext = requestQueue.peek();
			long nextExpTime = (qnext == null ? 0L : qnext.getExpirationTime());
			long nextTime = Math.max(timeNow + quietTime, nextExpTime);
			logger.debug("next request queue processed in {} msec, quiettime = {}", nextTime - timeNow, quietTime);
			return (nextTime);
		}
	}

	/**
	 * Enqueues message to be sent at the next possible time
	 * 
	 * @param m message to be sent
	 * @param f device feature that sent this message (so we can associate the
	 *          response message with it)
	 */
	public void enqueueMessage(Msg m, DeviceFeature f) {
		enqueueDelayedMessage(m, f, 0);
	}

	/**
	 * Enqueues message to be sent after a delay
	 * 
	 * @param m message to be sent
	 * @param f device feature that sent this message (so we can associate the
	 *          response message with it)
	 * @param d time (in milliseconds)to delay before enqueuing message
	 */
	public void enqueueDelayedMessage(Msg m, DeviceFeature f, long delay) {
		long now = System.currentTimeMillis();
		synchronized (requestQueue) {
			requestQueue.add(new QEntry(f, m, now + delay));
		}
		if (!m.isBroadcast()) {
			m.setQuietTime(QUIET_TIME_DIRECT_MESSAGE);
		}
		logger.trace("enqueing direct message with delay {}", delay);
		RequestQueueManager.s_instance().addQueue(this, now + delay);
	}

	private void writeMessage(Msg m) throws IOException {
		// TODO: JWP Reimplement if needed
		handler.getPort().writeMessage(m);
	}

	private void instantiateFeatures(DeviceType dt) {
		for (Entry<String, String> fe : dt.getFeatures().entrySet()) {
			DeviceFeature f = DeviceFeature.s_makeDeviceFeature(fe.getValue());
			if (f == null) {
				logger.error("device type {} references unknown feature: {}", dt, fe.getValue());
			} else {
				addFeature(fe.getKey(), f);
			}
		}
		for (Entry<String, FeatureGroup> fe : dt.getFeatureGroups().entrySet()) {
			FeatureGroup fg = fe.getValue();
			DeviceFeature f = DeviceFeature.s_makeDeviceFeature(fg.getType());
			if (f == null) {
				logger.error("device type {} references unknown feature group: {}", dt, fg.getType());
			} else {
				addFeature(fe.getKey(), f);
			}
			connectFeatures(fe.getKey(), f, fg.getFeatures());
		}
	}

	private void connectFeatures(String gn, DeviceFeature fg, ArrayList<String> features) {
		for (String fs : features) {
			DeviceFeature f = this.features.get(fs);
			if (f == null) {
				logger.error("feature group {} references unknown feature {}", gn, fs);
			} else {
				logger.debug("{} connected feature: {}", gn, f);
				fg.addConnectedFeature(f);
			}
		}
	}

	private void addFeature(String name, DeviceFeature f) {
		f.setDevice(this);
		synchronized (features) {
			features.put(name, f);
		}
	}

	@Override
	public String toString() {
		String s = address.toString();
		for (Entry<String, DeviceFeature> f : features.entrySet()) {
			s += "|" + f.getKey() + "->" + f.getValue().toString();
		}
		return s;
	}

	/**
	 * Factory method
	 * 
	 * @param dt device type after which to model the device
	 * @return newly created device
	 */
	public static InsteonDevice s_makeDevice(DeviceType dt) {
		InsteonDevice dev = new InsteonDevice();
		dev.instantiateFeatures(dt);
		return dev;
	}

	/**
	 * Queue entry helper class
	 * 
	 * @author Bernd Pfrommer
	 */
	public static class QEntry implements Comparable<QEntry> {
		private DeviceFeature m_feature = null;
		private Msg m_msg = null;
		private long m_expirationTime = 0L;

		public DeviceFeature getFeature() {
			return m_feature;
		}

		public Msg getMsg() {
			return m_msg;
		}

		public long getExpirationTime() {
			return m_expirationTime;
		}

		QEntry(DeviceFeature f, Msg m, long t) {
			m_feature = f;
			m_msg = m;
			m_expirationTime = t;
		}

		@Override
		public int compareTo(QEntry a) {
			return (int) (m_expirationTime - a.m_expirationTime);
		}
	}
}
