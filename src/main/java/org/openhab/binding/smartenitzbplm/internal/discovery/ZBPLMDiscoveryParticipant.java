package org.openhab.binding.smartenitzbplm.internal.discovery;

import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;


public interface ZBPLMDiscoveryParticipant {

	/**
     * Defines the list of thing types that this participant can identify
     *
     * @return a set of thing type UIDs for which results can be created
     */
    public Set<ThingTypeUID> getSupportedThingTypeUIDs();

}
