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
package org.openhab.binding.smartenitzbplm.internal.driver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.io.transport.serial.PortInUseException;
import org.eclipse.smarthome.io.transport.serial.SerialPort;
import org.eclipse.smarthome.io.transport.serial.SerialPortEvent;
import org.eclipse.smarthome.io.transport.serial.SerialPortEventListener;
import org.eclipse.smarthome.io.transport.serial.SerialPortIdentifier;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.eclipse.smarthome.io.transport.serial.UnsupportedCommOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;







/**
 * Implements IOStream for serial devices.
 * 
 * @author Bernd Pfrommer
 * @author Daniel Pfrommer
 * @since 1.7.0
 */
public class SerialIOStream extends IOStream implements SerialPortEventListener {
    private static final Logger logger = LoggerFactory.getLogger(SerialIOStream.class);
    private SerialPort m_port = null;
    private final String m_appName = "PLM";
    private int baudRate = 115200; // baud rate
    private String portName = null;
    // TODO: JWP Pass this in
    private SerialPortManager serialPortManager = null;
	private SerialPort serialPort;
	
	/**
     * The length of the receive buffer
     */
    private static final int RX_BUFFER_LEN = 512;
	/**
     * The circular fifo queue for receive data
     */
    private final int[] buffer = new int[RX_BUFFER_LEN];

    /**
     * The receive buffer end pointer (where we put the newly received data)
     */
    private int end = 0;

    /**
     * The receive buffer start pointer (where we take the data to pass to the application)
     */
    private int start = 0;

    /**
     * Synchronisation object for buffer queue manipulation
     */
    private final Object bufferSynchronisationObject = new Object();

	private Set<String> portOpenRuntimeExcepionMessages = ConcurrentHashMap.newKeySet();
	

    public SerialIOStream(String devName) {
        portName = devName;
    }

    public SerialIOStream(String devName, int speed) {
    	portName = devName;
    	baudRate = speed;
	}

//	@Override
//    public boolean open() {
//        try {
//            updateSerialProperties(m_devName);
//            CommPortIdentifier ci = CommPortIdentifier.getPortIdentifier(m_devName);
//            CommPort cp = ci.open(m_appName, 1000);
//            if (cp instanceof SerialPort) {
//                m_port = (SerialPort) cp;
//            } else {
//                throw new IllegalStateException("unknown port type");
//            }
//            m_port.setSerialPortParams(m_speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
//            m_port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
//            logger.debug("setting port speed to {}", m_speed);
//            //m_port.disableReceiveFraming();
//            m_port.enableReceiveThreshold(1);
//            // m_port.disableReceiveTimeout();
//            m_port.enableReceiveTimeout(1000);
//            m_in = m_port.getInputStream();
//            m_out = m_port.getOutputStream();
//            logger.info("successfully opened port {}", m_devName);
//            return true;
//        } catch (IOException e) {
//            logger.error("cannot open port: {}, got IOException ", m_devName, e);
//        } catch (PortInUseException e) {
//            logger.error("cannot open port: {}, it is in use!", m_devName);
//        } catch (UnsupportedCommOperationException e) {
//            logger.error("got unsupported operation {} on port {}", e.getMessage(), m_devName);
//        } catch (NoSuchPortException e) {
//            logger.error("got no such port for {}", m_devName);
//        } catch (IllegalStateException e) {
//            logger.error("got unknown port type for {}", m_devName);
//        }
//        return false;
//    }
// TODO:JWP Find usage
    private void updateSerialProperties(String devName) {

//		/*
//		 * By default, RXTX searches only devices /dev/ttyS* and /dev/ttyUSB*, and will
//		 * therefore not find devices that have been symlinked. Adding them however is
//		 * tricky, see below.
//		 */
//
//		//
//		// first go through the port identifiers to find any that are not in
//		// "gnu.io.rxtx.SerialPorts"
//		//
//		ArrayList<String> allPorts = new ArrayList<String>();
//		@SuppressWarnings("rawtypes")
//		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
//		while (portList.hasMoreElements()) {
//			CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
//			if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
//				allPorts.add(id.getName());
//			}
//		}
//		logger.trace("ports found from identifiers: {}", StringUtils.join(allPorts.toArray(), ":"));
//		//
//		// now add our port so it's in the list
//		//
//		if (!allPorts.contains(devName)) {
//			allPorts.add(devName);
//		}
//		//
//		// add any that are already in "gnu.io.rxtx.SerialPorts"
//		// so we don't accidentally overwrite some of those ports
//
//		String ports = System.getProperty("gnu.io.rxtx.SerialPorts");
//		if (ports != null) {
//			ArrayList<String> propPorts = new ArrayList<String>(Arrays.asList(ports.split(":")));
//			for (String p : propPorts) {
//				if (!allPorts.contains(p)) {
//					allPorts.add(p);
//				}
//			}
//		}
//		String finalPorts = StringUtils.join(allPorts.toArray(), ":");
//		logger.trace("final port list: {}", finalPorts);
//
//		//
//		// Finally overwrite the "gnu.io.rxtx.SerialPorts" System property.
//		//
//		// Note: calling setProperty() is not threadsafe. All bindings run in
//		// the same address space, System.setProperty() is globally visible
//		// to all bindings.
//		// This means if multiple bindings use the serial port there is a
//		// race condition where two bindings could be changing the properties
//		// at the same time
//		//
//		System.setProperty("gnu.io.rxtx.SerialPorts", finalPorts);
    }
//
//    @Override
//    public void close() {
//        if (m_port != null) {
//            m_port.close();
//        }
//        m_port = null;
//    }
//    
    @Override
    public boolean open() {
        try {
            logger.debug("Connecting to serial port [{}] at {} baud", portName, baudRate);

            // in some rare cases we have to check whether a port really exists, because if it doesn't the call to
            // CommPortIdentifier#open will kill the whole JVM
            Stream<SerialPortIdentifier> serialPortIdentifiers = serialPortManager.getIdentifiers();
            if (!serialPortIdentifiers.findAny().isPresent()) {
                logger.debug("No communication ports found, cannot connect to [{}]", portName);
                return false;
            }

            SerialPortIdentifier portIdentifier = serialPortManager.getIdentifier(portName);
            if (portIdentifier == null) {
                logger.error("Serial Error: Port {} does not exist.", portName);
                return false;
            }

            try {
                SerialPort localSerialPort = portIdentifier.open("org.openhab.binding.zigbee", 100);
                localSerialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
                localSerialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

                localSerialPort.enableReceiveTimeout(100);
                localSerialPort.addEventListener(this);
                localSerialPort.notifyOnDataAvailable(true);

                logger.debug("Serial port [{}] is initialized.", portName);
                serialPort = localSerialPort;
                portOpenRuntimeExcepionMessages.clear();
            } catch (PortInUseException e) {
                logger.error("Serial Error: Port {} in use.", portName);
                return false;
            } catch (UnsupportedCommOperationException e) {
                logger.error("Serial Error: Unsupported comm operation on Port {}.", portName);
                return false;
            } catch (TooManyListenersException e) {
                logger.error("Serial Error: Too many listeners on Port {}.", portName);
                return false;
            } catch (RuntimeException e) {
                if (!portOpenRuntimeExcepionMessages.contains(e.getMessage())) {
                    portOpenRuntimeExcepionMessages.add(e.getMessage());
                    logger.error("Serial Error: Device cannot be opened on Port {}. Caused by {}", portName,
                            e.getMessage());
                }
                return false;
            }

            try {
                inputStream = serialPort.getInputStream();
                outputStream = serialPort.getOutputStream();
            } catch (IOException e) {
            }

            return true;
        } catch (Exception e) {
            logger.error("Unable to open serial port: ", e);
            return false;
        }
    }

