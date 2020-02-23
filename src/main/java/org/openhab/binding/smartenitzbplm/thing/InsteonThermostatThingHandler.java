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

public class InsteonThermostatThingHandler extends InsteonBaseThingHandler {
	private final Logger logger = LoggerFactory.getLogger(InsteonThermostatThingHandler.class);

	public InsteonThermostatThingHandler(Thing thing) {
		super(thing);

//		InsteonDimmerConfig config = getConfigAs(InsteonDimmerConfig.class);

	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		super.handleCommand(channelUID, command);
		logger.info("got a command {} for channel {}", command, channelUID);
		
	}

	@Override
	public void onMessage(Msg msg) {
		super.onMessage(msg);
		// check to see if its for me
		

	}

	public int transformSystemMode(int cmd) {
		switch (cmd) {
		case 0:
			return (0x09); // off
		case 1:
			return (0x04); // heat
		case 2:
			return (0x05); // cool
		case 3:
			return (0x06); // auto (aka manual auto)
		case 4:
			return (0x0A); // program (aka auto)
		default:
			break;
		}
		return (0x0A); // when in doubt go to program
	}

	public int transformFanMode(int cmd) {
		switch (cmd) {
		case 0:
			return (0x08); // fan mode auto
		case 1:
			return (0x07); // fan always on
		default:
			break;
		}
		return (0x08); // when in doubt go auto mode
	}
}
