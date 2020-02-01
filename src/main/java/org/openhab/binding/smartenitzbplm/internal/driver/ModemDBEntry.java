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

import java.util.ArrayList;
import java.util.Collections;

import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.Port;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.utils.Utils;

/*
 * The ModemDBEntry class holds a modem device type record
 * an xml file.
 *
 * @author Bernd Pfrommer
 * @since 1.6.0
 */
public class ModemDBEntry {
    private InsteonAddress m_address = null;
    private Port m_port = null;
    private ArrayList<Msg> m_linkRecords = new ArrayList<Msg>();
    private ArrayList<Byte> m_controls = new ArrayList<Byte>();
    private ArrayList<Byte> m_respondsTo = new ArrayList<Byte>();

    public ModemDBEntry(InsteonAddress aAddr) {
        m_address = aAddr;
    }

    public ArrayList<Msg> getLinkRecords() {
        return m_linkRecords;
    }

    public void addLinkRecord(Msg m) {
        m_linkRecords.add(m);
    }

    public void addControls(byte c) {
        m_controls.add(c);
    }

    public ArrayList<Byte> getControls() {
        return m_controls;
    }

    public void addRespondsTo(byte r) {
        m_respondsTo.add(r);
    }

    public ArrayList<Byte> getRespondsTo() {
        return m_respondsTo;
    }

    public void setPort(Port p) {
        m_port = p;
    }

    public Port getPort() {
        return m_port;
    }

    @Override
    public String toString() {
        String s = "addr:" + m_address + "|controls:[" + toGroupString(m_controls) + "]|responds_to:["
                + toGroupString(m_respondsTo) + "]|link_recors";
        for (Msg m : m_linkRecords) {
            s += ":(" + m + ")";
        }
        return s;
    }

    private String toGroupString(ArrayList<Byte> group) {
        ArrayList<Byte> sorted = new ArrayList<Byte>(group);
        Collections.sort(sorted);

        StringBuffer buf = new StringBuffer();
        for (Byte b : sorted) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append("0x");
            buf.append(Utils.getHexString(b));
        }

        return buf.toString();
    }
}
