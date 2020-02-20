package org.openhab.binding.smartenitzbplm.thing.listener;

import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;

public interface InsteonMsgListener {

	public InsteonAddress getAddress();
	
	public void onMessage(Msg msg);

}
