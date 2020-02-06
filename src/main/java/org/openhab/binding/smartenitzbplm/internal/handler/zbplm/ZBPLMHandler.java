package org.openhab.binding.smartenitzbplm.internal.handler.zbplm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonDevice;


/**
 * This thing supports the SmartenIt Zigbee/Insteon Power Line Modem
 * @author jpowers
 *
 */
public class ZBPLMHandler extends BaseBridgeHandler {
	private Driver driver = null;
	private ConcurrentMap<InsteonAddress, InsteonDevice> devices = null;
	private Port port = null;
	private IOStream ioStream = null;
	private SerialPortManager serialPortManager;
	
	public ZBPLMHandler(Bridge bridge, SerialPortManager serialPortManager) {
		super(bridge);
		
		ZBPLMConfig config = getConfigAs(ZBPLMConfig.class);
		this.serialPortManager = serialPortManager;
		this.driver = new Driver();
		this.devices = new ConcurrentHashMap<>();
		this.ioStream = new SerialIOStream(serialPortManager, config.zbplm_port,config.zbplm_baud);
		this.port = new Port(driver, ioStream);
		this.port.setModemDBRetryTimeout(120000); // TODO: JWP add config
		boolean portStarted = this.port.start();
		if(portStarted) {
			this.updateStatus(ThingStatus.ONLINE);	
		} else {
			port.stop();
			this.updateStatus(ThingStatus.OFFLINE,ThingStatusDetail.BRIDGE_OFFLINE);
		}
		
		
		
	}

	@Override
	public void handleRemoval() {
		// TODO Auto-generated method stub
		super.handleRemoval();
		this.port.stop();
	}

	@Override
	public void dispose() {
		super.dispose();
		this.port.stop();
	}
	
	
	@Override
	public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
		// TODO Auto-generated method stub
		super.handleConfigurationUpdate(configurationParameters);
	}

	
	@Override
	public void thingUpdated(Thing thing) {
		// TODO Auto-generated method stub
		super.thingUpdated(thing);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 * @param scanTimeout
	 */
	public void startScan(int scanTimeout) {
		
	}

}
