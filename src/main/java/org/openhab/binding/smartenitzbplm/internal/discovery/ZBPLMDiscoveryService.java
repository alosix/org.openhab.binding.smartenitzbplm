package org.openhab.binding.smartenitzbplm.internal.discovery;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.zbplm")
public class ZBPLMDiscoveryService extends AbstractDiscoveryService {
	private static final Logger logger = LoggerFactory.getLogger(ZBPLMDiscoveryService.class);

	/**
	 * Default search time
	 */
	private final static int SEARCH_TIME = 60;

	private final Set<ZBPLMHandler> handlers = new CopyOnWriteArraySet<>();

	private final Set<ZBPLMDiscoveryParticipant> participants = new CopyOnWriteArraySet<>();

	public ZBPLMDiscoveryService() throws IllegalArgumentException {
		super(SEARCH_TIME);
		logger.info("Discovery service created");

	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void addZBPLMDiscoveryParticipant(ZBPLMDiscoveryParticipant participant) {
		logger.info("Adding discovery participant:" + participant.toString());
		participants.add(participant);
	}

	protected void removeZBPLMDiscoveryParticipant(ZBPLMDiscoveryParticipant participant) {
		participants.remove(participant);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void addZBPLMHandler(ZBPLMHandler handler) {
		logger.info("Adding handler:" + handler);
		handlers.add(handler);
	}

	protected void removeZBPLMHandler(ZBPLMHandler handler) {
		handlers.remove(handler);
	}

	@Override
	@Activate
	protected void activate(Map<String, Object> configProperties) {
		logger.debug("Activating ZBPLM discovery service");
		super.activate(configProperties);
		startScan();
	}

	@Override
	@Modified
	protected void modified(Map<String, Object> configProperties) {
		super.modified(configProperties);
	}

	@Override
	@Deactivate
	public void deactivate() {
		logger.debug("Deactivating Bluetooth discovery service");
	}

	@Override
	protected void startScan() {
		logger.info("Starting scan");
		for (ZBPLMHandler handler : handlers) {
			handler.startScan(this.getScanTimeout());

		}

	}

}
