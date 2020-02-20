package org.openhab.binding.smartenitzbplm.internal.handler.zbplm;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingDeque;

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
import org.openhab.binding.smartenitzbplm.internal.message.MsgListener;
import org.openhab.binding.smartenitzbplm.thing.listener.InsteonMsgListener;
import org.openhab.binding.smartenitzbplm.thing.listener.ShutdownMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This thing supports the SmartenIt Zigbee/Insteon Power Line Modem
 * 
 * @author jpowers
 *
 */
public class ZBPLMHandler extends BaseBridgeHandler implements MsgListener {
	private final Logger logger = LoggerFactory.getLogger(ZBPLMHandler.class);

	private ConcurrentMap<InsteonAddress, InsteonDevice> devices = null;
	private Port port = null;
	private IOStream ioStream = null;
	private SerialPortManager serialPortManager;
	private MsgFactory msgFactory = new MsgFactory();
	private DeviceTypeLoader deviceTypeLoader;
	private ZBPLMConfig config = null;
	private ExecutorService executorService = null;

	public ExecutorService getExecutorService() {
		return executorService;
	}

	// holds the queues for the message listners so they don't block the world
	private Map<InsteonMsgListener, BlockingQueue<Msg>> messageQueues = new ConcurrentHashMap<>();

	public ZBPLMHandler(Bridge bridge, SerialPortManager serialPortManager, DeviceTypeLoader deviceTypeLoader) {
		super(bridge);

		this.config = getConfigAs(ZBPLMConfig.class);
		this.serialPortManager = serialPortManager;
		this.deviceTypeLoader = deviceTypeLoader;
		this.devices = new ConcurrentHashMap<>();

	}

	@Override
	public void initialize() {
		super.initialize();
		this.executorService = ForkJoinPool.commonPool();
		this.ioStream = new SerialIOStream(serialPortManager, config.zbplm_port, config.zbplm_baud, msgFactory);
		this.port = new Port(this);
		this.port.addListener(this);
		
		this.port.setModemDBBuilder(new ModemDBBuilder(this));
		this.port.setModemDBRetryTimeout(120000); // TODO: JWP add config
		
		final Port port = this.port;
		executorService.execute(new Runnable() {

			@Override
			public void run() {
				port.start();

			}
		});

	}
	public void setPortStatus(boolean up) {
		if (up) {
			this.updateStatus(ThingStatus.ONLINE);
		} else {
			this.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
		}
	}

	public void addInsteonMsgListener(final InsteonMsgListener listener) {
		logger.info("Adding msglistener:" + listener.toString());
		BlockingQueue<Msg> msgQueue = new LinkedBlockingDeque<Msg>();
		messageQueues.put(listener, msgQueue);
		
		Runnable msgRunnable = new Runnable() {

			@Override
			public void run() {
				while (true) {
					Msg msg = null;
					try {
						msg = msgQueue.take();
						logger.info("Taking message:" + msg.toString());
						if (msg == null || ShutdownMsg.class.isInstance(msg)) {
							return;
						}
						// Pass the message off to the listener
						listener.onMessage(msg);
					} catch (InterruptedException e) {

					}

				}

			}
		};
		executorService.execute(msgRunnable);

	}

	@Override
	public void msg(Msg msg, ZBPLMHandler handler) {
		logger.info("Got a message:" + msg.toString());
		Collection<BlockingQueue<Msg>> values = messageQueues.values();
		for (BlockingQueue<Msg> queue : values) {
			logger.info("Dispatching message to queues:" + msg.toString());
			queue.offer(msg);
		}
	}

	public void sendMsg(Msg msg) {
		try {
			port.writeMessage(msg);
		} catch (IOException e) {
			logger.error("Unable to write message" + msg.toString(), e);
		}
	}

	public Bridge getBridge() {
		return super.getBridge();
	}

	public Port getPort() {
		return port;
	}

	@Override
	public void dispose() {
		super.dispose();
		if(this.port != null) {
			this.port.stop();
		}
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

	
}
