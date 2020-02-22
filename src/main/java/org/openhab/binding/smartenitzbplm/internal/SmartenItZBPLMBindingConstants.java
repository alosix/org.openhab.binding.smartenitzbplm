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
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;

/**
 * The {@link SmartenItZBPLMBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Jason Powers - Initial contribution
 */
@NonNullByDefault
public class SmartenItZBPLMBindingConstants {

    static final String BINDING_ID = "smartenitzbplm";

    public static final ThingTypeUID THING_TYPE_PLM_COORDINATOR = new ThingTypeUID(BINDING_ID, "coordinator_zbplm");
    public static final ThingTypeUID THING_TYPE_LAMPLINC_2457D2 = new ThingTypeUID(BINDING_ID, "lamplinc_2457D2");
    
    
    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";
    
	
    // List of configuration keys
    public static final String ZBPLM_PORT = "zbplm_port";
	public static final String ZBPLM_BAUD = "zbplm_baud";
	public static final String ZBPLM_FLOWCONTROL =  "zbplm_flowcontrol";

	
	public static final String INSTEON_ADDRESS = "insteon_address";
	
	 // List of configuration values for flow control
    public static final Integer FLOWCONTROL_CONFIG_NONE = Integer.valueOf(0);
    public static final Integer FLOWCONTROL_CONFIG_HARDWARE_CTSRTS = Integer.valueOf(1);
    public static final Integer FLOWCONTROL_CONFIG_SOFTWARE_XONXOFF = Integer.valueOf(2);


    // Message Constants
    public static final String SEND_STANDARD_MESSAGE = "SendStandardMessage";
    public static final String SEND_EXTENDED_MESSAGE = "SendExtendedMessage";
    public static final String STANDARD_MESSAGE_RECEIVED = "StandardMessageReceived";
    
    public static final String GET_IM_INFO_REPLY = "GetIMInfoReply";
	public static final String FIRMWARE_VERSION = "FirmwareVersion";
	public static final String DEVICE_SUB_CATEGORY = "DeviceSubCategory";
	public static final String DEVICE_CATEGORY = "DeviceCategory";
	public static final String IM_ADDRESS = "IMAddress";
	
	public static final String TO_ADDRESS = "toAddress";
	public static final String FROM_ADDRESS = "fromAddress";
	
	public static final String MESSAGE_FLAGS = "messageFlags";
	public static final String RECORD_FLAGS = "recordFlags";
	public static final String CONTROL_CODE = "controlCode";
	
	public static final String COMMAND_1 = "command1";
	public static final String COMMAND_2 = "command2";
	
	public static final String USER_DATA_1 = "userData1";
	public static final String USER_DATA_2 = "userData2";
	public static final String USER_DATA_3 = "userData3";
	public static final String USER_DATA_4 = "userData4";
	public static final String USER_DATA_5 = "userData5";
	public static final String USER_DATA_6 = "userData6";
	public static final String USER_DATA_7 = "userData7";
	public static final String USER_DATA_8 = "userData8";
	public static final String USER_DATA_9 = "userData9";
	public static final String USER_DATA_10 = "userData10";
	public static final String USER_DATA_11 = "userData11";
	public static final String USER_DATA_12 = "userData12";
	public static final String USER_DATA_13 = "userData13";
	public static final String USER_DATA_14 = "userData14";
	
	public static final String LINK_DATA_1 = "LinkData1";
	public static final String LINK_DATA_2 = "LinkData2";
	public static final String LINK_DATA_3 = "LinkData3";
	
}
