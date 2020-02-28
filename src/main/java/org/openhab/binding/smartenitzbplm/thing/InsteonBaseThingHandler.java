package org.openhab.binding.smartenitzbplm.thing;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;

import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddress;
import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddressFactory;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgFactory;
import org.openhab.binding.smartenitzbplm.thing.config.InsteonBaseConfig;
import org.openhab.binding.smartenitzbplm.thing.listener.InsteonMsgListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//,  ConfigDescriptionProvider, DynamicStateDescriptionProvider
public abstract class InsteonBaseThingHandler extends BaseThingHandler implements InsteonMsgListener {

	private final Logger logger = LoggerFactory.getLogger(InsteonBaseThingHandler.class);
	protected DeviceAddress address;
	protected ZBPLMHandler handler;
	protected ScheduledExecutorService scheduledExecutors = ThreadPoolManager.getScheduledPool(SCHEDULED_POOL);
	
	protected int pollSinceLastMessage = 0;

	public InsteonBaseThingHandler(Thing thing) {
		super(thing);
		InsteonBaseConfig config = getConfigAs(InsteonBaseConfig.class);
		this.address = DeviceAddressFactory.fromString(config.insteon_address);

	}

	@Override
	public void initialize() {
		if (getBridge() != null) {
			bridgeStatusChanged(getBridge().getStatusInfo());
		}
	}

	@Override
	public void onMessage(Msg msg) {
		if(this.address.equals(msg.getAddr(FROM_ADDRESS))) {
			pollSinceLastMessage = 0;
			updateStatus(ThingStatus.ONLINE);
		}

	}

	@Override
	public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
		super.bridgeStatusChanged(bridgeStatusInfo);
		if (getBridge() != null) {
			this.handler = (ZBPLMHandler) getBridge().getHandler();
			this.handler.addInsteonMsgListener(this);
			init();
		}

	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		logger.info("Handling command {}", command);
		if(RefreshType.REFRESH == command) {
			logger.info("Refreshing state");
			try {
				Msg msg = MsgFactory.makeStandardMessage(this.address, (byte) 0x0f, (byte) 0x19, (byte) 0x00);
				handler.sendMsg(msg);

			} catch (IOException | FieldException e) {
				logger.error("Unable to send status message", e);
			}	
		}
	}

	@Override
	public DeviceAddress getAddress() {
		return address;
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
					Msg msg = MsgFactory.makeStandardMessage(address, (byte) 0x0f, (byte) 0x19, (byte) 0x00);
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

	@Override
	public void dispose() {
		super.dispose();
		this.handler.removeInsteonMsgListener(this);
	}

	
}
