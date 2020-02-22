package org.openhab.binding.smartenitzbplm.internal.device;

public interface DeviceAddress {
	public void storeBytes(byte[] bytes, int offset);
	
	public byte getHighByte();
	public byte getMiddleByte();
	public byte getLowByte();
}
