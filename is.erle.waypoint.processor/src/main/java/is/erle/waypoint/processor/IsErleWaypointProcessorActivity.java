package is.erle.waypoint.processor;

import interactivespaces.activity.impl.BaseActivity;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleWaypointProcessorActivity extends BaseActivity {

    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.waypoint.processor setup");
    }

    @Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.waypoint.processor startup");
    }

    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.waypoint.processor post startup");
    }

    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.waypoint.processor activate");
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.waypoint.processor deactivate");
    }

    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity is.erle.waypoint.processor pre shutdown");
    }

    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.waypoint.processor shutdown");
    }

    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.waypoint.processor cleanup");
    }
}
