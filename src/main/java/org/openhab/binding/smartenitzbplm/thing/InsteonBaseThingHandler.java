package org.openhab.binding.smartenitzbplm.thing;

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.openhab.binding.smartenitzbplm.thing.config.InsteonBaseConfig;
import org.openhab.binding.smartenitzbplm.thing.listener.InsteonMsgListener;

public abstract class InsteonBaseThingHandler extends BaseThingHandler implements InsteonMsgListener {
	protected InsteonAddress address;
	protected ZBPLMHandler handler;
	
	public InsteonBaseThingHandler(Thing thing, ZBPLMHandler handler) {
		super(thing);
		InsteonBaseConfig  config = getConfigAs(InsteonBaseConfig.class);
		this.address = new InsteonAddress(config.insteon_address);
		this.handler = handler;
		this.handler.addInsteonMsgListener(this);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public InsteonAddress getAddress() {
		return address;
	}

	public abstract void init() ;
	
		
	
	
}
