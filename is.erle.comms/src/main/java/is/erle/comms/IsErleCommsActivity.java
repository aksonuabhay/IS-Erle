package is.erle.comms;

import interactivespaces.activity.impl.BaseActivity;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleCommsActivity extends BaseActivity {

    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.comms setup");
    }

    @Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.comms startup");
    }

    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.comms post startup");
    }

    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.comms activate");
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.comms deactivate");
    }

    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity is.erle.comms pre shutdown");
    }

    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.comms shutdown");
    }

    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.comms cleanup");
    }
}
