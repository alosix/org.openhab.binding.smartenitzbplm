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
import java.io.InputStream;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Stream;

import org.eclipse.smarthome.io.transport.serial.PortInUseException;
import org.eclipse.smarthome.io.transport.serial.SerialPort;
import org.eclipse.smarthome.io.transport.serial.SerialPortEvent;
import org.eclipse.smarthome.io.transport.serial.SerialPortEventListener;
import org.eclipse.smarthome.io.transport.serial.SerialPortIdentifier;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.eclipse.smarthome.io.transport.serial.UnsupportedCommOperationException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgFactory;
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
	private int baudRate = 115200; // baud rate
	private String portName = null;
	private SerialPortManager serialPortManager = null;
	private SerialPort serialPort = null;
	private MsgFactory msgFactory = null;
	
	// Note: Be careful who gets to read from this as pending reads will
	// affect the ability to close the port
	private InputStream inputStream = null;


	public SerialIOStream(SerialPortManager serialPortManager, String devName, int speed, MsgFactory msgFactory) {
		this.serialPortManager = serialPortManager;
		this.msgFactory = msgFactory;
		this.portName = devName;
		this.baudRate = speed;
		
		msgFactory.setInboundQueue(inboundQueue);
		msgFactory.start();
	}
	
	@Override
	public String toString() {
		return "SerialIOStream [baudRate=" + baudRate + ", portName=" + portName + "]";
	}

	@Override
	public boolean open() {
		try {
			synchronized (serialPortManager) {

				logger.debug("Connecting to serial port [{}] at {} baud", portName, baudRate);

				// in some rare cases we have to check whether a port really exists, because if
				// it doesn't the call to
				// CommPortIdentifier#open will kill the whole JVM
				Stream<SerialPortIdentifier> serialPortIdentifiers = serialPortManager.getIdentifiers();
				if (!serialPortIdentifiers.findAny().isPresent()) {
					logger.warn("No communication ports found, cannot connect to [{}]", portName);
					return false;
				}

				SerialPortIdentifier portIdentifier = serialPortManager.getIdentifier(portName);
				if (portIdentifier == null) {
					logger.error("Serial Error: Port {} does not exist.", portName);
					return false;
				}

				if (portIdentifier.isCurrentlyOwned()) {
					logger.error("Port is already owned by {}.", portIdentifier.getCurrentOwner());
					return false;
				}

				try {
					serialPort = portIdentifier.open("org.openhab.binding.smartenitzbplm", 5000); // wait 5 seconds 
					serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
							SerialPort.PARITY_NONE);
					serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

					// localSerialPort.enableReceiveTimeout(100);
					serialPort.addEventListener(this);
					serialPort.notifyOnDataAvailable(true);

					logger.debug("Serial port [{}] is initialized.", portName);
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
					logger.error("Serial Error: Device cannot be opened on Port {}. Caused by {}", portName,
							e.getMessage());
					return false;
				}

			}
			try {
				// This is ending meaninful bit.. the io streams we read and write from get
				// assigned
				inputStream = serialPort.getInputStream();
				outputStream = serialPort.getOutputStream();


			} catch (IOException e) {
				logger.error("Error getting one of the streams", e);
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
				inputStream.close();
				logger.info("input stream closed");
				outputStream.close();
				logger.info("output stream closed");
				serialPort.close();
				logger.info("Serial port '{}' closed.", portName);
				msgFactory.stop();
			} else {
				logger.info("serial port was null");
			}

		} catch (Throwable e) {
			logger.error("Error closing serial port: '{}' ", portName, e);
		}
	}

	@Override
	public String getDeviceName() {
		return this.portName;
	}

	public void purgeRxBuffer() {

		// buffer.clear();

	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		if(SerialPortEvent.DATA_AVAILABLE == event.getEventType()) {
			try {
				int available = inputStream.available();
				byte buffer[] = new byte[available];
				int totalRead = 0;
				while (totalRead < available) {
					int read = inputStream.read(buffer);
					logger.info("Read {} bytes", read);
					msgFactory.addData(buffer, read);
									//for (Msg m = msgFactory.processData(); m != null; m = msgFactory.processData()) {
//						inboundQueue.put(m);
	//				}
					totalRead += read;
				}
			} catch (IOException e) {
				logger.error("Error reading from the input stream", e);
			} 
		}
		

	}

}
