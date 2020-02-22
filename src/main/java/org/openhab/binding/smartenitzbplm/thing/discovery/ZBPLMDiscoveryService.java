package org.openhab.binding.smartenitzbplm.thing.discovery;

import static java.util.stream.Collectors.toSet;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.smartenitzbplm.internal.device.DeviceAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ModemDBEntry;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.Port;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.thing.listener.InsteonMsgListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = { DiscoveryService.class,
		ZBPLMDiscoveryService.class }, configurationPid = "discovery.smartenitzbplm.device")
public class ZBPLMDiscoveryService extends AbstractDiscoveryService implements InsteonMsgListener {
	private static final Logger logger = LoggerFactory.getLogger(ZBPLMDiscoveryService.class);

	/**
	 * Default search time (2m)
	 */
	private final static int SEARCH_TIME = 120;

	private ZBPLMHandler handler = null;

	private final Set<InsteonDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();

	private final BlockingQueue<Msg> deviceReplyQueue = new LinkedBlockingDeque<Msg>();

	private final ExecutorService executor = ForkJoinPool.commonPool();

	public ZBPLMDiscoveryService() throws IllegalArgumentException {
		super(SEARCH_TIME);
		logger.info("Discovery service created");

	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void addInsteonDiscoveryParticipant(InsteonDiscoveryParticipant participant) {
		participants.add(participant);
	}

	protected void removeInsteonDiscoveryParticipant(InsteonDiscoveryParticipant participant) {
		participants.remove(participant);
	}

	@Reference
	protected void setZBPLMHandler(ZBPLMHandler handler) {
		this.handler = handler;
	}

	@Override
	public Set<ThingTypeUID> getSupportedThingTypes() {
		return participants.stream().flatMap(participant -> participant.getSupportedThingTypeUIDs().stream())
				.collect(toSet());
	}

	@Override
	@Modified
	protected void modified(Map<String, Object> configProperties) {
		super.modified(configProperties);
	}

	@Override
	@Deactivate
	public void deactivate() {
		logger.debug("Deactivating SmartenIt discovery service");
	}

	@Override
	protected void startScan() {
		logger.info("Starting scan");
		// if we run this in the same thread the handler sits in the inbox until it
		// completes
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					scanModemDB(handler);
				} catch (InterruptedException | IOException e) {
					logger.warn("Error while scanning DB for new handler:" + handler.toString(), e);
				}

			}
		});

	}

	/**
	 * Used when a new handler is added, we'll check its DB for anything we don't
	 * currently have
	 * 
	 * @param handler
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void scanModemDB(ZBPLMHandler handler) throws InterruptedException, IOException {
		logger.info("Starting to scan the contents of the modemDB");
		Port port = handler.getPort();
		long waitTime = 0;
		while (!port.isRunning() && !port.isModemDBComplete() && waitTime < (getScanTimeout() * 10)) {
			Thread.sleep(100);
			waitTime += 100;
		}

		if (!port.getModemDBBuilder().isComplete()) {
			logger.warn("DB download not complete, skipping scan");
			return;
		}
		
		handler.addInsteonMsgListener(this);

		try {
			Map<DeviceAddress, ModemDBEntry> entries = handler.getPort().getModemDBEntries();
			

			DeviceAddress modem = port.getAddress();
			for (DeviceAddress address : entries.keySet()) {
				if (address.equals(modem)) {
					// No need to try to discover the modem..
					continue;
				}
				logger.info("Sending discovery message to:" + address.toString());
				Msg msg = Msg.makeMessage(SEND_STANDARD_MESSAGE);
				msg.setAddress(TO_ADDRESS, address);
				msg.setByte(MESSAGE_FLAGS, (byte) 0x0F);
				msg.setByte(COMMAND_1, (byte) 0x10);
				msg.setByte(COMMAND_2, (byte) 0x00);
				port.writeMessage(msg);

				Msg reply = deviceReplyQueue.poll(10, TimeUnit.SECONDS);
				if (reply != null) {
					createDiscoveryResult(address, reply, handler);
				}
			}

		} catch (FieldException e) {
			logger.error("Error sending device type request", e);
		} finally {
			//port.removeListener(this);
		}

	}

	private void createDiscoveryResult(DeviceAddress address, Msg msg, ZBPLMHandler handler) throws FieldException {
		if (msg.isBroadcast() && msg.getByte(COMMAND_1) == 0x01) {
			DeviceAddress toAddress = msg.getAddr(TO_ADDRESS);

			InsteonDeviceInformation deviceInformation = new InsteonDeviceInformation();
			deviceInformation.setAddress(address);
			deviceInformation.setDeviceCategory(toAddress.getHighByte());
			deviceInformation.setDeviceSubCategory(toAddress.getMiddleByte());
			deviceInformation.setFirmwareVersion(toAddress.getLowByte());
			deviceInformation.setHandler(handler);
			for (InsteonDiscoveryParticipant participant : participants) {
				DiscoveryResult discoveryResult = participant.createResult(deviceInformation);
				if (discoveryResult != null) {
					logger.info("Found a thing:" + discoveryResult.toString());
					thingDiscovered(discoveryResult);
				}
			}

		}
	}

//	// Messages from the modem about new devices will come in here
//	@Override
//	public void msg(Msg msg, ZBPLMHandler fromPort) {
//		try {
//			if (msg.isBroadcast() && msg.getByte("command1") == 0x01) {
//				deviceReplyQueue.offer(msg);
//			}
//		} catch (FieldException e) {
//			// Just eat this in case we get a stray message
//		}
//	}

	@Override
	public DeviceAddress getAddress() {
		return handler.getPort().getAddress();
	}

	@Override
	public void onMessage(Msg msg) {
		try {
			if (msg.isBroadcast() && msg.getByte("command1") == 0x01) {
				deviceReplyQueue.offer(msg);
			}
		} catch (FieldException e) {
			// Just eat this in case we get a stray message
		}
	}

}
