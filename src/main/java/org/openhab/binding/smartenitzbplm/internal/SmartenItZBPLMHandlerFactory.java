/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.smartenitzbplm.internal;

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.THING_TYPE_GENERIC_DEVICE;
import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.THING_TYPE_PLM_COORDINATOR;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonDevice;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * The {@link SmartenItZBPLMHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Jason Powers - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.smartenitzbplm", service = ThingHandlerFactory.class)
public class SmartenItZBPLMHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(SmartenItZBPLMHandlerFactory.class);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_PLM_COORDINATOR);
    
    //private Driver driver = null;
    //private PortListener portListener = null;
    
    private ConcurrentHashMap<InsteonAddress, InsteonDevice> devices = new ConcurrentHashMap<InsteonAddress, InsteonDevice>(); // list of all configured devices
    
    private long devicePollInterval = 300000L; // in milliseconds
    private long deadDeviceTimeout = -1L;
    private long refreshInterval = 600000L; // in milliseconds
    private int messagesReceived = 0;
    private boolean isActive = false; // state of binding
    private boolean hasInitialItemConfig = false;
    private @Nullable SerialPortManager serialPortManager = null;
    
    
    @Reference
    public void bindSerialPortManager(SerialPortManager serialPortManager) {
    	this.serialPortManager = serialPortManager;
    }
    

	//private DeviceTypeLoader bindDeviceTypeLoader;

    // TODO: JWP: Add back in when the references exist
//    @Reference
//    public void bindDeviceTypeLoader(DeviceTypeLoader deviceTypeLoader) {
//    	//this.bindDeviceTypeLoader = deviceTypeLoader;
//    }
//
//    @Override
//	protected void activate(ComponentContext componentContext) {
//		super.activate(componentContext);
//		//driver = new Driver();
//		
//		//portListener = new PortListener(driver, devices);
//	}

	@Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
		
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if(THING_TYPE_PLM_COORDINATOR.equals(thingTypeUID)) {
        	return new ZBPLMHandler((Bridge) thing, serialPortManager);
        }

        if (THING_TYPE_GENERIC_DEVICE.equals(thingTypeUID)) {
            return new SmartenItZBPLMHandler(thing);
        }

        return null;
    }
}
