package org.openhab.binding.smartenitzbplm.thing.discovery;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants;

import static java.util.stream.Collectors.toSet;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ModemDBEntry;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.Port;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = {DiscoveryService.class,  ZBPLMDiscoveryService.class}, configurationPid = "discovery.smartenitzbplm")
public class ZBPLMDiscoveryService extends AbstractDiscoveryService implements MsgListener {
	private static final Logger logger = LoggerFactory.getLogger(ZBPLMDiscoveryService.class);

	/**
	 * Default search time (2m)
	 */
	private final static int SEARCH_TIME = 120;

	private final Set<ZBPLMHandler> handlers = new CopyOnWriteArraySet<>();

	private final Set<InsteonDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();

	private final BlockingQueue<Msg> deviceReplyQueue = new LinkedBlockingDeque<Msg>();

	public ZBPLMDiscoveryService() throws IllegalArgumentException {
		super(SEARCH_TIME);
		logger.info("Discovery service created");

	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void addInsteonDiscoveryParticipant(InsteonDiscoveryParticipant participant) {
		logger.info("**************************Adding discovery participant:" + participant.toString());
		participants.add(participant);
	}

	protected void removeInsteonDiscoveryParticipant(InsteonDiscoveryParticipant participant) {
		participants.remove(participant);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void addZBPLMHandler(ZBPLMHandler handler) {
		logger.info("*************************Adding handler:" + handler);
		handlers.add(handler);
		try {
			scanModemDB(handler);
		} catch (InterruptedException | IOException e) {
			logger.warn("Error while scanning DB for new handler:" + handler.toString(), e);
		}
	}

	@Override
	public Set<ThingTypeUID> getSupportedThingTypes() {
		return participants.stream().flatMap(participant -> participant.getSupportedThingTypeUIDs().stream())
				.collect(toSet());
	}

	protected void removeZBPLMHandler(ZBPLMHandler handler) {
		handlers.remove(handler);
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
		for (ZBPLMHandler handler : handlers) {
			handler.startScan(this.getScanTimeout());

		}

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
		Port port = handler.getPort();
		long waitTime = 0;
		while (!port.getModemDBBuilder().isComplete() && waitTime < (getScanTimeout() * 10)) {
			Thread.sleep(100);
			waitTime += 100;
		}

		if (!port.getModemDBBuilder().isComplete()) {
			logger.warn("DB download not complete, skipping scan");
			return;
		}

		try {
			Map<InsteonAddress, ModemDBEntry> entries = handler.getPort().getModemDBEntries();
			port.addListener(this);

			InsteonAddress modem = port.getAddress();
			for (InsteonAddress address : entries.keySet()) {
				if (address.equals(modem)) {
					// No need to try to discover the modem..
					continue;
				}
				logger.info("Sending discovery message to:" + address.toString());
				Msg msg = Msg.makeMessage(SEND_STANDARD_MESSAGE);
				msg.setAddress("toAddress", address);
				msg.setByte("messageFlags", (byte) 0x0F);
				msg.setByte("command1", (byte) 0x10);
				msg.setByte("command2", (byte) 0x00);
				port.writeMessage(msg);

				Msg reply = deviceReplyQueue.poll(10, TimeUnit.SECONDS);
				if (reply != null) {
					createDiscoveryResult(address, reply, handler);
				}
			}

		} catch (FieldException e) {
			logger.error("Error sending device type request", e);
		} finally {
			port.removeListener(this);
		}

	}

	private void createDiscoveryResult(InsteonAddress address, Msg msg, ZBPLMHandler handler) throws FieldException {
		if (msg.isBroadcast() && msg.getByte("command1") == 0x01) {
			InsteonAddress toAddress = msg.getAddr("toAddress");

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

	// Messages from the modem about new devices will come in here
	@Override
	public void msg(Msg msg, ZBPLMHandler fromPort) {
		try {
			if (msg.isBroadcast() && msg.getByte("command1") == 0x01) {
				deviceReplyQueue.offer(msg);
			}
		} catch (FieldException e) {
			// Just eat this in case we get a stray message
		}
	}

}
