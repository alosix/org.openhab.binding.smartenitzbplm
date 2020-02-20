package org.openhab.binding.smartenitzbplm.thing.listener;

import org.openhab.binding.smartenitzbplm.internal.message.Msg;

/**
 * Used to shut down the threads and queues for the message listeners
 * This should never actually be seen by the listeners
 * @author jpowers
 *
 */
public class ShutdownMsg extends Msg {
	public ShutdownMsg() {
		super(0, new byte[0],0 , null);
	}

}
