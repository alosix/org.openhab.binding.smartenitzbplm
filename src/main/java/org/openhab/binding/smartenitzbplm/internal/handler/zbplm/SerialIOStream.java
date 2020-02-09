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
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.collections.buffer.SynchronizedBuffer;
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
	private static final int RX_BUFFER_LEN = 1024;
	/**
	 * The circular fifo queue for receive data
	 */
	private final Buffer buffer = SynchronizedBuffer.decorate(new CircularFifoBuffer(RX_BUFFER_LEN));

	private Set<String> portOpenRuntimeExcepionMessages = ConcurrentHashMap.newKeySet();

	public SerialIOStream(SerialPortManager serialPortManager, String devName, int speed) {
		this.serialPortManager = serialPortManager;
		portName = devName;
		baudRate = speed;
	}

	@Override
	public String toString() {
		return "SerialIOStream [baudRate=" + baudRate + ", portName=" + portName + "]";
	}

	@Override
	public boolean open() {
		try {
			logger.debug("Connecting to serial port [{}] at {} baud", portName, baudRate);

			// in some rare cases we have to check whether a port really exists, because if
			// it doesn't the call to
			// CommPortIdentifier#open will kill the whole JVM
			Stream<SerialPortIdentifier> serialPortIdentifiers = serialPortManager.getIdentifiers();
			if (!serialPortIdentifiers.findAny().isPresent()) {
				logger.info("No communication ports found, cannot connect to [{}]", portName);
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
				// This is ending meaninful bit.. the io streams we read and write from get
				// assigned
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
				serialPort.close();
				serialPort.removeEventListener();

				inputStream.close();
				outputStream.close();

				logger.info("Serial port '{}' closed.", portName);
			}

		} catch (Exception e) {
			logger.error("Error closing serial port: '{}' ", portName, e);
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				int available = inputStream.available();
				logger.trace("Processing DATA_AVAILABLE event: have {} bytes available", available);
				byte buf[] = new byte[available];
				int offset = 0;
				while (offset != available) {
					if (logger.isTraceEnabled()) {
						logger.trace("Processing DATA_AVAILABLE event: try read  {} at offset {}", available - offset,
								offset);
					}
					int n = inputStream.read(buf, offset, available - offset);
					if (logger.isTraceEnabled()) {
						logger.trace("Processing DATA_AVAILABLE event: did read {} of {} at offset {}", n,
								available - offset, offset);
					}
					if (n <= 0) {
						throw new IOException(
								"Expected to be able to read " + available + " bytes, but saw error after " + offset);
					}
					offset += n;
				}
				for (int i = 0; i < available; i++) {
					buffer.add(new Integer(buf[i] & 0xff));
				}
			} catch (IOException e) {
				logger.warn("Processing DATA_AVAILABLE event: received IOException in serial port event", e);
			}


		}

	}

	@Override
	public String getDeviceName() {
		return this.portName;
	}

	public void purgeRxBuffer() {
		buffer.clear();

	}

}
