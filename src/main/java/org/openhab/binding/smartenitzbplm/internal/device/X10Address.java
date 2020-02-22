package org.openhab.binding.smartenitzbplm.internal.device;

public class X10Address implements DeviceAddress {
	private final byte lowByte;

	public X10Address(byte lowByte) {
		this.lowByte = lowByte;
	}

	@Override
	public void storeBytes(byte[] bytes, int offset) {
		bytes[offset] = 0;
		bytes[offset + 1] = 0;
		bytes[offset + 2] = getLowByte();
	}


	@Override
	public String toString() {
		byte house = (byte) (((lowByte & 0xf0) >> 4) & 0xff);
		byte unit = (byte) ((lowByte & 0x0f) & 0xff);
		return X10.houseToString(house) + "." + X10.unitToInt(unit);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + lowByte;
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
		X10Address other = (X10Address) obj;
		if (lowByte != other.lowByte)
			return false;
		return true;
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
		return lowByte;
	}
	
	public byte getX10HouseCode() {
		return (byte) ((lowByte & 0xf0) >> 4);
	}

	public byte getX10UnitCode() {
		return (byte) ((lowByte & 0x0f));
	}

	
}
