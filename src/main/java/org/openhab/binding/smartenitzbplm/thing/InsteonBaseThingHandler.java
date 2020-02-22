package org.openhab.binding.smartenitzbplm.thing;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddress;
import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddressFactory;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.openhab.binding.smartenitzbplm.thing.config.InsteonBaseConfig;
import org.openhab.binding.smartenitzbplm.thing.listener.InsteonMsgListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//,  ConfigDescriptionProvider, DynamicStateDescriptionProvider
public abstract class InsteonBaseThingHandler extends BaseThingHandler implements InsteonMsgListener {
		
	private final Logger logger = LoggerFactory.getLogger(InsteonBaseThingHandler.class);
	protected DeviceAddress address;
	protected ZBPLMHandler handler;
	
	public InsteonBaseThingHandler(Thing thing) {
		super(thing);
		InsteonBaseConfig  config = getConfigAs(InsteonBaseConfig.class);
		this.address = DeviceAddressFactory.fromString(config.insteon_address);
	
	}
	
	
	@Override
	public void initialize() {
		logger.info("Init called");
		updateStatus(ThingStatus.UNKNOWN);

        if (getBridge() != null) {
            bridgeStatusChanged(getBridge().getStatusInfo());
        }
	}


	
	@Override
	public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
		super.bridgeStatusChanged(bridgeStatusInfo);
		logger.info("*****Bridge status changed !!");
		this.handler = (ZBPLMHandler) getBridge().getHandler();
		this.handler.addInsteonMsgListener(this);
		
		init();
		
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DeviceAddress getAddress() {
		return address;
	}

	public abstract void init() ;
	
		
	
	
}
