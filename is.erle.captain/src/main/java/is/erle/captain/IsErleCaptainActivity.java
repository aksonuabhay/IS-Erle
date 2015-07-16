package is.erle.captain;

import java.util.concurrent.TimeUnit;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.util.concurrency.ManagedCommand;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleCaptainActivity extends BaseRoutableRosActivity {

	private ManagedCommand monitorCaptainThread,heartbeatThread;
	
	
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
