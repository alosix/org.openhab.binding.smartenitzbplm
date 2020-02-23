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
public class ThermostatDiscoveryParticipant extends InsteonBaseDiscoveryParticipant {
	private static final int THERMOSTAT_CAT = 0x05;
	
	private static final int THERMOSTAT_SUBCAT_2441TH = 0x0b;
		

	private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>();
	static {
		SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_THERMOSTAT_2441TH);
	}
	
	@Override
	public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
		return SUPPORTED_THING_TYPES_UIDS;
	}

	@Override
	public @Nullable DiscoveryResult createResult(InsteonDeviceInformation deviceInformation) {
		if(deviceInformation.getDeviceCategory() != THERMOSTAT_CAT) {
			// not a thermostat
			return null;
		}
		if(deviceInformation.getDeviceSubCategory() == THERMOSTAT_SUBCAT_2441TH) {
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
		return new ThingUID(THING_TYPE_THERMOSTAT_2441TH, getAddress(deviceInformation));
		
	}
	
	
	private ThingUID getUID(InsteonDeviceInformation deviceInformation) {
		return getThingUID(deviceInformation);
	}
	
	private String getAddress(InsteonDeviceInformation deviceInformation) {
		return deviceInformation.getAddress().toString().replaceAll("\\.", "");
	}

}
