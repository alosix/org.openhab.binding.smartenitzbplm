package org.openhab.binding.smartenitzbplm.internal.handler.zbplm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonDevice;


/**
 * This thing supports the SmartenIt Zigbee/Insteon Power Line Modem
 * @author jpowers
 *
 */
public class ZBPLMHandler extends BaseBridgeHandler {
	private Driver driver = null;
	private ConcurrentMap<InsteonAddress, InsteonDevice> devices = null;
	
	public ZBPLMHandler(Bridge bridge) {
		super(bridge);
		Configuration config = thing.getConfiguration();
		
		this.driver = new Driver();
		this.devices = new ConcurrentHashMap<>();
		
		
	}

	@Override
	public void handleRemoval() {
		// TODO Auto-generated method stub
		super.handleRemoval();
	}

	@Override
	public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
		// TODO Auto-generated method stub
		super.handleConfigurationUpdate(configurationParameters);
	}

	
	@Override
	public void thingUpdated(Thing thing) {
		// TODO Auto-generated method stub
		super.thingUpdated(thing);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// TODO Auto-generated method stub
		
	}

}
