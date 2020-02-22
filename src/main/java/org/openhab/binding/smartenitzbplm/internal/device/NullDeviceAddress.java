package org.openhab.binding.smartenitzbplm.internal.device;

public class NullDeviceAddress implements DeviceAddress {

	@Override
	public void storeBytes(byte[] bytes, int offset) {
		bytes[offset] = 0;
		bytes[offset + 1] = 0;
		bytes[offset + 2] = 0;		

	}

	@Override
	public byte getHighByte() {
		return 0;
	}

	@Override
	public byte getMiddleByte() {
		return 0;
	}

	@Override
	public byte getLowByte() {
		return 0;
	}

}
