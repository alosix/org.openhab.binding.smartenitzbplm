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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.openhab.binding.smartenitzbplm.internal.device.DeviceType;
import org.openhab.binding.smartenitzbplm.internal.device.DeviceTypeLoader;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonDevice;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgListener;
import org.openhab.binding.smartenitzbplm.thing.listener.ShutdownMsg;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Port class represents a port, that is a connection to either an Insteon
 * modem either through a serial or USB port, or via an Insteon Hub. It does the
 * initialization of the port, and (via its inner classes IOStreamReader and
 * IOStreamWriter) manages the reading/writing of messages on the Insteon
 * network.
 *
 * The IOStreamReader and IOStreamWriter class combined implement the somewhat
 * tricky flow control protocol. In combination with the MsgFactory class, the
 * incoming data stream is turned into a Msg structure for further processing by
 * the upper layers (MsgListeners).
 *
 * A write queue is maintained to pace the flow of outgoing messages. Sending
 * messages back-to-back can lead to dropped messages.
 *
 *
 * @author Bernd Pfrommer
 * @author Daniel Pfrommer
 * @since 1.5.0
 */

public class Port {
	private static final Logger logger = LoggerFactory.getLogger(Port.class);

	/**
	 * The ReplyType is used to keep track of the state of the serial port receiver
	 */
	enum ReplyType {
		GOT_ACK, WAITING_FOR_ACK, GOT_NACK
	}

	private IOStream ioStream = null;
	private Modem modem = null;
	private IOStreamReader reader = null;
	private IOStreamWriter writer = null;
	private final int readSize = 1024; // read buffer size
	private boolean running = false;
	private boolean modemDBComplete = false;
	// private MsgFactory msgFactory = null;
	private ModemDBBuilder modemDBBuilder = null;
	private DeviceTypeLoader deviceTypeLoader = null;
	private List<MsgListener> listeners = Collections.synchronizedList(new ArrayList<MsgListener>());

	private Map<InsteonAddress, ModemDBEntry> modemDBEntries = new ConcurrentHashMap<InsteonAddress, ModemDBEntry>();

	private final BlockingQueue<Msg> writeQueue = new LinkedBlockingQueue<Msg>();
	private ZBPLMHandler handler;

	/**
	 * Constructor
	 * 
	 * @param devName          the name of the port, i.e. '/dev/insteon'
	 * @param baudRate
	 * @param driver           The Driver object that manages this port
	 * @param deviceTypeLoader
	 */
	public Port(ZBPLMHandler handler) {
		this.handler = handler;
		this.modem = new Modem();
		this.ioStream = handler.getIoStream();
		// this.msgFactory = handler.getMsgFactory();
		addListener(modem);
		this.reader = new IOStreamReader();
		this.writer = new IOStreamWriter();
		this.deviceTypeLoader = handler.getDeviceTypeLoader();
	}

	public void setModemDBBuilder(ModemDBBuilder modemDBBuilder) {
		this.modemDBBuilder = modemDBBuilder;
	}

	public boolean isModemDBComplete() {
		return (modemDBComplete);
	}

	public boolean isRunning() {
		return running;
	}

	public InsteonAddress getAddress() {
		return modem.getAddress();
	}

	public String getDeviceName() {
		return ioStream.getDeviceName();
	}

	public ModemDBBuilder getModemDBBuilder() {
		return modemDBBuilder;
	}

