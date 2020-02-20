package org.openhab.binding.smartenitzbplm.thing;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.thing.config.InsteonDimmerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;

import java.io.IOException;

public class InsteonSwitchThingHandler extends InsteonBaseThingHandler {
	private final Logger logger = LoggerFactory.getLogger(InsteonSwitchThingHandler.class);
	public InsteonSwitchThingHandler(Thing thing, ZBPLMHandler handler) {
		super(thing, handler);
		
//		InsteonDimmerConfig config = getConfigAs(InsteonDimmerConfig.class);
				
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
	}

	

	@Override
	public void onMessage(Msg msg) {
		logger.info("got a message");
		
	}

	@Override
	public void init() {
		try {
			Msg msg = Msg.makeMessage(SEND_STANDARD_MESSAGE);
			msg.setAddress("toAddress", this.address);
			msg.setByte("command1", (byte) 0x19);
			msg.setByte("command2", (byte) 0x00);
			handler.sendMsg(msg);
			
			
		} catch (IOException | FieldException e) {
			logger.error("Unable to send status message", e);
		}
		
		
	}

}
