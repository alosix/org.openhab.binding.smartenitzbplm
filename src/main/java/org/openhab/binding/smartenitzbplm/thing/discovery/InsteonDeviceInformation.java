package org.openhab.binding.smartenitzbplm.thing.discovery;

import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddress;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;

/**
 * This is the information that's contained in the response to a std message with data 0x10 0x00
 * The device category and subcategory together should be enough to identify the device.
 * @author jpowers
 *
 */
public class InsteonDeviceInformation {
	private DeviceAddress address;
	private byte deviceCategory;
	private byte deviceSubCategory;
	private byte firmwareVersion;
	private ZBPLMHandler handler;
	
	public byte getDeviceCategory() {
		return deviceCategory;
	}
	public void setDeviceCategory(byte deviceCategory) {
		this.deviceCategory = deviceCategory;
	}
	public byte getDeviceSubCategory() {
		return deviceSubCategory;
	}
	public void setDeviceSubCategory(byte deviceSubCategory) {
		this.deviceSubCategory = deviceSubCategory;
	}
	public byte getFirmwareVersion() {
		return firmwareVersion;
	}
	public void setFirmwareVersion(byte firmwareVersion) {
		this.firmwareVersion = firmwareVersion;
	}
	public DeviceAddress getAddress() {
		return address;
	}
	public void setAddress(DeviceAddress address) {
		this.address = address;
	}
	public ZBPLMHandler getHandler() {
		return handler;
	}
	public void setHandler(ZBPLMHandler handler) {
		this.handler = handler;
	}

	

}
