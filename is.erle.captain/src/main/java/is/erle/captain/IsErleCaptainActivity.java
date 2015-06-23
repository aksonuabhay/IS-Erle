package is.erle.captain;

import interactivespaces.activity.impl.BaseActivity;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleCaptainActivity extends BaseActivity {

    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.captain setup");
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
