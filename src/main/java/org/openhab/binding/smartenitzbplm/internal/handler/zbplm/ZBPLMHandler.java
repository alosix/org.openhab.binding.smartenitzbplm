package org.openhab.binding.smartenitzbplm.internal.handler.zbplm;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;

/**
 * This thing supports the SmartenIt Zigbee/Insteon Power Line Modem
 * @author jpowers
 *
 */
public class ZBPLMHandler extends BaseThingHandler {

	public ZBPLMHandler(Thing thing) {
		super(thing);
		Configuration config = thing.getConfiguration();
		
		
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// TODO Auto-generated method stub
		
	}

}
