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

/**
 * The {@link SmartenItZBPLMConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Jason Powers - Initial contribution
 */
public class SmartenItZBPLMConfiguration {

    /**
     * The Refresh interval
     */
    public Integer refresh;
    
    /**
     * The device dead count
     */
    public Long deadDeviceCount;

	public Double rampTime;

	public String cmd;

	public int dimmerMax;
	
	public Integer rampLevel;
	
	public Integer group;

	public String name;

	
}
