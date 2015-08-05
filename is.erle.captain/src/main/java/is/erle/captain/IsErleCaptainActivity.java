package is.erle.captain;

import java.util.concurrent.TimeUnit;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.util.concurrency.ManagedCommand;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleCaptainActivity extends BaseRoutableRosActivity {

	private ManagedCommand monitorCaptainThread,heartbeatThread;
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
}
