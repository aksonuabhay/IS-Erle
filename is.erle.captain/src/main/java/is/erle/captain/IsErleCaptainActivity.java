package is.erle.captain;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.util.concurrency.ManagedCommand;

/**
 * IsErleCaptainActivity is class which handles all tasks a real Captain of a
 * Drone would handle. This class extends BaseRoutableRosActivity class to use
 * its publihser/subscriber modules.
 * <p>
 * This activity receives data from mavlink activity. It monitors the remote
 * Drone using this data. It has sendCommand method which allows to command
 * mavlink activity. It can command any drone using this simple sendCommand
 * function. All it has to do is to send a command from a list of Commands.
 * <p>
 * It will act as the captain of the mission. It will arm/disarm the motors, it
 * will make sure that all of the text file has been transmitted before letting
 * the drone fly. It will see drone's neighbourhood, battery status, current and
 * planned course and all other things which a drone pilot has to do to maintain
 * the safety of the drone and its surroundings. It monitors heartbeat of the
 * drones and maintains data about the connected drones.
 * 
 * @author Abhay Kumar
 * @version %I%, %G%
 * @since 1.0.0
 */
public class IsErleCaptainActivity extends BaseRoutableRosActivity {

	/**
	 * A thread to process the heartbeat messages. It processes heartbeat
	 * messages and removes any disconnected drones. Basically it manages a Map
	 * of connected drones.
	 */
	private ManagedCommand heartbeatThread;
	
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
	 * Topic Name : captain/output
	 * Usage : Send command to the mavlink activity to execute some functions.
	 */
	private static String publishers[];

	/**
	 * The topic names for subscribing data 
	 * SUBSCRIBER MAPPING
	 * 
	 * subscribers[0] -> input 
	 * Topic Name : captain/input
	 * Usage : Receive data from mavlink activity and process response.
	 * 
	 * subscribers[1] -> inputWP 
	 * Topic Name : mavlink/heartbeat
	 * Usage : Receive data from mavlink activity about heartbeat message and process it.
	 */
	private static String subscribers[];
	
	/**
	 * A HashMap containing the last heartbeat message of a particular drone id.
	 * It gets removed if that drone doesn't send data for more than 20s.
	 */
	private static Map<Byte,Date> heartbeatLastUpdate;
	
	/**
	 * A Global variale to store the response received after a command from the
	 * mavlink activity.
	 * VALUE TABLE
	 * cmdReturn=0 -> SUCCESS
	 * cmdReturn=-1 -> TIMEOUT
	 * cmdReturn=-2 -> BADCMD
	 * cmdReturn=-3 -> NULL
	 * cmdReturn= +ve value -> FAIL CODE
	 */
	private static int cmdReturn=-1;
	
	/*
	 * Do not change the order of the command options. Everything depends on the
	 * ordering of this enum. So if you need to add command , add it at last and
	 * then update mavlink activity to process this commadn
	 */
	
