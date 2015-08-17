package is.erle.captain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.python.antlr.PythonParser.continue_stmt_return;

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
	 * @see    is.erle.mavlink.IsErleMavlinkActivity
	 * @since  1.0.0
	 */
	enum CommandOptions
	{
		/**
		 * Request heartbeat message from mavlink activity.
		 * Can not have any function arguments.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#heartbeat
		 */
		HEARTBEAT,
		
		/**
		 * Read Mission file from the drone. Can have 2 arguments as target
		 * system and target component. If no arguments are given, then the
		 * command will be sent to the default drone of the mavlink activity.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#readMissionListStart(byte,
		 *      byte)
		 */
		READ_MISSION,
		
		/**
		 * Get Mission data stored in mavlink activity after receiving from drone.
		 * Can not have any other arguments.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#readWaypointList
		 */
		GET_MISSION,
		
		/**
		 * Write Mission file on the drone. Can have 2 arguments as target
		 * system and target component. If no arguments are given, then the
		 * command will be sent to the default drone of the mavlink activity.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#sendMissionListStart(byte,
		 *      byte)
		 */
		WRITE_MISSION,
		
		/**
		 * Set current active waypoint on the drone from the mission file. Can
		 * have 3 arguments as current sequence, target system and target
		 * component in order. If 1 argument is given, then the command will be
		 * sent to the default drone of the mavlink activity with the current
		 * sequence as argument.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#setCurrentActiveWP(short,byte,
		 *      byte)
		 */
		SET_CURRENT_ACTIVE_WP,
		
		/**
		 * Clears Mission file on the drone. Can have 2 arguments as target
		 * system and target component. If no arguments are given, then the
		 * command will be sent to the default drone of the mavlink activity.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#clearMissionList(byte,
		 *      byte)
		 */
		CLEAR_MISSION,
		
		/**
		 * Arms/Disarms the drone. Can have 0,1,2,3 arguments as switch on/off,
		 * target system and target component in order. If no arguments are
		 * given, then the command will be sent arm the default drone of the
		 * mavlink activity. To disarm send 1 argument as arm on/off value and
		 * the command will be performed on the default drone. If 2 arguments
		 * are there, the drone with target system and target component will be
		 * armed. If 3 arguments are there, the drone with target system and
		 * target component will be armed/disarmed.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#doARM(boolean, byte, byte)
		 */
		ARM,
		
		/**
		 * Read all the parameter data from the drone. Can have 2 arguments as
		 * target system and target component in order. If no arguments are
		 * given, then the command will be sent to the default drone of the
		 * mavlink activity.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#readParameterListStart(byte,
		 *      byte)
		 */
		READ_PARAMETER_LIST_START,
		
		/**
		 * Get parameter data map stored in mavlink activity after receiving
		 * from drone. Can not have any other arguments.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#paramList
		 */
		GET_PARAMETER_LIST,
		
		/**
		 * Get parameter data for a single parameter stored in mavlink activity
		 * after receiving from drone. It will have one more argument in the
		 * form of parameter string id.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#paramList
		 */
		GET_PARAMETER,
		
		/**
		 * Sets the supplied parameter value on the drone. Can have 2 or 4
		 * arguments as parameter Id, parameter value, target system and target
		 * component in order. If 2 arguments are given, then the command will
		 * be sent to set the parameter value with the given parameter Id to the
		 * default drone of the mavlink activity. If 4 arguments are there, the
		 * parameter will be written on the drone with target system and target
		 * component.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#setParam(String,float,byte,
		 *      byte)
		 */
		SET_PARAMETER,
		
		/**
		 * Reboot the Autopilot system running on the drone. Can have 2
		 * arguments as target system and target component in order. If no
		 * arguments are given, then the command will be sent to the default
		 * drone of the mavlink activity.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#doRebootAutopilot(byte,
		 *      byte)
		 */
		AUTOPILOT_REBOOT,
		
		/**
		 * Shutdown the Autopilot system running on the drone. Can have 2
		 * arguments as target system and target component in order. If no
		 * arguments are given, then the command will be sent to the default
		 * drone of the mavlink activity.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#doShutdownAutopilot(byte,
		 *      byte)
		 */
		AUTOPILOT_SHUTDOWN,
		
		/**
		 * Do Bootloader reboot of the drone. Can have 2 arguments as target
		 * system and target component in order. If no arguments are given, then
		 * the command will be sent to the default drone of the mavlink
		 * activity.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#doBootloaderReboot(byte,
		 *      byte)
		 */
		BOOTLOADER_REBOOT,
		
		/**
		 * Shutdown the system of the drone. Can have 2 arguments as target
		 * system and target component in order. If no arguments are given, then
		 * the command will be sent to the default drone of the mavlink
		 * activity.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#doSystemShutdown(byte,
		 *      byte)
		 */
		SYSTEM_SHUTDOWN,
		
		/**
		 * Reboot the system of the drone. Can have 2 arguments as target system
		 * and target component in order. If no arguments are given, then the
		 * command will be sent to the default drone of the mavlink activity.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#doSystemReboot(byte, byte)
		 */
		SYSTEM_REBOOT,
		
		/**
		 * Set the mode of the drone as auto, guided, stabilize etc. Can have 1
		 * or 2 arguments as mode and target system in order. If 1 argument is
		 * given, then the command will be sent to the default drone of the
		 * mavlink activity to switch to the given mode. Otherwise it will be
		 * sent to the target system specified.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#setMode(String, byte)
		 */
		SET_MODE,
		
		/**
		 * Set the allowed area of the drone. Can have 7 or 9 arguments as
		 * Point3D 1, Point3D 2,coordinate frame, target system and target
		 * component in order. If 7 arguments are given, then the command will
		 * be sent to the default drone of the mavlink activity to set the
		 * allowed area with the given coordinate frame. Otherwise it will be
		 * sent to the target system and target component specified.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#setAllowedArea(Point3D,
		 *      Point3D, byte, byte, byte)
		 * @see is.erle.mavlink.Point3D
		 */
		SET_ALLOWED_AREA,
		
		/**
		 * Set the GPS Origin of the drone. Can have 3 or 5 arguments as Point3D
		 * gps origin , target system and target component in order. If 3
		 * arguments are given, then the command will be sent to the default
		 * drone of the mavlink activity to switch to set the GPS Origin with
		 * the given coordinate frame. Otherwise it will be sent to the target
		 * system and target component specified.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#setAllowedArea(Point3D,
		 *      byte, byte)
		 * @see is.erle.mavlink.Point3D
		 */
		SET_GPS_ORIGIN,
		
		/**
		 * Read all the Log Entries from the drone. Can have 2 arguments as
		 * target system and target component in order. If no arguments are
		 * given, then the command will be sent to the default drone of the
		 * mavlink activity.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#getLogList(byte, byte)
		 */
		READ_LOG_ENTRY,
		
		/**
		 * Get all the Log Entries stored in mavlink activity after receiving
		 * from drone. Can not have any other arguments.
		 * 
		 * @see is.erle.mavlink.IsErleMavlinkActivity#logEntry
		 */
		GET_LOG_ENTRY,
		
		/**
		 * 
		 */
		READ_LOG_DATA,
		
		/**
		 * 
		 */
		GET_LOG_DATA,
		
		/**
		 * Update the default target system and target component. Can have 1 or
		 * 2 arguments as target system and target component in order. If 1
		 * argument is given, then the default target system of the mavlink
		 * activity will be set. Otherwise both target system and target
		 * component will be set.
		 */
		UPDATE_TARGET // Update target system and target component
	};
	
	/**  
	 * A List of String arrays containing waypoint data read from the drone
	 * DATA MAPPING FOR STRING ARRAY
	 * Index 0 -> INDEX
	 * Index 1 -> CURRENT WP
	 * Index 2 -> COORD FRAME
	 * Index 3 -> COMMAND
	 * Index 4 -> PARAM1
	 * Index 5 -> PARAM2
	 * Index 6 -> PARAM3
	 * Index 7 -> PARAM4
	 * Index 8 -> PARAM5/X/LONGITUDE
	 * Index 9 -> PARAM6/Y/LATITUDE
	 * Index 10 -> PARAM7/Z/ALTITUDE
	 * Index 11 -> AUTOCONTINUE
	 */
	private List<String []> readWaypointList;
	
	/**
	 * A HashMap to store the value of the parameter with a certain string id. 
	 */
	private Map<String , Double> paramList;
	
	/**
	 * Stores the parameter value last requested.
	 */
	private double param;
	
	/**
	 * Stores the log Entry found on the remote drone.
	 */
	private List<String> logEntry;
    /**
     * Executes on activity setup.
     * @see		interactivespaces.activity.impl.BaseActivity#onActivitySetup()
     * @since	1.0.0
     */
	
	/**
	 * RC Transmitter output values to be sent regularly.
	 */
	private static short []rc_out = new short[8];
	
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
							getLog().warn("Keep the throttle stick down always. Whenever starting a mission just raise it a bit to start the mission.");
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
    
    /**
     * Executes on activity startup.
     * @see		interactivespaces.activity.impl.BaseActivity#onActivityStartup()
     * @since	1.0.0
     */
    @Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.captain startup");
    }

    /**
     * Executes on activity post startup.
     * @see		interactivespaces.activity.impl.BaseActivity#onActivityPostStartup()
     * @since	1.0.0
     */
    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.captain post startup");
    }

    /**
     * Executes on activity activate.
     * @see		interactivespaces.activity.impl.BaseActivity#onActivityActivate()
     * @since	1.0.0
     */
    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.captain activate");
        //sendCommand(CommandOptions.WRITE_MISSION);
        int rslt = sendCommand(CommandOptions.READ_PARAMETER_LIST_START,20000);
        //getLog().info("Param list start get" +rslt);
        if (rslt ==0)
		{
			rslt = sendCommand(CommandOptions.GET_PARAMETER_LIST);
			//getLog().info("Param list get" +rslt);
			if (rslt == 0)
			{
				getLog().info(paramList.toString());
			}
		}
		rslt = sendCommand(CommandOptions.GET_PARAMETER,"RC3_MAX");
		if (rslt == 0)
		{
			getLog().info(param);
		}
        rslt = sendCommand(CommandOptions.READ_MISSION);
        if (rslt ==0)
		{
			rslt = sendCommand(CommandOptions.GET_MISSION);
			if (rslt == 0)
			{
				getLog().info(Arrays.deepToString(readWaypointList.toArray()));
			}
		}
        rslt = sendCommand(CommandOptions.READ_LOG_ENTRY);
        if (rslt ==0)
		{
			rslt = sendCommand(CommandOptions.GET_LOG_ENTRY);
			if (rslt == 0)
			{
				getLog().info(Arrays.toString(logEntry.toArray()));
			}
		}
		rc_out[0] = (short) (paramList.get("RC1_MIN").shortValue() / 2 + paramList
				.get("RC1_MAX").shortValue() / 2);
		rc_out[1] = (short) (paramList.get("RC2_MIN").shortValue() / 2 + paramList
				.get("RC2_MAX").shortValue() / 2);
		rc_out[2] = paramList.get("RC3_MIN").shortValue();
		rc_out[3] = (short) (paramList.get("RC4_MIN").shortValue() / 2 + paramList
				.get("RC4_MAX").shortValue() / 2);
		rc_out[4] = (short) 0xFFFF;
		rc_out[5] = (short) 0xFFFF;
		rc_out[6] = (short) 0xFFFF;
		rc_out[7] = (short) 0xFFFF;
        
        
        ManagedCommand rcOutput = getManagedCommands().scheduleAtFixedRate(new Runnable()
		{
			
			public void run()
			{
				Map<String, Object> mapRCOut = Maps.newHashMap();
				mapRCOut.put("rc", Arrays.toString(rc_out));
				sendOutputJson(publishers[1], mapRCOut);
			}
		}, 1, 20, TimeUnit.SECONDS);
    }

    /**
     * Executes on activity deactivate.
     * @see		interactivespaces.activity.impl.BaseActivity#onActivityDeactivate()
     * @since	1.0.0
     */
    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.captain deactivate");
        //sendCommand(CommandOptions.ARM);
    }

    /**
     * Executes on activity pre shutdown.
     * @see		interactivespaces.activity.impl.BaseActivity#onActivityPreShutdown()
     * @since	1.0.0
     */
    @Override
    public void onActivityPreShutdown() {
		getLog().info("Activity is.erle.captain pre shutdown");
		int shut = sendCommand(CommandOptions.SET_MODE, "RTL");
		if (shut == 0)
		{
			getLog().info(
					"Captain activity shut down, Drone returning to launch");
		}
		else
		{
			getLog().error(
					"Could not send a Return to Launch command to the drone, be careful!! ");
		}
    }

    /**
     * Executes on activity shutdown.
     * @see		interactivespaces.activity.impl.BaseActivity#onActivityShutdown()
     * @since	1.0.0
     */
    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.captain shutdown");
    }

    /**
     * Executes on activity cleanup.
     * @see		interactivespaces.activity.impl.BaseActivity#onActivityCleanup()
     * @since	1.0.0
     */
    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.captain cleanup");
    }
    
	/**
	 * Sends a command to the mavlink activity to perform some action. 
	 * Used to execute a function with the arguments target system and target component in the
	 * mavlink activity. The command will be sent to the target system and default target 
	 * component value supplied here.
	 * 
	 * @param opt			   Command from the command option list.
	 * @param timeout		 Timeout Period of the command.
	 * @param targetSystem	   Target drone to send command to.
	 * @param targetComponent  Target component on the drone.
	 * @return				   Response from the mavlink activity.
	 * 						   value = 0 	-> 	SUCCESS,
	 * 						   value =-1 	-> 	TIMEOUT,
	 * 						   value=-2 	-> 	BADCMD,
	 * 						   value=-3 	-> 	NULL,
	 *						   value= +ve  ->   FAIL CODE
	 * @see					   #cmdReturnCheck(int)
	 * @see					   CommandOptions
	 * @since				   1.0.0
	 */
	private int sendCommand(CommandOptions opt, byte targetSystem,
			byte targetComponent, int timeout)
	{
		String command = opt.ordinal() + "-" + Byte.toString(targetSystem)
				+ "-" + Byte.toString(targetComponent);
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
		
		return cmdReturnCheck(timeout);
	}
	
	/**
	 * Sends a command to the mavlink activity to perform some action. 
	 * Used to execute a function with the arguments target system and target component in the
	 * mavlink activity. The command will be sent to the target system and default target 
	 * component value supplied here.
	 * 
	 * @param opt			   Command from the command option list.
	 * @param targetSystem	   Target drone to send command to.
	 * @param targetComponent  Target component on the drone.
	 * @return				   Response from the mavlink activity.
	 * 						   value = 0 	-> 	SUCCESS,
	 * 						   value =-1 	-> 	TIMEOUT,
	 * 						   value=-2 	-> 	BADCMD,
	 * 						   value=-3 	-> 	NULL,
	 *						   value= +ve  ->   FAIL CODE
	 * @see					   #cmdReturnCheck(int)
	 * @see					   CommandOptions
	 * @since				   1.0.0
	 */
	private int sendCommand(CommandOptions opt, byte targetSystem,
			byte targetComponent)
	{
		return sendCommand(opt, targetSystem, targetComponent, 3000);
	}

	/**
	 * Sends a command to the mavlink activity to perform some action. 
	 * This function has no command arguments. Used to execute a function without arguments in the
	 * mavlink activity. The command will be sent to the default target system and default target 
	 * component set in the mavlink activity.
	 * 
	 * @param opt			 Command from the command option list.
	 * @return				 Response from the mavlink activity.
	 * 						 value = 0 	-> 	SUCCESS,
	 * 						 value =-1 	-> 	TIMEOUT,
	 * 						 value=-2 	-> 	BADCMD,
	 * 						 value=-3 	-> 	NULL,
	 *						 value= +ve  ->  FAIL CODE
	 * @see					 #cmdReturnCheck(int)
	 * @see					 CommandOptions
	 * @since				 1.0.0
	 */
	private int sendCommand(CommandOptions opt)
	{
		return sendCommand(opt, 3000);
	}
	
	/**
	 * Sends a command to the mavlink activity to perform some action. 
	 * This function has no command arguments. Used to execute a function without arguments in the
	 * mavlink activity. The command will be sent to the default target system and default target 
	 * component set in the mavlink activity.
	 * 
	 * @param opt			 Command from the command option list.
	 * @param timeout		 Timeout Period of the command.
	 * @return				 Response from the mavlink activity.
	 * 						 value = 0 	-> 	SUCCESS,
	 * 						 value =-1 	-> 	TIMEOUT,
	 * 						 value=-2 	-> 	BADCMD,
	 * 						 value=-3 	-> 	NULL,
	 *						 value= +ve  ->  FAIL CODE
	 * @see					 #cmdReturnCheck(int)
	 * @see					 CommandOptions
	 * @since				 1.0.0
	 */
	private int sendCommand(CommandOptions opt , int timeout)
	{
		String command = Integer.toString(opt.ordinal());
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
		
		return cmdReturnCheck(timeout);
	}

	/**
	 * Sends a command to the mavlink activity to perform some action. The
	 * command argument comes in a String array format. 
	 * 
	 * @param opt			 Command from the command option list.
	 * @param param			 Contains the command arguments as a string array.
	 *            			 This follows the parameters of the command as explained
	 *            			 in the CommandOptions enum.
	 * @param timeout		 Timeout Period of the command.
	 * @return				 Response from the mavlink activity.
	 * 						 value = 0 	-> 	SUCCESS,
	 * 						 value =-1 	-> 	TIMEOUT,
	 * 						 value=-2 	-> 	BADCMD,
	 * 						 value=-3 	-> 	NULL,
	 *						 value= +ve  ->  FAIL CODE
	 * @see					 #cmdReturnCheck(int)
	 * @see					 CommandOptions
	 * @since				 1.0.0
	 */
	private int sendCommand(CommandOptions opt, String[] param, int timeout)
	{
		String command = Integer.toString(opt.ordinal());
		for (int i = 0; i < param.length; i++)
		{
			command += "-" + param;
		}
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
		
		return cmdReturnCheck(timeout);
	}
	
	/**
	 * Sends a command to the mavlink activity to perform some action. The
	 * command argument comes in a String array format. 
	 * 
	 * @param opt			 Command from the command option list.
	 * @param param			 Contains the command arguments as a string array.
	 *            			 This follows the parameters of the command as explained
	 *            			 in the CommandOptions enum.
	 * @return				 Response from the mavlink activity.
	 * 						 value = 0 	-> 	SUCCESS,
	 * 						 value =-1 	-> 	TIMEOUT,
	 * 						 value=-2 	-> 	BADCMD,
	 * 						 value=-3 	-> 	NULL,
	 *						 value= +ve  ->  FAIL CODE
	 * @see					 #cmdReturnCheck(int)
	 * @see					 CommandOptions
	 * @since				 1.0.0
	 */
	private int sendCommand(CommandOptions opt, String[] param)
	{
		return sendCommand(opt, param, 3000);
	}
	
	/**
	 * Sends a command to the mavlink activity to perform some action. The
	 * command argument comes in a String separated by '-' separator. 
	 * 
	 * @param opt			 Command from the command option list.
	 * @param param			 Contains the command arguments as a string separated by '-' separator.
	 *            			 This follows the parameters of the command as explained
	 *            			 in the CommandOptions enum.
	 * @param timeout		 Timeout Period of the command.
	 * @return				 Response from the mavlink activity.
	 * 						 value = 0 	-> 	SUCCESS,
	 * 						 value =-1 	-> 	TIMEOUT,
	 * 						 value=-2 	-> 	BADCMD,
	 * 						 value=-3 	-> 	NULL,
	 *						 value= +ve  ->  FAIL CODE
	 * @see					 #cmdReturnCheck(int)
	 * @see					 CommandOptions
	 * @since				 1.0.0
	 */
	private int sendCommand(CommandOptions opt, String param, int timeout)
	{
		String command = Integer.toString(opt.ordinal())+"-" +param;
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
		
		return cmdReturnCheck(timeout);
	}
	
	/**
	 * Sends a command to the mavlink activity to perform some action. The
	 * command argument comes in a String separated by '-' separator. 
	 * 
	 * @param opt			 Command from the command option list.
	 * @param param			 Contains the command arguments as a string separated by '-' separator.
	 *            			 This follows the parameters of the command as explained
	 *            			 in the CommandOptions enum.
	 * @return				 Response from the mavlink activity.
	 * 						 value = 0 	-> 	SUCCESS,
	 * 						 value =-1 	-> 	TIMEOUT,
	 * 						 value=-2 	-> 	BADCMD,
	 * 						 value=-3 	-> 	NULL,
	 *						 value= +ve  ->  FAIL CODE
	 * @see					 #cmdReturnCheck(int)
	 * @see					 CommandOptions
	 * @since				 1.0.0
	 */
	private int sendCommand(CommandOptions opt, String param)
	{
		return sendCommand(opt, param, 3000);
	}
	
	/*
	 * Not recommended for use
	 */
	/**
	 * Sends a command to the mavlink activity to perform some action. The
	 * command comes in a String whose first element always contains
	 * CommandOptions ordinal value. After this follows the other arguments
	 * required for the function.
	 * 
	 * @param cmd			 Contains the command as a string separated by '-' separator.
	 *           			 The first value is always a CommandOption's ordinal value.
	 *            			 After this follows the parameters of the command as explained
	 *            			 in the CommandOptions enum. They are separated by '-'.
	 * @param timeout		 Timeout Period of the command.
	 * @return				 Response from the mavlink activity.
	 * 						 value = 0 	-> 	SUCCESS,
	 * 						 value =-1 	-> 	TIMEOUT,
	 * 						 value=-2 	-> 	BADCMD,
	 * 						 value=-3 	-> 	NULL,
	 *						 value= +ve  ->  FAIL CODE
	 * @see					 #cmdReturnCheck(int)
	 * @see					 CommandOptions
	 * @since				 1.0.0
	 */
	private int sendCommand(String cmd, int timeout)
	{
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", cmd);
		sendOutputJson(publishers[0], commandMap);
		
		return cmdReturnCheck(timeout);
	}
    
	/**
	 * Sends a command to the mavlink activity to perform some action. The
	 * command comes in a String whose first element always contains
	 * CommandOptions ordinal value. After this follows the other arguments
	 * required for the function.
	 * 
	 * @param cmd			 Contains the command as a string separated by '-' separator.
	 *           			 The first value is always a CommandOption's ordinal value.
	 *            			 After this follows the parameters of the command as explained
	 *            			 in the CommandOptions enum. They are separated by '-'.
	 * @return				 Response from the mavlink activity.
	 * 						 value = 0 	-> 	SUCCESS,
	 * 						 value =-1 	-> 	TIMEOUT,
	 * 						 value=-2 	-> 	BADCMD,
	 * 						 value=-3 	-> 	NULL,
	 *						 value= +ve  ->  FAIL CODE
	 * @see					 #cmdReturnCheck(int)
	 * @see					 CommandOptions
	 * @since				 1.0.0
	 */
	private int sendCommand(String cmd)
	{
		return sendCommand(cmd, 3000);
	}
	
	/**
	 * Waits for the response of a command until a timeout period. After the
	 * timeout or response from the mavlink activity, it returns the command
	 * status. This function makes a synchronized update to the global cmdReturn
	 * variable to make it to have the default value.
	 * 
	 * @param timeout		Time to wait for the response from mavlink activity.
	 * @return				value = 0 	-> 	SUCCESS,
	 * 						value =-1 	-> 	TIMEOUT,
	 * 						value=-2 	-> 	BADCMD,
	 * 						value=-3 	-> 	NULL,
	 *						value= +ve  ->  FAIL CODE
	 * @since				1.0.0
	 */
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
	
	/**
	 * Callback for new message on the subscribed topics.
	 * Processes incoming messages.
	 * 
	 * @param channelName 	Channel name of incoming message
	 * @param message 		Message stored in a key-value pair in a map
	 * @see 				interactivespaces.activity.impl.ros.BaseRoutableRosActivity
	 * @see					java.util.Map
	 * @since				1.0.0
	 */
    @Override
    public void onNewInputJson(String channelName, Map <String , Object> message)
    {
		if (channelName.equals(subscribers[0]))
		{
			if (message.containsKey("mission"))
			{
				readWaypointList = new ArrayList<String[]>();
				//getLog().info(message.get("mission").toString());
				String [] result = message.get("mission").toString().split("\\],");
				String[] wpList;
				for (int i = 0; i < result.length; i++)
				{
					wpList = result[i].replaceAll("\\[", "")
							.replaceAll("\\]", "").replaceAll(" ", "")
							.split(",");
					readWaypointList.add(wpList);
				} 
			}
			if (message.containsKey("param_list"))
			{
				//@SuppressWarnings("unchecked")
				Map<String,Double> map = (Map<String,Double>) message.get("param_list");
				//getLog().info(message.get("param_list").toString());
				paramList = map;
			}
			if (message.containsKey("param"))
			{
				String doubleMessage = message.get("param").toString();
				param = Double.parseDouble(doubleMessage);
			}
			if (message.containsKey("log_entry"))
			{
				logEntry = new ArrayList<String>();
				//getLog().info(message.get("log_entry").toString());
				String [] result = message.get("log_entry").toString().replaceAll("\\[", "")
						.replaceAll("\\]", "").replaceAll(" ", "")
						.split(",");
				for (int i = 0; i < result.length; i++)
				{
					logEntry.add(result[i]);
				}
			}
			if (message.containsKey("command"))
			{
				String[] splitMessage = message.get("command").toString()
						.split("-");
				if (splitMessage[0].equals("SUCCESS"))
				{
					getLog().info(
							"Mavlink activity returned SUCCESS for the given command");
					synchronized (this)
					{
						cmdReturn = 0;
					}
				}
				else if (splitMessage[0].equals("BADCMD"))
				{
					getLog().warn(
							"Mavlink activity does not recognize the given command");
					synchronized (this)
					{
						cmdReturn = -2;
					}
				}
				else if (splitMessage[0].equals("NULL"))
				{
					getLog().warn(
							"Mavlink activity returned NULL for the get command");
					synchronized (this)
					{
						cmdReturn = -3;
					}
				}
				else if (splitMessage[0].equals("FAIL"))
				{
					getLog().warn(
							"Mavlink activity returned FAIL status for the given command");
					if (splitMessage.length == 2)
					{
						try
						{
							synchronized (this)
							{
								cmdReturn = Integer.parseInt(splitMessage[1]
										.trim());
							}
						}
						catch (NumberFormatException e)
						{
							getLog().error(
									"Arbitrary fail type from mavlink activity");
						}
					}
					else if (splitMessage.length ==1)
					{
						synchronized (this)
						{
							cmdReturn = 1;
						}
					}
				}
				else
				{
					getLog().warn("Mavlink Activity sent unkown response");
				}
			}
		}
		else if (channelName.equals(subscribers[1]))
		{
			String [] heartbeatmsg = message.get("heartbeat").toString().split(",");
			Byte systemId = Byte.parseByte(heartbeatmsg[0]);
			heartbeatLastUpdate.put(systemId, new Date());
		}
		else if (channelName.equals(subscribers[2]))
		{
			if (message.get("fly").toString().equals("FLY"))
			{
				startFlying();
			}
		}
    }

	/**
	 * Send sequence of commands to mavlink activity to make the drone fly
	 * autonomously according to the mission file.
	 */
	private void startFlying()
	{
		int ack = sendCommand(CommandOptions.SET_MODE, "Stabilize");
		if (ack == 0)
		{
			getLog().info("Mode successfully set to stabilize");
			ack = sendCommand(CommandOptions.WRITE_MISSION);
			if (ack == 0)
			{
				getLog().info("Mission write successful");
				ack = sendCommand(CommandOptions.ARM);
				if (ack == 0)
				{
					getLog().info("Arming of the drone successful");
					ack = sendCommand(CommandOptions.SET_MODE, "Auto");
					if (ack == 0)
					{
						Date start = new Date();
						int i = 1;
						while ((System.currentTimeMillis() - start.getTime()) < 3050)
						{
							if ((System.currentTimeMillis() - start.getTime()) >(1000*i))
							{
								getLog().warn(
										"GOING TO FLY IN " + (4 - i) + " sec");
								i++;
							}
						}
						rc_out[2] = (short) (paramList.get("RC3_MIN").shortValue()+150);
						getLog().info("Drone set to auto mode");
						getLog().info(
								"All sequence successfully sent to the drone");
						getLog().warn(
								"STAY AWAY FROM THE DRONE, IT SHOULD START FLYING ANYTIME NOW");
					}
					else
					{
						getLog().error("Set Mode Failed");
					}
				}
				else
				{
					getLog().error("Arming the drone failed");
				}
			}
			else
			{
				getLog().error("Mision Write Failed");
			}
		}
		else
		{
			getLog().error("Set Stabilize mode failed");
		}
	}
}
