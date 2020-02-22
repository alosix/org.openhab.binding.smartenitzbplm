package org.openhab.binding.smartenitzbplm.internal.device;

import org.openhab.binding.smartenitzbplm.internal.utils.Utils;

public class DeviceAddressFactory {
	public static DeviceAddress fromString(String address) {
		byte highByte = 0;
		byte middleByte = 0;
		byte lowByte = 0;
		if (X10.isValidAddress(address)) {
			lowByte = X10.addressToByte(address);
			return new X10Address(lowByte);
		} else {
			String[] parts = address.split("\\.");
			if (parts.length != 3) {
				return new NullDeviceAddress();
			}
			highByte = (byte) Utils.fromHexString(parts[0]);
			middleByte = (byte) Utils.fromHexString(parts[1]);
			lowByte = (byte) Utils.fromHexString(parts[2]);
			return new InsteonAddress(highByte, middleByte, lowByte);
		}
	}

	public static DeviceAddress fromBytes(byte[] bytes, int offset) {
		byte highByte = bytes[offset];
		byte middleByte = bytes[offset + 1];
		byte lowByte = bytes[offset + 2];
		
		if(highByte == 0x00 && lowByte == 0x00) {
			return new X10Address(lowByte);
		} else {
			return new InsteonAddress(highByte, middleByte, lowByte);
		}
	}
	
	/**
	 * Test if Insteon address is valid
	 * 
	 * @return true if address is in valid AB.CD.EF or (for X10) H.UU format
	 */
	public static boolean isValid(String addr) {
		if (addr == null) {
			return false;
		}
		if (X10.isValidAddress(addr)) {
			return true;
		}
		String[] fields = addr.split("\\.");
		if (fields.length != 3) {
			return false;
		}
		try {
			// convert the insteon xx.xx.xx address to integer to test
			@SuppressWarnings("unused")
			int test = Integer.parseInt(fields[2], 16) * 65536 + Integer.parseInt(fields[1], 16) * 256
					+ +Integer.parseInt(fields[0], 16);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
