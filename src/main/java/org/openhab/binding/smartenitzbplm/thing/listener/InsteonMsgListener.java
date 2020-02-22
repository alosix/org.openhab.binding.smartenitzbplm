package org.openhab.binding.smartenitzbplm.thing.listener;

import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddress;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;

public interface InsteonMsgListener {

	public DeviceAddress getAddress();
	
	public void onMessage(Msg msg);

}