	/**
	 * This enum contains a list of possible commands to be sent to the mavlink
	 * activity. Use this command list to send commands to mavlink activity.
	 * 
	 * @author Abhay Kumar
	 * 
	 */
	enum CommandOptions
	{
		HEARTBEAT,
		READ_MISSION,
		GET_MISSION,
		WRITE_MISSION,
		SET_CURRENT_ACTIVE_WP,
		CLEAR_MISSION,
		ARM,
		READ_PARAMETER_LIST_START,
		GET_PARAMETER_LIST,
		GET_PARAMETER,
		SET_PARAMETER,
		AUTOPILOT_REBOOT,
		AUTOPILOT_SHUTDOWN,
		BOOTLOADER_REBOOT,
		SYSTEM_SHUTDOWN,
		SYSTEM_REBOOT,
		SET_MODE,
		SET_ALLOWED_AREA,
		SET_GPS_ORIGIN,
		READ_LOG_ENTRY,
		GET_LOG_ENTRY,
		READ_LOG_DATA,
		GET_LOG_DATA,
		UPDATE_TARGET // Update target system and target component
	};
	
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.captain setup");
        publishers = getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME).split(":");
        subscribers = getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME).split(":");
        /*
         * SUBSCRIBER MAPPING
         * Subscriber[0] -> input (As in general input of Captain activity)
         * Subscriber[1] -> heartbeat (Published by mavlink activity)
         */
		heartbeatLastUpdate = new ConcurrentHashMap<Byte, Date>(); // To provide
																	// for
																	// thread
																	// safety
		heartbeatThread = getManagedCommands().scheduleWithFixedDelay(
				new Runnable()
				{

					public void run()
					{
						for (Map.Entry<Byte, Date> entry : heartbeatLastUpdate.entrySet())
						{
							if ((System.currentTimeMillis() - entry.getValue()
									.getTime()) > 2000)
							{
								getLog().warn(
										"Drone with System ID "
												+ entry.getKey()
												+ " did not send a heartbeat packet in last 2s");
							}

							if ((System.currentTimeMillis() - entry.getValue()
									.getTime()) > 20000)
							{
								getLog().warn(
										"Drone with System ID "
												+ entry.getKey()
												+ " did not send a heartbeat packet in last 20s");
								getLog().warn(
										"Disconnecting Drone with System ID "
												+ entry.getKey());
								heartbeatLastUpdate.remove(entry.getKey());
							}
						}
					}
				}, 30, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.captain startup");
    }

    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.captain post startup");
    }

    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.captain activate");
        sendCommand(CommandOptions.WRITE_MISSION);
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.captain deactivate");
        //sendCommand(CommandOptions.ARM);
    }

    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity is.erle.captain pre shutdown");
    }

    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.captain shutdown");
    }

    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.captain cleanup");
    }
    
	private int sendCommand(CommandOptions opt, byte targetSystem,
			byte targetComponent)
	{
		String command = opt.ordinal() + "-" + Byte.toString(targetSystem)
				+ "-" + Byte.toString(targetComponent);
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
		
		return cmdReturnCheck(3000);
	}

	private int sendCommand(CommandOptions opt)
	{
		String command = Integer.toString(opt.ordinal());
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
		
		return cmdReturnCheck(3000);
	}

	private int sendCommand(CommandOptions opt, String[] param)
	{
		String command = Integer.toString(opt.ordinal());
		for (int i = 0; i < param.length; i++)
		{
			command += "-" + param;
		}
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
		
		return cmdReturnCheck(3000);
	}
	
	private int sendCommand(CommandOptions opt, String param)
	{
		String command = Integer.toString(opt.ordinal())+"-" +param;
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
		
		return cmdReturnCheck(3000);
	}
	
	/*
	 * Not recommended for use
	 */
	private int sendCommand(String cmd)
	{
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", cmd);
		sendOutputJson(publishers[0], commandMap);
		
		return cmdReturnCheck(3000);
	}
    
	private int cmdReturnCheck(int timeout)
	{
		Date start=new Date();
		while ( (cmdReturn==-1) && (System.currentTimeMillis()-start.getTime())<timeout);
		
		int temp=cmdReturn;
		synchronized (this)
		{
			cmdReturn =-1;
		}
		return temp;
	}
	
    @Override
    public void onNewInputJson(String channelName, Map <String , Object> message)
    {
		if (channelName.equals(subscribers[0]))
		{
			String [] splitMessage = message.get("command").toString().split("-");
			if (splitMessage[0].equals("SUCCESS"))
			{
				getLog().info("Mavlink activity returned SUCCESS for the given command");
				synchronized (this)
				{
					cmdReturn = 0;
				}
			}
			else if (splitMessage[0].equals("BADCMD"))
			{
				getLog().warn("Mavlink activity does not recognize the given command");
				synchronized (this)
				{
					cmdReturn = -2;
				}
			}
			else if (splitMessage[0].equals("NULL"))
			{
				getLog().warn("Mavlink activity returned NULL for the get command");
				synchronized (this)
				{
					cmdReturn = -3;
				}
			}
			else if (splitMessage[0].equals("FAIL"))
			{
				getLog().warn("Mavlink activity returned FAIL status for the given command");
				try
				{
					synchronized (this)
					{
						cmdReturn = Integer.parseInt(splitMessage[1].trim());
					}
				}
				catch (NumberFormatException e)
				{
					getLog().error("Arbitrary fail type from mavlink activity");
				}
			}
			else
			{
				getLog().warn("Mavlink Activity sent unkown response");
			}
		}
		else if (channelName.equals(subscribers[1]))
		{
			String [] heartbeatmsg = message.get("heartbeat").toString().split(",");
			Byte systemId = Byte.parseByte(heartbeatmsg[0]);
			heartbeatLastUpdate.put(systemId, new Date());
		}
    }
}
