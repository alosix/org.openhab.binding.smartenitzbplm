package org.openhab.binding.smartenitzbplm.thing;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddress;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgFactory;
import org.openhab.binding.smartenitzbplm.internal.utils.Utils;
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
		try {
			String channel = channelUID.getIdWithoutGroup();
			if (THERMOSTAT_HEATING.equals(channel) && command instanceof Number) {
				Number heatType = (Number) command;
				logger.info("Setting headpoint to: {} {}", command, heatType.intValue());
				int heatPoint = heatType.intValue() * 2;
				Msg msg = MsgFactory.makeExtendedMessage(address, (byte) 0x1f, (byte) 0x6d, (byte) heatPoint);
				handler.sendMsg(msg);

			} else if (THERMOSTAT_COOLING.contentEquals(channel) && command instanceof Number) {
				Number coolType = (Number) command;
				logger.info("Setting coolpoint to: {} {}", command, coolType.intValue());
				int coolPoint = coolType.intValue() * 2;
				Msg msg = MsgFactory.makeExtendedMessage(address, (byte) 0x1f, (byte) 0x6c, (byte) coolPoint);
				handler.sendMsg(msg);
			}

		} catch (FieldException | IOException e) {
			logger.error("Unable to update thermostat", e);
		}
	}

	@Override
	public void onMessage(Msg msg) {
		super.onMessage(msg);
		// check to see if its for me
		if (!this.address.equals(msg.getAddr(FROM_ADDRESS))) {
			return;
		}

		logger.info("Got a message in the thermostat:" + msg.toString());
		pollSinceLastMessage = 0;
		updateStatus(ThingStatus.ONLINE);

		try {
			if (msg.getName().equals(STANDARD_MESSAGE_RECEIVED)) {

			}

			if (msg.getName().equals(EXTENDED_MESSAGE_RECIEVED) && msg.getByte(COMMAND_1) == (byte) 0x2e) {
				int mode = msg.getByte(USER_DATA_6) & 0xff;
				int coolPoint = msg.getByte(USER_DATA_7) & 0xff;
				int humidity = msg.getByte(USER_DATA_8) & 0xff;
				int temp = ((int) msg.getByte(USER_DATA_10)) & 0xff;
				temp |= (((int) msg.getByte(USER_DATA_9)) & 0xff) << 8;
				int status = msg.getByte(USER_DATA_11) & 0xff;
				int heatPoint = msg.getByte(USER_DATA_12) & 0xff;

				double celsius = (double) temp * 0.1;
				double fahrenheit = (9.0 / 5.0) * celsius + 32;

				int systemMode = mode & 0x0f >> 4;
				int fanMode = mode & 0xf0 >> 0;

				logger.info("System mode {} running mode {}", Utils.getHexString(systemMode),
						Utils.getHexString(status));

				updateState(THERMOSTAT_SYSTEMMODE, new DecimalType(systemMode));
				updateState(THERMOSTAT_COOLING, new DecimalType(coolPoint));
				updateState(THERMOSTAT_LOCALHUMIDITY, new PercentType(humidity));
				updateState(THERMOSTAT_LOCALTEMP, new DecimalType(fahrenheit));
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
		logger.info("Scheduling thermostat update");
		final DeviceAddress address = this.address;
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				try {
					if (pollSinceLastMessage > 2) {
						updateStatus(ThingStatus.UNKNOWN);
					}
					Msg msg = MsgFactory.makeExtendedMessageCRC2(address, (byte) 0x1f, (byte) 0x2e, (byte) 0x02);
					handler.sendMsg(msg);
					pollSinceLastMessage++;

				} catch (IOException | FieldException e) {
					logger.error("Unable to send status message", e);
				}

			}
		};
		// run the status right now, and every 5 minutes
		scheduledExecutors.scheduleAtFixedRate(runnable, random.nextInt(60), 60, TimeUnit.SECONDS);
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
