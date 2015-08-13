package is.erle.captain;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.util.concurrency.ManagedCommand;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleCaptainActivity extends BaseRoutableRosActivity {

	private ManagedCommand monitorCaptainThread,heartbeatThread;
	
	private static final String CONFIGURATION_PUBLISHER_NAME = "space.activity.routes.outputs";
	private static final String CONFIGURATION_SUBSCRIBER_NAME = "space.activity.routes.inputs";
	private static String publishers[];
	private static String subscribers[];
	
	private static Map<Byte,Date> heartbeatLastUpdate;
	
	private static int cmdReturn=-1;
	/*
	 * Do not change the order of the command options. Everything depends on the
	 * ordering of this enum. So if you need to add command , add it at last and
	 * then update mavlink activity to process this commadn
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
