package org.openhab.binding.smartenitzbplm.thing;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.openhab.binding.smartenitzbplm.thing.config.InsteonDimmerConfig;

public class InsteonDimmerThingHandler extends InsteonSwitchThingHandler {
	public InsteonDimmerThingHandler(Thing thing) {
		super(thing);
		
		InsteonDimmerConfig config = getConfigAs(InsteonDimmerConfig.class);
//		address = new InsteonAddress(config.insteon_address);
		
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		super.handleCommand(channelUID, command);
	}
	

}
