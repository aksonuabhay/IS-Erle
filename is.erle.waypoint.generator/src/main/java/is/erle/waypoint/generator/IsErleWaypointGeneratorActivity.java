package is.erle.waypoint.generator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleWaypointGeneratorActivity extends BaseRoutableRosActivity
{
	private static final String CONFIGURATION_PUBLISHER_NAME = "space.activity.routes.outputs";
	private static final String CONFIGURATION_SUBSCRIBER_NAME = "space.activity.routes.inputs";
	private static final String CONFIGURATION_FILE_NAME = "~/interactivespaces/controller/mission.txt";
	
	private BufferedReader br;
	
	private int waypointCount;
	
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.waypoint.generator setup");
        int lineCount=1;
        try 
        {
			br = new BufferedReader(new FileReader(CONFIGURATION_FILE_NAME));
			String currentLine;
			while ((currentLine = br.readLine())!=null) 
			{
				if (lineCount != 1) 
				{
					currentLine =currentLine.trim();
					waypointCount = Integer.parseInt(currentLine.substring(0,currentLine.indexOf(" ") )); // Not sure that space is the separator of tab is the separator
				}

				lineCount++;
			}
			if (lineCount != waypointCount) 
			{
				getLog().fatal("Waypoint count and number of lines mismatch , recheck mission file ");
			}
			
		} 
        catch (FileNotFoundException e) 
        {
			// TODO Auto-generated catch block
			getLog().error(e);
		} 
        catch (IOException e) 
		{
			// TODO Auto-generated catch block
			getLog().error(e);
		}
        
    }

    @Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.waypoint.generator startup");
    }

    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.waypoint.generator post startup");
    }

    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.waypoint.generator activate");
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.waypoint.generator deactivate");
    }

    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity is.erle.waypoint.generator pre shutdown");
    }

    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.waypoint.generator shutdown");
    }

    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.waypoint.generator cleanup");
    }
    
    @Override
    public void onNewInputJson(String channelName, Map <String , Object> message)
    {
    	//To Do
    }
}
