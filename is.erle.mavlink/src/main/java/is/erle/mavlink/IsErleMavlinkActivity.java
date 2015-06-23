package is.erle.mavlink;

import interactivespaces.activity.impl.BaseActivity;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleMavlinkActivity extends BaseActivity {

    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.mavlink setup");
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
