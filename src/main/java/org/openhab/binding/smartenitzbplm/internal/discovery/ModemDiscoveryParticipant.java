package org.openhab.binding.smartenitzbplm.internal.discovery;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.THING_TYPE_PLM_COORDINATOR;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.eclipse.smarthome.config.discovery.usbserial.UsbSerialDiscoveryParticipant;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = UsbSerialDiscoveryParticipant.class)
public class ModemDiscoveryParticipant implements UsbSerialDiscoveryParticipant {
	private static final Logger logger = LoggerFactory.getLogger(ModemDiscoveryParticipant.class);

	private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_PLM_COORDINATOR);

	public static final int VENDOR_ID = 0x10c4;
	public static final int PRODUCT_ID = 0xea60;
	
	@Override
	public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
		return SUPPORTED_THING_TYPES_UIDS;
	}


	@Override
	public @Nullable DiscoveryResult createResult(UsbSerialDeviceInformation deviceInformation) {
		if(deviceInformation.getVendorId() == VENDOR_ID && deviceInformation.getProductId() == PRODUCT_ID) {
			logger.info("Found matching usb devices at:" + deviceInformation.getSerialPort());
			return DiscoveryResultBuilder
					.create(getUID(deviceInformation))
					.withRepresentationProperty(ZBPLM_PORT)
					.withProperty(ZBPLM_BAUD, 115200)
					.withProperty(ZBPLM_PORT, deviceInformation.getSerialPort())
					.withProperty(ZBPLM_FLOWCONTROL, FLOWCONTROL_CONFIG_NONE)
					.build();
			
		} else {
			return null;
		}
	}


	@Override
	public @Nullable ThingUID getThingUID(UsbSerialDeviceInformation deviceInformation) {
		if(deviceInformation.getVendorId() == VENDOR_ID && deviceInformation.getProductId() == PRODUCT_ID) {
			return getUID(deviceInformation);
		} else {
			return null;
		}
	}
	
	private ThingUID getUID(UsbSerialDeviceInformation deviceInformation) {
		return new ThingUID(THING_TYPE_PLM_COORDINATOR, deviceInformation.getSerialNumber());
	}
	

	
}
