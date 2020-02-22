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

import org.openhab.binding.smartenitzbplm.internal.utils.Utils;

/**
 * This class wraps an Insteon Address 'xx.xx.xx'
 *
 * @author Daniel Pfrommer
 * @since 1.5.0
 */

public class InsteonAddress implements DeviceAddress {
	private byte highByte;
	private byte middleByte;
	private byte lowByte;

	public InsteonAddress(InsteonAddress a) {
		highByte = a.highByte;
		middleByte = a.middleByte;
		lowByte = a.lowByte;
	}

	public InsteonAddress(byte high, byte middle, byte low) {
		highByte = high;
		middleByte = middle;
		lowByte = low;
	}

	/**
	 * Constructor for an InsteonAddress that wraps an X10 address. Simply stuff the
	 * X10 address into the lowest byte.
	 * 
	 * @param aX10HouseUnit the house & unit number as encoded by the X10 protocol
	 */
	public InsteonAddress(byte aX10HouseUnit) {
		highByte = 0;
		middleByte = 0;
		lowByte = aX10HouseUnit;
	}

	private void setHighByte(byte h) {
		highByte = h;
	}

	private void setMiddleByte(byte m) {
		middleByte = m;
	}

	private void setLowByte(byte l) {
		lowByte = l;
	}

	public byte getHighByte() {
		return highByte;
	}

	public byte getMiddleByte() {
		return middleByte;
	}

	public byte getLowByte() {
		return lowByte;
	}

	
	public boolean isX10() {
		return this.highByte == 0x00 && this.middleByte == 0x00 && this.lowByte != 0x00;

	}

	public void storeBytes(byte[] bytes, int offset) {
		bytes[offset] = getHighByte();
		bytes[offset + 1] = getMiddleByte();
		bytes[offset + 2] = getLowByte();
	}

	public void loadBytes(byte[] bytes, int offset) {
		setHighByte(bytes[offset]);
		setMiddleByte(bytes[offset + 1]);
		setLowByte(bytes[offset + 2]);
	}

	@Override
	public String toString() {
		return Utils.getHexString(highByte) + "." + Utils.getHexString(middleByte) + "." + Utils.getHexString(lowByte);

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + highByte;
		result = prime * result + lowByte;
		result = prime * result + middleByte;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InsteonAddress other = (InsteonAddress) obj;
		if (highByte != other.highByte)
			return false;
		if (lowByte != other.lowByte)
			return false;
		if (middleByte != other.middleByte)
			return false;
		return true;
	}

	

}