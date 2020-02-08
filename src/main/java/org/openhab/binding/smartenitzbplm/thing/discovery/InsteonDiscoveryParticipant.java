package org.openhab.binding.smartenitzbplm.thing.discovery;

import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;


public interface InsteonDiscoveryParticipant {

	/**
     * Defines the list of thing types that this participant can identify
     *
     * @return a set of thing type UIDs for which results can be created
     */
    public Set<ThingTypeUID> getSupportedThingTypeUIDs();
    
    
    public @Nullable DiscoveryResult createResult(InsteonDeviceInformation deviceInformation);

    public @Nullable ThingUID getThingUID(InsteonDeviceInformation deviceInformation);
}