    @Override
    public void close() {
        try {
            if (serialPort != null) {
                serialPort.removeEventListener();

                outputStream.flush();

                inputStream.close();
                outputStream.close();

                serialPort.close();

                serialPort = null;
                inputStream = null;
                outputStream = null;

                synchronized (this) {
                    this.notify();
                }

                logger.debug("Serial port '{}' closed.", portName);
            }
        } catch (Exception e) {
            logger.error("Error closing serial port: '{}' ", portName, e);
        }
    }

	@Override
	// TODO: JWP implement
	public void serialEvent(SerialPortEvent event) {
		 if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
	            try {
	                synchronized (bufferSynchronisationObject) {
	                    int available = inputStream.available();
	                    logger.trace("Processing DATA_AVAILABLE event: have {} bytes available", available);
	                    byte buf[] = new byte[available];
	                    int offset = 0;
	                    while (offset != available) {
	                        if (logger.isTraceEnabled()) {
	                            logger.trace("Processing DATA_AVAILABLE event: try read  {} at offset {}",
	                                    available - offset, offset);
	                        }
	                        int n = inputStream.read(buf, offset, available - offset);
	                        if (logger.isTraceEnabled()) {
	                            logger.trace("Processing DATA_AVAILABLE event: did read {} of {} at offset {}", n,
	                                    available - offset, offset);
	                        }
	                        if (n <= 0) {
	                            throw new IOException("Expected to be able to read " + available
	                                    + " bytes, but saw error after " + offset);
	                        }
	                        offset += n;
	                    }
	                    for (int i = 0; i < available; i++) {
	                        buffer[end++] = buf[i] & 0xff;
	                        if (end >= RX_BUFFER_LEN) {
	                            end = 0;
	                        }
	                        if (end == start) {
	                            logger.warn("Processing DATA_AVAILABLE event: Serial buffer overrun");
	                            if (++start == RX_BUFFER_LEN) {
	                                start = 0;
	                            }

	                        }
	                    }
	                }
	            } catch (IOException e) {
	                logger.warn("Processing DATA_AVAILABLE event: received IOException in serial port event", e);
	            }

	            synchronized (this) {
	                this.notify();
	            }
	        }
		
	}
	
	 // TODO: JWP see if this is needed
	    public void purgeRxBuffer() {
	        synchronized (bufferSynchronisationObject) {
	            start = 0;
	            end = 0;
	        }
	    }


}
