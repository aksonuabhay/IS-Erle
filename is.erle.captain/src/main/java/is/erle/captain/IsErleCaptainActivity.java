package is.erle.captain;

import java.util.Map;
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
		GET_LOG_DATA
	};
	
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.captain setup");
        publishers = getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME).split(":");
        subscribers = getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME).split(":");
        
        heartbeatThread = getManagedCommands().scheduleWithFixedDelay(new Runnable() {

			public void run() {
				
			}
		},120,20,TimeUnit.SECONDS);
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
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.captain deactivate");
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
    
	private void sendCommand(CommandOptions opt, byte targetSystem,
			byte targetComponent)
	{
		String command = opt.ordinal() + "-" + Byte.toString(targetSystem)
				+ "-" + Byte.toString(targetComponent);
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
	}

	private void sendCommand(CommandOptions opt)
	{
		String command = Integer.toString(opt.ordinal());
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
	}

	private void sendCommand(CommandOptions opt, String[] param)
	{
		String command = Integer.toString(opt.ordinal());
		for (int i = 0; i < param.length; i++)
		{
			command += "-" + param;
		}
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
	}
	
	private void sendCommand(CommandOptions opt, String param)
	{
		String command = Integer.toString(opt.ordinal())+"-" +param;
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", command);
		sendOutputJson(publishers[0], commandMap);
	}
	
	/*
	 * Not recommended for use
	 */
	private void sendCommand(String cmd)
	{
		Map<String, Object> commandMap = Maps.newHashMap();
		commandMap.put("command", cmd);
		sendOutputJson(publishers[0], commandMap);
	}
    
    
}
