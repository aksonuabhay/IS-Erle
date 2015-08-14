package is.erle.comm.serial;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.service.comm.serial.SerialCommunicationEndpoint;
import interactivespaces.service.comm.serial.SerialCommunicationEndpointService;
import interactivespaces.util.concurrency.ManagedCommand;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleCommSerialActivity extends BaseRoutableRosActivity {

	/**
	 * The name of the config property for obtaining the publisher List.
	 */
	private static final String CONFIGURATION_PUBLISHER_NAME = "space.activity.routes.outputs";
	
	/**
	 * The name of the config property for obtaining the subscriber List.
	 */
	private static final String CONFIGURATION_SUBSCRIBER_NAME = "space.activity.routes.inputs";
	
	/**
	 * The topic names for publishing data
	 * PUBLISHER MAPPING
	 * 
	 * publishers[0] -> output 
	 * Topic Name : comms/output
	 * Usage : Send output to the mavlink activity after receiving it from the drone.
	 */
	private static String publishers[];

	/**
	 * The topic names for subscribing data 
	 * SUBSCRIBER MAPPING
	 * 
	 * subscribers[0] -> input 
	 * Topic Name : comms/input
	 * Usage : Receive data from mavlink activity and send it to the drone.
	 */
	private static String subscribers[];
	
	/**
	 * A counter to count the the number of calls sendOutputJson calls.
	 */
	private static long jsonOutputCounter = 0;
	
	/**
	 * A counter to count the the number of calls onNewInputJson calls.
	 */
	private static long jsonInputCounter = 0 ;

	/**
	 * A serial object to handle all the serial communication with the drone.
	 */
	private static SerialCommunicationEndpoint serial;
	
	/**
	 * A global object to store the received serial data
	 */
	private static byte[] serialData;

	@Override
	public void onActivitySetup() {
		getLog().info("Activity is.erle.comm.serial setup");

        publishers = getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME).split(":");
        subscribers = getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME).split(":");
        
		SerialCommunicationEndpointService serialService = getSpaceEnvironment()
				.getServiceRegistry().getRequiredService(
						SerialCommunicationEndpointService.SERVICE_NAME);
		String portName = getConfiguration().getRequiredPropertyString(
				"space.hardware.serial.port");
		serial = serialService.newSerialEndpoint(portName);
		serial.setBaud(115200);
		serial.setInputBufferSize(10000);
		serial.setOutputBufferSize(1000);
		serialData = new byte[600];
		serial.startup();

		ManagedCommand threadSender = getManagedCommands().submit(new Runnable() {
						public void run() {
							while (!Thread.interrupted())
						{
								while (serial.available() >0) 
								{
									int tempInt = serial.read(serialData);
									serialData = ArrayUtils.subarray(serialData, 0, tempInt);
									Map<String, Object> temp = Maps.newHashMap();
									temp.put(publishers[0], Arrays.toString(serialData));
									sendOutputJson("output", temp);
								}
								
							}
						}
					});
	}

	@Override
	public void onActivityStartup() {
		getLog().info("Activity is.erle.comm.serial startup");
	}

	@Override
	public void onActivityPostStartup() {
		getLog().info("Activity is.erle.comm.serial post startup");
	}

	@Override
	public void onActivityActivate() {
		getLog().info("Activity is.erle.comm.serial activate");
		jsonOutputCounter = 0;
		/*
		 * Map<String,Object> temp=Maps.newHashMap();
		 * temp.put(Long.toString(jsonOutputCounter++), "ACTIVATE");
		 * sendOutputJson("output", temp);
		 */
	}

	@Override
	public void onActivityDeactivate() {
		getLog().info("Activity is.erle.comm.serial deactivate");
		
//		 Map<String, Object> temp = Maps.newHashMap();
//		 temp.put(Long.toString(jsonOutputCounter), "DEACTIVATE");
//		 sendOutputJson("output", temp);
		 
	}

	@Override
	public void onActivityPreShutdown() {
		getLog().info("Activity is.erle.comm.serial pre shutdown");
		serial.shutdown();
	}

	@Override
	public void onActivityShutdown() {
		getLog().info("Activity is.erle.comm.serial shutdown");
	}

	@Override
	public void onActivityCleanup() {
		getLog().info("Activity is.erle.comm.serial cleanup");
	}

	@Override
	public void onNewInputJson(String channelName, Map<String, Object> message)
	{
		if (subscribers[0].equals(channelName))
		{
			byte[] responseGlobal;
			String items[] = message.get("comm").toString()
					.replaceAll("\\[", "").replaceAll("\\]", "")
					.replaceAll(" ", "").split(",");
			int lenItems = items.length;
			responseGlobal = new byte[lenItems];
			for (int i = 0; i < lenItems; i++)
			{
				try
				{
					responseGlobal[i] = Byte.parseByte(items[i]);
				}
				catch (NumberFormatException e)
				{
					getLog().error(e);
				}

			}
			getLog().debug(Arrays.toString(responseGlobal));
			serial.write(responseGlobal);
			jsonInputCounter++; // Take care of this variable
		}
	}
}
