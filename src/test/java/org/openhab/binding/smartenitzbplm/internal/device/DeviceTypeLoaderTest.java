package org.openhab.binding.smartenitzbplm.internal.device;

import static org.junit.Assert.*;

import org.junit.Test;

public class DeviceTypeLoaderTest {

	@Test
	public void testLoad() {
		DeviceTypeLoader loader = new DeviceTypeLoader();
		assertTrue(loader.getDeviceTypes().size() > 0);
	}

}
