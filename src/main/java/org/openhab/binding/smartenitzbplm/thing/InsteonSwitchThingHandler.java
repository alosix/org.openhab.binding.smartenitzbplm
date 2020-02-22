package org.openhab.binding.smartenitzbplm.thing;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.COMMAND_2;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.FROM_ADDRESS;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.STANDARD_MESSAGE_RECEIVED;

import java.io.IOException;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
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
			Msg msg = MsgFactory.makeStandardMessage(this.address, (byte) 0x0f, (byte) 0x19, (byte) 0x00);
			handler.sendMsg(msg);

		} catch (IOException | FieldException e) {
			logger.error("Unable to send status message", e);
		}

	}

}
