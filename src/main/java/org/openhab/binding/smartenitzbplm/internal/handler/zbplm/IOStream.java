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
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
    protected final BlockingQueue<Msg> inboundQueue = new LinkedBlockingDeque<Msg>();
	
    protected OutputStream outputStream = null;

    /**
     * read data from iostream
     *
     * @param b byte array (output)
     * @param offset offset for placement into byte array
     * @param readSize size to read
     * @return number of bytes read
     */
    public Msg read() throws InterruptedException {
        return inboundQueue.take();
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
        close();
        return open();
    }

	public String getDeviceName() {
		// TODO Auto-generated method stub
		return "";
	}

}
