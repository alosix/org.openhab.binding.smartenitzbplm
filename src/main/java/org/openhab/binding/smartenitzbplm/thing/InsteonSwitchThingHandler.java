package org.openhab.binding.smartenitzbplm.thing;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;

import java.io.IOException;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsteonSwitchThingHandler extends InsteonBaseThingHandler {
	private final Logger logger = LoggerFactory.getLogger(InsteonSwitchThingHandler.class);

	protected byte onLevel = 0x00;

	public InsteonSwitchThingHandler(Thing thing) {
		super(thing);

//		InsteonDimmerConfig config = getConfigAs(InsteonDimmerConfig.class);

	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
			super.handleCommand(channelUID, command);
			logger.info("got a command {} for channel {}", command, channelUID);
			try {
				if (command == OnOffType.ON) {
					Msg msg = MsgFactory.makeExtendedMessage(this.address, (byte) 0x0f, (byte) 0x11, (byte) 0xff);
					handler.sendMsg(msg);
					
				} else if (command == OnOffType.OFF) {
					Msg msg = MsgFactory.makeExtendedMessage(this.address, (byte) 0x0f, (byte) 0x13, (byte) 0x00f);
					handler.sendMsg(msg);

				}
			} catch (FieldException | IOException e) {
				logger.error("Error Sending switch update", e);
			}
	}

	@Override
	public void onMessage(Msg msg) {
		super.onMessage(msg);
		// check to see if its for me
		if (!this.address.equals(msg.getAddr(FROM_ADDRESS))) {
			logger.info("Message from address {} does not equal this address {}", msg.getAddr(FROM_ADDRESS),
					this.address.toString());
			return;
		}
		try {
			if (msg.getName().equals(STANDARD_MESSAGE_RECEIVED)) {
				onLevel = msg.getByte(COMMAND_2);
				this.updateStatus(ThingStatus.ONLINE);
				logger.info("setting onLevel to {}", onLevel);
				updateState(SWITCH_ONOFF, onLevel != 0 ? OnOffType.ON : OnOffType.OFF);
			}
		} catch (FieldException e) {
			logger.error("Error getting on level ", e);
			return;
		}

		logger.info("got a message:" + msg.toString());

	}

}
