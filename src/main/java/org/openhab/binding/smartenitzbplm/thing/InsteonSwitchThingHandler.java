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

	protected byte onLevel = 0x00;

	public InsteonSwitchThingHandler(Thing thing) {
		super(thing);

//		InsteonDimmerConfig config = getConfigAs(InsteonDimmerConfig.class);

	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
	}

	@Override
	public void onMessage(Msg msg) {
		// check to see if its for me
		if (msg.getAddr(FROM_ADDRESS) != this.address) {
			logger.info("Message from address {} does not equal this address {}", msg.toString(),
					this.address.toString());
			return;
		}
		try {
			if (msg.getName().equals(STANDARD_MESSAGE_RECEIVED)) {
				onLevel = msg.getByte(COMMAND_2);
				logger.info("setting onLevel to {}", onLevel);
			}
		} catch (FieldException e) {
			logger.error("Error getting on level ", e);
			return;
		} 
		
		logger.info("got a message:" + msg.toString());

	}

	@Override
	public void init() {
		try {
			Msg msg = Msg.makeMessage(SEND_STANDARD_MESSAGE);
			msg.setAddress(TO_ADDRESS, this.address);
			msg.setByte(MESSAGE_FLAGS, (byte) 0x0f);
			msg.setByte(COMMAND_1, (byte) 0x19);
			msg.setByte(COMMAND_2, (byte) 0x00);
			handler.sendMsg(msg);

		} catch (IOException | FieldException e) {
			logger.error("Unable to send status message", e);
		}

	}

}
