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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The driver class manages the modem ports.
 * XXX: at this time, only a single modem has ever been used. Expect
 * the worst if you connect multiple modems. When multiple modems
 * are required, this code needs to be tested and fixed.
 *
 * @author Bernd Pfrommer
 * @since 1.5.0
 */

// TODO: JWP It might be worth folding this into the port

public class Driver {
    private static final Logger logger = LoggerFactory.getLogger(Driver.class);

    // maps device name to serial port, i.e /dev/insteon -> Port object
    private HashMap<String, Port> ports = new HashMap<String, Port>();
    private DriverListener driverListener = null; // single listener for notifications
    private HashMap<InsteonAddress, ModemDBEntry> modemDBEntries = new HashMap<InsteonAddress, ModemDBEntry>();
    private ReentrantLock modemDBEntriesLock = new ReentrantLock();
  
    public void setDriverListener(DriverListener listener) {
        driverListener = listener;
    }

  
    public boolean isReady() {
        for (Port p : ports.values()) {
            if (!p.isRunning()) {
                return false;
            }
        }
        return true;
    }

    public Map<InsteonAddress, ModemDBEntry> lockModemDBEntries() {
        modemDBEntriesLock.lock();
        return modemDBEntries;
    }

    public void unlockModemDBEntries() {
        modemDBEntriesLock.unlock();
    }

    /**
     * Add new port (modem) to the driver
     * 
     * @param name the name of the port (from the config file, e.g. port_0, port_1, etc
     * @param port the device name, e.g. /dev/insteon, /dev/ttyUSB0 etc
     */
    public void addPort(Port port) {
        if (ports.keySet().contains(port)) {
            logger.warn("ignored attempt to add duplicate port: {}", port);
        } else {
            ports.put(port.getDeviceName(),port);
            logger.debug("added new port: {} ", port);
        }
    }

    /**
     * Register a message listener with a port
     * 
     * @param listener the listener who wants to listen to port messages
     * @param port the port (e.g. /dev/ttyUSB0) to which the listener listens
     */
    public void addMsgListener(MsgListener listener, String port) {
        if (ports.keySet().contains(port)) {
            ports.get(port).addListener(listener);
        } else {
            logger.error("referencing unknown port {}!", port);
        }
    }

    public void startAllPorts() {
        for (Port p : ports.values()) {
            p.start();
        }
    }

    public void stopAllPorts() {
        for (Port p : ports.values()) {
            p.stop();
        }
    }

    /**
     * Write message to a port
     * 
     * @param port name of the port to write to (e.g. '/dev/ttyUSB0')
     * @param m the message to write
     * @throws IOException
     */
    public void writeMessage(Port port, Msg m) throws IOException {
        if (port == null) {
            logger.error("cannot write to unknown port {}", port);
            throw new IOException();
        }
        port.writeMessage(m);
    }

    public String getDefaultPort() {
        return (ports.isEmpty() ? null : ports.keySet().iterator().next());
    }

    public int getNumberOfPorts() {
        int n = 0;
        for (Port p : ports.values()) {
            if (p.isRunning()) {
                n++;
            }
        }
        return n;
    }

    public boolean isMsgForUs(InsteonAddress toAddr) {
        if (toAddr == null) {
            return false;
        }
        for (Port p : ports.values()) {
            if (p.getAddress().equals(toAddr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get port object corresponding to device
     * 
     * @param port device name of port (e.g. /dev/ttyUSB0)
     * @return corresponding Port object or null if not found
     */
    public Port getPort(String port) {
        if (port.equalsIgnoreCase("DEFAULT")) {
            if (ports.isEmpty()) {
                logger.error("no default port found!");
                return null;
            }
            return ports.values().iterator().next();
        }
        if (!ports.containsKey(port)) {
            logger.error("no port of name {} found!", port);
            return null;
        }
        return ports.get(port);
    }

    public void modemDBComplete(Port port) {
        // check if all ports have a complete device list
        if (!isModemDBComplete()) {
            return;
        }
        // if yes, notify listener
        driverListener.driverCompletelyInitialized();
    }

    public boolean isModemDBComplete() {
        // check if all ports have a complete device list
        for (Port p : ports.values()) {
            if (!p.isModemDBComplete()) {
                return false;
            }
        }
        return true;
    }
}
