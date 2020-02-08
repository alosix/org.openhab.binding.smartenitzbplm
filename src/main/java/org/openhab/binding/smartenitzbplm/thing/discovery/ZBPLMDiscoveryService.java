package org.openhab.binding.smartenitzbplm.thing.discovery;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
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

@Component(immediate = true, service = ZBPLMDiscoveryService.class, configurationPid = "discovery.zbplm")
public class ZBPLMDiscoveryService extends AbstractDiscoveryService  {
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
	protected void addZBPLMDiscoveryParticipant(InsteonDiscoveryParticipant participant) {
		logger.info("**************************Adding discovery participant:" + participant.toString());
		participants.add(participant);
	}

	protected void removeZBPLMDiscoveryParticipant(InsteonDiscoveryParticipant participant) {
		participants.remove(participant);
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void addZBPLMHandler(ZBPLMHandler handler) {
		logger.info("*************************Adding handler:" + handler);
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
		logger.debug("Deactivating SmartenIt discovery service");
	}

	@Override
	protected void startScan() {
		logger.info("Starting scan");
		for (ZBPLMHandler handler : handlers) {
			handler.startScan(this.getScanTimeout());

		}

	}
	

}
