package org.openhab.binding.smartenitzbplm.thing;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddress;
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
		if (!this.address.equals(msg.getAddr(FROM_ADDRESS))) {
			return;
		}
		
		logger.info("Got a message in the thermostat:" + msg.toString());
		try {
			if (msg.getName().equals(STANDARD_MESSAGE_RECEIVED)) {
			}

			if (msg.getName().equals(READ_DATA_2_RECIEVED)) {
				int mode = msg.getByte(USER_DATA_6) ;
				int coolPoint = msg.getByte(USER_DATA_7);
				int humidity = msg.getByte(USER_DATA_8);
				int temp = msg.getByte(USER_DATA_10);
				temp |= msg.getByte(USER_DATA_9) << 8;
				int status = msg.getByte(USER_DATA_11);
				int heatPoint = msg.getByte(USER_DATA_12);
				
				updateState(THERMOSTAT_SYSTEMMODE, new DecimalType(mode));
				updateState(THERMOSTAT_COOLING, new DecimalType(coolPoint));
				updateState(THERMOSTAT_LOCALHUMIDITY, new DecimalType(humidity));
				updateState(THERMOSTAT_LOCALTEMP, new DecimalType((double)temp * 0.1));
				updateState(THERMOSTAT_HEATING, new DecimalType(heatPoint));
				updateState(THERMOSTAT_RUNNIGNMODE, new DecimalType(status));
				
				
				

			}
		} catch (FieldException e) {
			logger.error("Error getting on level ", e);
			return;
		}


	}
	
	/**
	 * Base init asks for the insteon status
	 */
	public void init() {
		final DeviceAddress address = this.address;
		Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				try {
					if(pollSinceLastMessage > 2) {
						updateStatus(ThingStatus.UNKNOWN);
					}
					Msg msg = MsgFactory.makeExtendedMessage(address, (byte) 0x1f, (byte) 0x2e, (byte) 0x02);
					handler.sendMsg(msg);
					pollSinceLastMessage++;

				} catch (IOException | FieldException e) {
					logger.error("Unable to send status message", e);
				}
				
			}
		};
		// run the status right now, and every 5  minutes
		scheduledExecutors.scheduleAtFixedRate(runnable, 0,  5, TimeUnit.MINUTES);
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
