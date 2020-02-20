package org.openhab.binding.smartenitzbplm.thing.discovery;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.THING_TYPE_LAMPLINC_2457D2;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.INSTEON_ADDRESS;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants;
import org.openhab.binding.smartenitzbplm.internal.discovery.ModemDiscoveryParticipant;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = InsteonDiscoveryParticipant.class)
public class LampLincDiscoveryParticipant extends InsteonBaseDiscoveryParticipant {
	private static final int LAMPLINC_2457D2_SUBCAT = 0x0e;

	private static final int LAMPLINC_CAT = 0x01;

	private static final Logger logger = LoggerFactory.getLogger(ModemDiscoveryParticipant.class);

	private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_LAMPLINC_2457D2);
	
	@Override
	public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
		return Collections.singleton(SmartenItZBPLMBindingConstants.THING_TYPE_LAMPLINC_2457D2);
	}

	@Override
	public @Nullable DiscoveryResult createResult(InsteonDeviceInformation deviceInformation) {
		if(deviceInformation.getDeviceCategory() == LAMPLINC_CAT && deviceInformation.getDeviceSubCategory() == LAMPLINC_2457D2_SUBCAT) {
			return DiscoveryResultBuilder
					.create(getUID(deviceInformation))
					.withRepresentationProperty(INSTEON_ADDRESS)
					.withBridge(deviceInformation.getHandler().getThing().getUID())
					.withProperty(INSTEON_ADDRESS, deviceInformation.getAddress().toString())
					.build();

		}
		
		return null;
	}

	@Override
	public @Nullable ThingUID getThingUID(InsteonDeviceInformation deviceInformation) {
		return new ThingUID(THING_TYPE_LAMPLINC_2457D2, getAddress(deviceInformation));
	}
	
	
	private ThingUID getUID(InsteonDeviceInformation deviceInformation) {
		return getThingUID(deviceInformation);
	}
	
	private String getAddress(InsteonDeviceInformation deviceInformation) {
		return deviceInformation.getAddress().toString().replaceAll("\\.", "");
	}

}
