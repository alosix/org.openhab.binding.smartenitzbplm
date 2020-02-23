package org.openhab.binding.smartenitzbplm.thing.discovery;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.THING_TYPE_GENERIC_SWITCH;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.THING_TYPE_LAMPLINC_2457D2;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.THING_TYPE_TOGGLELINC_2446SW;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.smartenitzbplm.internal.discovery.ModemDiscoveryParticipant;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = InsteonDiscoveryParticipant.class)
public class IOLincDiscoveryParticipant extends InsteonBaseDiscoveryParticipant {
	private static final int IOLINC_CAT = 0x07;

	private static final int IOLINC_SUB_CAT = 0x00;
	

	private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>();
	static {
		SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_IOLINC_2450);
	}
	
	@Override
	public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
		return SUPPORTED_THING_TYPES_UIDS;
	}

	@Override
	public @Nullable DiscoveryResult createResult(InsteonDeviceInformation deviceInformation) {
		if(deviceInformation.getDeviceCategory() != IOLINC_CAT) {
			// not a toggle switch
			return null;
		}
		if(deviceInformation.getDeviceSubCategory() == IOLINC_SUB_CAT) {
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
		return new ThingUID(THING_TYPE_IOLINC_2450, getAddress(deviceInformation));

	}
	
	
	private ThingUID getUID(InsteonDeviceInformation deviceInformation) {
		return getThingUID(deviceInformation);
	}
	
	private String getAddress(InsteonDeviceInformation deviceInformation) {
		return deviceInformation.getAddress().toString().replaceAll("\\.", "");
	}

}
