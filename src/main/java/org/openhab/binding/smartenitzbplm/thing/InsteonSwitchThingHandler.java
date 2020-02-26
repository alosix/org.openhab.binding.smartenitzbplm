package org.openhab.binding.smartenitzbplm.thing;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;
import java.io.IOException;
import java.math.BigDecimal;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
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

	private static final int MAX_LED_LEVEL = 0x7f;
	private static final int MIN_LED_LEVEL = 0x11;
	private boolean on = false;

	public InsteonSwitchThingHandler(Thing thing) {
		super(thing);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
			super.handleCommand(channelUID, command);
			logger.info("got a command {} for channel {}", command, channelUID);
			String channelId = channelUID.getIdWithoutGroup();
			try {
				if(SWITCH_ONOFF.contentEquals(channelId)) {
					if (command == OnOffType.ON) {
						Msg msg = MsgFactory.makeStandardMessage(this.address, (byte) 0x0f, (byte) 0x11, (byte) 0xff);
						handler.sendMsg(msg);
						
					} else if (command == OnOffType.OFF) {
						Msg msg = MsgFactory.makeStandardMessage(this.address, (byte) 0x0f, (byte) 0x13, (byte) 0x00);
						handler.sendMsg(msg);
	
					}
				}
				if(SWITCH_LEVEL_LED.contentEquals(channelId) && command instanceof PercentType) {
					Msg msg = MsgFactory.makeExtendedMessage(this.address, (byte) 0x1f, (byte) 0x2e, (byte) 0x00, 
							new byte[] {(byte) 0x01, (byte) 0x07, convertToLedLevel((PercentType) command)});
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
			return;
		}
		try {
			if (msg.getName().equals(STANDARD_MESSAGE_RECEIVED)) {
				on = msg.getByte(COMMAND_2) != 0x00;
				logger.info("setting onLevel to {}", on);
				updateState(SWITCH_ONOFF, on ? OnOffType.ON : OnOffType.OFF);
			}

			if (msg.getName().equals(EXTENDED_MESSAGE_RECIEVED)) {
				byte ledBrightness = msg.getByte(USER_DATA_9);
				updateState(SWITCH_LEVEL_LED, new PercentType(convertFromLedLevel(ledBrightness)));

			}
		} catch (FieldException e) {
			logger.error("Error getting on level ", e);
			return;
		}

		logger.info("got a message:" + msg.toString());

	}

	/**
	 * Converts the value from the percent type command to level for the led
	 * 
	 * @param command
	 * @return
	 */
	private byte convertToLedLevel(PercentType command) {
		PercentType percentType = (PercentType) command;
		double percentOn = percentType.doubleValue() / 100.0;
		int ledMultiplier = MAX_LED_LEVEL -MIN_LED_LEVEL;
		byte level = (byte) ((ledMultiplier * percentOn)  +  MIN_LED_LEVEL);
		logger.info("On level to be set to {}", level);
		return level;
	}
	
	private int convertFromLedLevel(byte ledLevel) {
		int ledMultiplier = MAX_LED_LEVEL -MIN_LED_LEVEL;
		int adjustedLevel = ledLevel - MIN_LED_LEVEL;
		double percent = ((double) adjustedLevel / (double) ledMultiplier) * (double)100.0;
		return (int) percent;
	}
	
	
}
