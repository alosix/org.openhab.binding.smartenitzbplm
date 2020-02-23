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

import static org.openhab.binding.smartenitzbplm.internal.SmartenItZBPLMBindingConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.smartenitzbplm.internal.device.DeviceTypeLoader;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonDevice;
import org.openhab.binding.smartenitzbplm.internal.handler.zbplm.ZBPLMHandler;
import org.openhab.binding.smartenitzbplm.thing.InsteonDimmerThingHandler;
import org.openhab.binding.smartenitzbplm.thing.InsteonSwitchThingHandler;
import org.openhab.binding.smartenitzbplm.thing.InsteonThermostatThingHandler;
import org.osgi.framework.ServiceRegistration;
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

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<ThingTypeUID>();
    
    
    static {
    	SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_PLM_COORDINATOR);
    	SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_LAMPLINC_2457D2);
    	SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_GENERIC_SWITCH);
    	SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_TOGGLELINC_2446SW);
    	SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_IOLINC_2450);
    	SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_THERMOSTAT_2441TH);
    }
    
    private final Map<ThingUID, ServiceRegistration<?>> coordinatorHandlerRegs = new HashMap<>();

        
    private ConcurrentHashMap<InsteonAddress, InsteonDevice> devices = new ConcurrentHashMap<InsteonAddress, InsteonDevice>(); // list of all configured devices
    
    private long devicePollInterval = 300000L; // in milliseconds
    private long deadDeviceTimeout = -1L;
    private long refreshInterval = 600000L; // in milliseconds
    private int messagesReceived = 0;
    private boolean isActive = false; // state of binding
    private boolean hasInitialItemConfig = false;
    private @Nullable SerialPortManager serialPortManager = null;

	private @Nullable DeviceTypeLoader deviceTypeLoader = null;
    
    
    @Reference
    public void setSerialPortManager(SerialPortManager serialPortManager) {
    	this.serialPortManager = serialPortManager;
    }

    @Reference
    public void setDeviceTypeLoader(DeviceTypeLoader deviceTypeLoader) {
    	this.deviceTypeLoader  = deviceTypeLoader;
    }

	@Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }
	

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if(THING_TYPE_PLM_COORDINATOR.equals(thingTypeUID)) {
        	logger.info("creating zbplm handler");
        	if(coordinatorHandlerRegs.containsKey(thing.getUID())) {
        		logger.warn("Thing already exists");
        		return (ThingHandler) coordinatorHandlerRegs.get(thing.getUID()).getReference();
        	}
        	
        	
        	ZBPLMHandler handler =  new ZBPLMHandler((Bridge) thing, serialPortManager, deviceTypeLoader);
        	
        	
        	// Save the reference and get it registered as a service so OSGI things can find it
        	coordinatorHandlerRegs.put(handler.getThing().getUID(),
        			bundleContext.registerService(ZBPLMHandler.class.getName(), handler, new Hashtable<>()));
        	return handler;
        }
        
        if(THING_TYPE_LAMPLINC_2457D2.equals(thingTypeUID)) {
        	InsteonDimmerThingHandler handler = new InsteonDimmerThingHandler(thing);
        	return handler;
        }
        
        if(THING_TYPE_GENERIC_SWITCH.equals(thingTypeUID) ||
        		THING_TYPE_TOGGLELINC_2446SW.equals(thingTypeUID)) {
        	InsteonSwitchThingHandler handler = new InsteonSwitchThingHandler(thing);
        	return handler;
        }
        
        if(THING_TYPE_IOLINC_2450.equals(thingTypeUID)) {
        	InsteonSwitchThingHandler handler = new InsteonSwitchThingHandler(thing);
        	return handler;
        }
        
        if(THING_TYPE_THERMOSTAT_2441TH.equals(thingTypeUID)) {
        	InsteonThermostatThingHandler handler = new InsteonThermostatThingHandler(thing);
        	return handler;
        }
        
        

        return null;
    }
}
