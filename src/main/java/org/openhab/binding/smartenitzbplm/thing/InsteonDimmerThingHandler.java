package org.openhab.binding.smartenitzbplm.thing;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.COMMAND_2;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.FROM_ADDRESS;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.STANDARD_MESSAGE_RECEIVED;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.SWITCH_LEVEL;

import java.io.IOException;

import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsteonDimmerThingHandler extends InsteonSwitchThingHandler {
	private final Logger logger = LoggerFactory.getLogger(InsteonDimmerThingHandler.class);
	private static final double MAX_LEVEL = 255.0;

	private int switchLevel = 0;
	
	public InsteonDimmerThingHandler(Thing thing) {
		super(thing);

	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		super.handleCommand(channelUID, command);
		String channelId = channelUID.getIdWithoutGroup();
		try {
			if(SWITCH_LEVEL.contentEquals(channelId)) {
				if (command instanceof PercentType) {
					PercentType percentType = (PercentType) command;
					double percentOn = percentType.doubleValue() / 100.0;
					byte level = (byte) (MAX_LEVEL * percentOn);
					logger.info("On level to be set to {}", level);
					Msg msg = MsgFactory.makeExtendedMessage(this.address, (byte) 0x0f, (byte) 0x11, level);
					handler.sendMsg(msg);
	
				}
			}
		} catch (FieldException | IOException e) {
			logger.error("Error sending dimmer message", e);
			
		} 
	}

	@Override
	public void onMessage(Msg msg) {
		super.onMessage(msg);
		// check to see if its for me
		if (!this.address.equals(msg.getAddr(FROM_ADDRESS))) {
			return;
		}
		try {
			if (msg.getName().equals(STANDARD_MESSAGE_RECEIVED)) {
				switchLevel = msg.getByte(COMMAND_2) & 0xFF;
				double percentOn = ((double) switchLevel/MAX_LEVEL) * 100.0;
				logger.info("setting level to {}", percentOn);
				updateState(SWITCH_LEVEL,new PercentType((int)percentOn));
				
			}
		} catch (FieldException e) {
			logger.error("Error getting on level ", e);
			return;
		}

		logger.info("got a message:" + msg.toString());

	}
}
