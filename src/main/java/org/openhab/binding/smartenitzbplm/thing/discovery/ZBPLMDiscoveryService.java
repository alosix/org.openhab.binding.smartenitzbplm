package org.openhab.binding.smartenitzbplm.thing.discovery;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.Driver;
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

@Component(immediate = true, service = ZBPLMDiscoveryService.class, configurationPid = "discovery.smartenitzbplm")
public class ZBPLMDiscoveryService extends AbstractDiscoveryService implements MsgListener {
	private static final Logger logger = LoggerFactory.getLogger(ZBPLMDiscoveryService.class);

	/**
	 * Default search time (2m)
	 */
	private final static int SEARCH_TIME = 120;

	private final Set<ZBPLMHandler> handlers = new CopyOnWriteArraySet<>();

	private final Set<InsteonDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();

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
		while (!port.getModemDBBuilder().isComplete() && waitTime < getScanTimeout()) {
			Thread.sleep(100);
			waitTime += 100;
		}
		
		if(!port.getModemDBBuilder().isComplete()) {
			logger.warn("DB download not complete, skipping scan");
			return;
		}

		Driver driver = port.getDriver();
		try {
			Map<InsteonAddress, ModemDBEntry> entries = driver.lockModemDBEntries();
			port.addListener(this);

			for (InsteonAddress address : entries.keySet()) {
				logger.info("Sending discovery message to:" + address.toString());
				Msg msg = Msg.makeMessage("SendStandardMessage");
				msg.setAddress("toAddress", address);
				msg.setByte("messageFlags", (byte) 0x0F);
				msg.setByte("command1", (byte) 0x10);
				msg.setByte("command2", (byte) 0x00);
				port.writeMessage(msg);
				

			}
			
			// Need to wait a bit so the port has a chance to get some replies
			Thread.sleep(getScanTimeout());

		} catch (FieldException e) {
			logger.error("Error sending device type request", e);
		} finally {
			driver.unlockModemDBEntries();
			port.removeListener(this);
		}

	}

	// Messages from the modem about new devices will come in here
	@Override
	public void msg(Msg msg, ZBPLMHandler fromPort) {
		logger.info("discovery got message:" + msg.toString());
		if (msg.getName().equals(SmartenItZBPLMBindingConstants.GET_IM_INFO_REPLY)) {
			try {
				logger.info("Got expected reply:"+ msg.toString());
				InsteonDeviceInformation deviceInformation = new InsteonDeviceInformation();
				deviceInformation.setAddress(msg.getAddr(SmartenItZBPLMBindingConstants.IM_ADDRESS));
				deviceInformation.setDeviceCategory(msg.getByte(SmartenItZBPLMBindingConstants.DEVICE_CATEGORY));
				deviceInformation.setDeviceSubCategory(msg.getByte(SmartenItZBPLMBindingConstants.DEVICE_SUB_CATEGORY));
				deviceInformation.setFirmwareVersion(msg.getByte(SmartenItZBPLMBindingConstants.FIRMWARE_VERSION));
				deviceInformation.setHandler(fromPort);
				for(InsteonDiscoveryParticipant participant: participants) {
					DiscoveryResult discoveryResult = participant.createResult(deviceInformation);
					if(discoveryResult != null) {
						thingDiscovered(discoveryResult);
					}
				}
				
			} catch (FieldException e) {
				logger.error("Error loading field for msg:" + msg.toString(), e);
			} 

		} else {
			logger.error("Unexpected message type:" + msg.getName());
		}
	}

}
