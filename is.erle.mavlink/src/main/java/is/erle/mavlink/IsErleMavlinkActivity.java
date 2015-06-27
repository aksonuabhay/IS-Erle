package is.erle.mavlink;

import java.util.Map;

import interactivespaces.activity.impl.BaseActivity;
import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;

import com.MAVLink.*;
import com.google.common.collect.Maps;
/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleMavlinkActivity extends BaseRoutableRosActivity {

	private static final String CONFIGURATION_PUBLISHER_NAME = "space.activity.routes.outputs";
	private static final String CONFIGURATION_SUBSCRIBER_NAME = "space.activity.routes.inputs";
	
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.mavlink setup");
        getLog().info(getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME));
        getLog().info(getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME));
    }

    @Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.mavlink startup");
    }

    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.mavlink post startup");
    }

    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.mavlink activate");
		Map<String, Object> temp = Maps.newHashMap();
		temp.put("mission", "START");
		sendOutputJson(getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME), temp);
		sendOutputJson("outputCOM_M", temp);
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.mavlink deactivate");
    }

    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity is.erle.mavlink pre shutdown");
    }

    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.mavlink shutdown");
    }

    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.mavlink cleanup");
    }
}
