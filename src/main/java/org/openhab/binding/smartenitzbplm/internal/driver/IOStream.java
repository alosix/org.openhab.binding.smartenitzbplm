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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vesalainen.comm.channel.SerialChannel.FlowControl;

/**
 * Abstract class for implementation for I/O stream with anything that looks
 * like a PLM (e.g. the insteon hubs, serial/usb connection etc)
 *
 * @author Bernd Pfrommer
 * @author Daniel Pfrommer
 * @since 1.7.0
 */

public abstract class IOStream {
    private static final Logger logger = LoggerFactory.getLogger(IOStream.class);
    protected InputStream inputStream = null;
    protected OutputStream outputStream = null;

    /**
     * read data from iostream
     *
     * @param b byte array (output)
     * @param offset offset for placement into byte array
     * @param readSize size to read
     * @return number of bytes read
     */
    public int read(byte[] b, int offset, int readSize) throws InterruptedException {
        int len = 0;
        while (len < 1) {
            try {
                len = inputStream.read(b, offset, readSize);
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            } catch (IOException e) {
                logger.trace("got exception while reading: {}", e.getMessage());
                while (!reconnect()) {
                    logger.trace("sleeping before reconnecting");
                    Thread.sleep(10000);
                }
            }
        }
        return (len);
    }

    /**
     * Write data to iostream
     *
     * @param b byte array to write
     */
    public void write(byte[] b) {
        try {
            outputStream.write(b);
        } catch (IOException e) {
            logger.trace("got exception while writing: {}", e.getMessage());
            while (!reconnect()) {
                try {
                    logger.trace("sleeping before reconnecting");
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {
                    logger.warn("interrupted while sleeping on write reconnect");
                }
            }
        }
    }

    /**
     * Opens the IOStream
     *
     * @return true if open was successful, false if not
     */
    public abstract boolean open();

    /**
     * Closes the IOStream
     */
    public abstract void close();

    /**
     * reconnects the stream
     *
     * @return true if reconnect succeeded
     */
    // TODO: JWP
    private synchronized boolean reconnect() {
        //close();
        //return (open());
    	return true;
    }

    /**
     * Creates an IOStream from an allowed config string:
     *
     * /dev/ttyXYZ[@baud_rate] (serial port like e.g. usb: /dev/ttyUSB0 or alias /dev/insteon)
     *
     * @param config
     * @return reference to IOStream
     */

    public static IOStream s_create(String config) {
    	String [] parts = config.split("@");
    	if(parts.length == 1) {
    		return new SerialIOStream(config);
    	} else {
    		return new SerialIOStream(parts[0], Integer.parseInt(parts[1]));
    	}
    }


}
