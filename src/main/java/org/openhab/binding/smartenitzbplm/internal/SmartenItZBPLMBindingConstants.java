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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link SmartenItZBPLMBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Jason Powers - Initial contribution
 */
@NonNullByDefault
public class SmartenItZBPLMBindingConstants {

    static final String BINDING_ID = "smartenitzbplm";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_GENERIC_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    
    public static final ThingTypeUID THING_TYPE_PLM_COORDINATOR = new ThingTypeUID(BINDING_ID, "coordinator_zbplm");

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";
    
	
    // List of configuration keys
    public static final String ZBPLM_PORT = "zbplm_port";
	public static final String ZBPLM_BAUD = "zbplm_baud";
	public static final String ZBPLM_FLOWCONTROL =  "zbplm_flowcontrol";

	
	 // List of configuration values for flow control
    public static final Integer FLOWCONTROL_CONFIG_NONE = Integer.valueOf(0);
    public static final Integer FLOWCONTROL_CONFIG_HARDWARE_CTSRTS = Integer.valueOf(1);
    public static final Integer FLOWCONTROL_CONFIG_SOFTWARE_XONXOFF = Integer.valueOf(2);
}
