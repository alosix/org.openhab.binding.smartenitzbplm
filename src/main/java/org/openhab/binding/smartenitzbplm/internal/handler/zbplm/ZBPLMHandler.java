package org.openhab.binding.smartenitzbplm.internal.handler.zbplm;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
import org.openhab.binding.smartenitzbplm.internal.device.DeviceTypeLoader;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
import org.openhab.binding.smartenitzbplm.internal.device.InsteonDevice;
import org.openhab.binding.smartenitzbplm.internal.message.FieldException;
import org.openhab.binding.smartenitzbplm.internal.message.Msg;
import org.openhab.binding.smartenitzbplm.internal.message.MsgFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thing supports the SmartenIt Zigbee/Insteon Power Line Modem
 * 
 * @author jpowers
 *
 */
public class ZBPLMHandler extends BaseBridgeHandler {
	private final Logger logger = LoggerFactory.getLogger(ZBPLMHandler.class);

	


	private ConcurrentMap<InsteonAddress, InsteonDevice> devices = null;
	private Port port = null;
	private IOStream ioStream = null;
	private SerialPortManager serialPortManager;
	private MsgFactory msgFactory = new MsgFactory();
	private DeviceTypeLoader deviceTypeLoader;

	public ZBPLMHandler(Bridge bridge, SerialPortManager serialPortManager, DeviceTypeLoader deviceTypeLoader) {
		super(bridge);

		ZBPLMConfig config = getConfigAs(ZBPLMConfig.class);
		this.serialPortManager = serialPortManager;
		this.deviceTypeLoader = deviceTypeLoader;
		this.devices = new ConcurrentHashMap<>();
		this.ioStream = new SerialIOStream(serialPortManager, config.zbplm_port, config.zbplm_baud);
		this.port = new Port(this);
		// TODO: reallyneed to remove the driver.. its carp
		this.port.setModemDBRetryTimeout(120000); // TODO: JWP add config

		boolean portStarted = this.port.start();
		if (portStarted) {
			this.updateStatus(ThingStatus.ONLINE);
		} else {
			port.stop();
			this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
		}

	}

	public Bridge getBridge() {
		return super.getBridge();
	}
	
	
	public Port getPort() {
		return port;
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
		logger.info("Got a command:" + channelUID.toString() + ":" + command.toFullString());
		// TODO Auto-generated method stub

	}

	public ConcurrentMap<InsteonAddress, InsteonDevice> getDevices() {
		return devices;
	}




	public IOStream getIoStream() {
		return ioStream;
	}

	public DeviceTypeLoader getDeviceTypeLoader() {
		return deviceTypeLoader;
	}


	public SerialPortManager getSerialPortManager() {
		return serialPortManager;
	}

	public MsgFactory getMsgFactory() {
		return msgFactory;
	}
	/**
	 * 
	 * @param scanTimeout
	 */
	public void startScan(int scanTimeout) {
		logger.info("Starting scan");
		Msg msg;
		try {
			msg = Msg.makeMessage("StartALLLinking");
			msg.setByte("LinkCode", (byte) 0x01);
			msg.setByte("ALLLinkGroup", (byte) 0x01); // everything about uses 0x01 so far
			//port.writeMessage(msg);

		} catch (IOException | FieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}




	

}