	public void setModemDBRetryTimeout(int timeout) {
		modemDBBuilder.setRetryTimeout(timeout);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void addListener(MsgListener l) {
		synchronized (listeners) {
			if (!listeners.contains(l)) {
				listeners.add(l);
			}
		}
	}

	public void removeListener(MsgListener l) {
		synchronized (listeners) {
			if (listeners.remove(l)) {
				// logger.debug("removed listener from port");
			}
		}
	}

	/**
	 * Clear modem database that has been queried so far.
	 */
	public void clearModemDB() {
		logger.debug("clearing modem db!");
		modemDBEntries.clear();
	}

	/**
	 * Starts threads necessary for reading and writing
	 */
	public boolean start() {
		logger.info("starting port {}", ioStream.toString());
		if (running) {
			logger.info("port {} already running, not started again", ioStream.toString());
			handler.setPortStatus(true);
			return true;
		}
		int retryCount = 0;
		boolean open = false;
		while (!(open = ioStream.open()) && retryCount < 5) {
			logger.info("failed to open port {} retrying", ioStream.toString());
			retryCount++;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		if (!open) {
			logger.info("failed to open port {}", ioStream.toString());
			handler.setPortStatus(false);
			return false;
		}

		//Thread readerThread = new Thread(reader, "ZBPLM Port-reader");
		//readerThread.start();
		//Thread writerThread = new Thread(writer, "ZBPLM Port-writer");
		//writerThread.start();
		handler.getExecutorService().execute(reader);
		handler.getExecutorService().execute(writer);

		InitializationListener modemInitializationListener = new InitializationListener() {

			@Override
			public void onInitFinished() {
				modemDBBuilder.start(); // start downloading the device list
				//running = true;
				//handler.setPortStatus(true);
				logger.info("Finished starting the port");
			}
		};
		modem.addInitializationListener(modemInitializationListener);
		modem.initialize();
		return true;
	}

	/**
	 * Stops all threads
	 */
	public void stop() {
		Thread.dumpStack();
		// logger.info("stacktrace:" +
		// Thread.currentThread().getStackTrace().toString());
		logger.info("Stopping port:" + this.getDeviceName());

		if (!running) {
			logger.debug("port {} not running, no need to stop it", ioStream.toString());
			return;
		}
		// delete the remaining write queue, then throw in the shutdown message
		writeQueue.clear();
		writeQueue.add(new ShutdownMsg());

		ioStream.close();

		logger.debug("waiting for read thread to exit for port {}", ioStream);

		running = false;
		listeners.clear();
	}

	/**
	 * Adds message to the write queue
	 * 
	 * @param m message to be added to the write queue
	 * @throws IOException
	 */
	public void writeMessage(Msg m) throws IOException {
		if (m == null) {
			logger.error("trying to write null message!");
			throw new IOException("trying to write null message!");
		}
		if (m.getData() == null) {
			logger.error("trying to write message without data!");
			throw new IOException("trying to write message without data!");
		}
		try {
			logger.info("offering message:{}", m);
			writeQueue.offer(m);
			logger.info("enqueued msg: {}", m);
		} catch (IllegalStateException e) {
			logger.error("cannot write message {}, write queue is full!", m);
		}

	}

	/**
	 * Gets called by the modem database builder when the modem database is complete
	 */
	public void modemDBComplete() {
		modemDBComplete = true;
	}

	public Map<InsteonAddress, ModemDBEntry> getModemDBEntries() {
		return modemDBEntries;
	}

	/**
	 * The IOStreamReader uses the MsgFactory to turn the incoming bytes into Msgs
	 * for the listeners. It also communicates with the IOStreamWriter to implement
	 * flow control (tell the IOStreamWriter that it needs to retransmit, or the
	 * reply message has been received correctly).
	 * 
	 * @author Bernd Pfrommer
	 */
	class IOStreamReader implements Runnable {

		private ReplyType m_reply = ReplyType.GOT_ACK;
		private Object m_replyLock = new Object();

		/**
		 * Helper function for implementing synchronization between reader and writer
		 * 
		 * @return reference to the RequesReplyLock
		 */
		public Object getRequestReplyLock() {
			return m_replyLock;
		}

		@Override
		public void run() {
			logger.info("***************** starting reader...");
			try {
				for (Msg msg = ioStream.read(); msg != null; msg = ioStream.read()) {
					toAllListeners(msg);
					notifyWriter(msg);

				}
			} catch (InterruptedException e) {
				logger.info("Interrupted whil waiting for message");
			} 
			// byte[] buffer = new byte[2 * readSize];
//			try {
//            	byte [] buffer = ioStream.read();
//            	while(buffer != null) {
//            		logger.info("Got bytes!!");
//                //for (int len = -1; (len = ioStream.read(buffer, 0, readSize)) > 0;) {
//                    msgFactory.addData(buffer, buffer.length);
//                    processMessages();
//                    buffer = ioStream.read();
//                }
//            } catch (InterruptedException e) {
//                logger.debug("reader thread got interrupted!");
//            }
//            logger.error("reader thread exiting!");
		}

//        private void processMessages() {
//        	logger.info("processing messages");
//            try {
//                // must call processData() until we get a null pointer back
//                for (Msg m = msgFactory.processData(); m != null; m = msgFactory.processData()) {
//                	logger.info("processed message:" + m.toString());
//                    toAllListeners(m);
//                    notifyWriter(m);
//                }
//            } catch (IOException e) {
//                // got bad data from modem,
//                // unblock those waiting for ack
//                logger.warn("bad data received: {}", e.getMessage());
//                synchronized (getRequestReplyLock()) {
//                    if (m_reply == ReplyType.WAITING_FOR_ACK) {
//                        logger.warn("got bad data back, must assume message was acked.");
//                        m_reply = ReplyType.GOT_ACK;
//                        getRequestReplyLock().notify();
//                    }
//                }
//            }
//        }

		private void notifyWriter(Msg msg) {
			synchronized (getRequestReplyLock()) {
				if (m_reply == ReplyType.WAITING_FOR_ACK) {
					logger.info("waiting for ack");
					if (!msg.isUnsolicited()) {
						m_reply = (msg.isPureNack() ? ReplyType.GOT_NACK : ReplyType.GOT_ACK);
						logger.info("signaling receipt of ack: {}", (m_reply == ReplyType.GOT_ACK));
						getRequestReplyLock().notify();
					} else if (msg.isPureNack()) {
						m_reply = ReplyType.GOT_NACK;
						logger.info("signaling receipt of pure nack");
						getRequestReplyLock().notify();
					} else {
						logger.info("got unsolicited message");
					}
				}
			}
		}

		@SuppressWarnings("unchecked")
		private void toAllListeners(Msg msg) {
			// When we deliver the message, the recipient
			// may in turn call removeListener() or addListener(),
			// thereby corrupting the very same list we are iterating
			// through. That's why we make a copy of it, and
			// iterate through the copy.
			List<MsgListener> tempList = new ArrayList<MsgListener>();
			tempList.addAll(listeners);

			for (MsgListener l : tempList) {
				l.msg(msg, handler); // deliver msg to listener
			}
		}

		/**
		 * Blocking wait for ack or nack from modem. Called by IOStreamWriter for flow
		 * control.
		 * 
		 * @return true if retransmission is necessary
		 */
		public boolean waitForReply() {
			m_reply = ReplyType.WAITING_FOR_ACK;
			while (m_reply == ReplyType.WAITING_FOR_ACK) {
				try {
					logger.info("writer waiting for ack.");
					// There have been cases observed, in particular for
					// the Hub, where we get no ack or nack back, causing the binding
					// to hang in the wait() below, because unsolicited messages
					// do not trigger a notify(). For this reason we request retransmission
					// if the wait() times out.
					synchronized (getRequestReplyLock()) {
						getRequestReplyLock().wait(10000); // be patient for 30 msec	
					}
					if (m_reply == ReplyType.WAITING_FOR_ACK) { // timeout expired without getting ACK or NACK
						logger.trace("writer timeout expired, asking for retransmit!");
						m_reply = ReplyType.GOT_NACK;
						break;
					} else {
						logger.info("writer got ack: {}", (m_reply == ReplyType.GOT_ACK));
					}
				} catch (InterruptedException e) {
					break; // done for the day...
				}
			}
			return (m_reply == ReplyType.GOT_NACK);
		}
	}

	/**
	 * Writes messages to the port. Flow control is implemented following Insteon
	 * documents to avoid over running the modem.
	 * 
	 * @author Bernd Pfrommer
	 */
	class IOStreamWriter implements Runnable {
		private static final int WAIT_TIME = 200; // milliseconds

		@Override
		public void run() {
			logger.debug("starting writer...");
			while (true) {
				try {
					// this call blocks until the lock on the queue is released
					logger.trace("writer checking message queue");
					Msg msg = writeQueue.take();
					if (msg.getClass().isInstance(ShutdownMsg.class)) {
						// exit the thread we're shutdown
						logger.info("Exiting writter");
						return;
					}
					if (msg.getData() == null) {
						logger.error("found null message in write queue!");
					} else {
						logger.info("writing ({}): {}", msg.getQuietTime(), msg);
						// To debug race conditions during startup (i.e. make the .items
						// file definitions be available *before* the modem link records,
						// slow down the modem traffic with the following statement:
						// Thread.sleep(500);
						synchronized (reader.getRequestReplyLock()) {
							ioStream.write(msg.getData());

//							while (reader.waitForReply()) {
//								Thread.sleep(WAIT_TIME);
//								logger.info("retransmitting msg: {}", msg);
//								ioStream.write(msg.getData());
//							}

						}
						// if rate limited, need to sleep now.
						if (msg.getQuietTime() > 0) {
							Thread.sleep(msg.getQuietTime());
						}
					}
				} catch (InterruptedException e) {
					logger.error("got interrupted exception in write thread");
					break;
				} catch (Exception e) {
					logger.error("got exception in write thread:", e);
				}
			}
			logger.debug("exiting writer thread!");
		}
	}

	/**
	 * Class to get info about the modem
	 */
	class Modem implements MsgListener {
		private InsteonDevice device = null;

		private List<InitializationListener> initListeners = new ArrayList<InitializationListener>();

		InsteonAddress getAddress() {
			return (device == null) ? new InsteonAddress() : (device.getAddress());
		}

		InsteonDevice getDevice() {
			return device;
		}

		@Override
		public void msg(Msg msg, ZBPLMHandler handler) {
			try {
				if (msg.isPureNack()) {
					return;
				}
				if (msg.getByte("Cmd") == 0x60) {
					// add the modem to the device list
					InsteonAddress address = new InsteonAddress(msg.getAddress("IMAddress"));
					logger.info("Modem device addr is:" + address.toString());
					String prodKey = "0x000045";
					DeviceType dt = deviceTypeLoader.getDeviceType(prodKey);
					if (dt == null) {
						logger.error("unknown modem product key: {} for modem: {}.", prodKey, address);
					} else {
						device = InsteonDevice.s_makeDevice(dt);
						device.setAddress(address);
						device.setProductKey(prodKey);
						device.setIsModem(true);
						device.setHandler(handler);
						logger.info("found modem {} in device_types: {}", address, device.toString());
						modemDBBuilder.updateModemDB(address, Port.this, null);
						notifyListeners();
					}
					// can unsubscribe now
					removeListener(this);
				}
			} catch (FieldException e) {
				logger.error("error parsing im info reply field: ", e);
			}
		}

		public void initialize() {
			try {
				Msg m = Msg.makeMessage("GetIMInfo");
				writeMessage(m);
			} catch (IOException e) {
				logger.error("modem init failed!", e);
			}
		}

		public void addInitializationListener(InitializationListener listener) {
			initListeners.add(listener);
		}

		private void notifyListeners() {
			logger.info("Notifying listeners:" + initListeners.toString());
			for (InitializationListener listener : initListeners) {
				listener.onInitFinished();
			}
		}
	}
}
