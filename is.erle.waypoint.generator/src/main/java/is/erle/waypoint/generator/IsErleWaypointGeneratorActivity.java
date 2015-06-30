package is.erle.waypoint.generator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;


import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;

/**
 * A simple Interactive Spaces Java-based activity.
 */

/*
 * Format
 * QGC WPL <VERSION> 
 * <INDEX> <CURRENT WP> <COORD FRAME><COMMAND> <PARAM1> <PARAM2> <PARAM3> <PARAM4><PARAM5/X/LONGITUDE> <PARAM6/Y/LATITUDE> <PARAM7/Z/ALTITUDE><AUTOCONTINUE> 
 * 
 * Example
 * QGC WPL 110 
 * 0 1 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1 
 * 1 0 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1 
 * 2 0 0 16 0.149999999999999994 0 0 0 8.54800000000000004 47.3759999999999977 550 1
 */

public class IsErleWaypointGeneratorActivity extends BaseRoutableRosActivity
{
	private static final String CONFIGURATION_PUBLISHER_NAME = "space.activity.routes.outputs";
	private static final String CONFIGURATION_SUBSCRIBER_NAME = "space.activity.routes.inputs";
	private static final String CONFIGURATION_FILE_NAME = "~/interactivespaces/controller/mission.txt";
	
	private static final String SEPARATOR ="\t";
	
	private BufferedReader br;
	
	private String currentLine;
	private short waypointCount;
	
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.waypoint.generator setup");
        short lineCount=1;
        try 
        {
			br = new BufferedReader(new FileReader(CONFIGURATION_FILE_NAME));
			while ((currentLine = br.readLine())!=null) 
			{
				if (lineCount != 1) 
				{
					currentLine =currentLine.trim();
					waypointCount = Short.parseShort(currentLine.substring(0,currentLine.indexOf(SEPARATOR) )); // Not sure that tab is the separator 
				}

				lineCount++;
			}
			if (lineCount != waypointCount) 
			{
				getLog().fatal("Waypoint count and number of lines mismatch , recheck mission file ");
			}
			br.close();
		} 
        catch (FileNotFoundException e) 
        {
			getLog().error(e);
		} 
        catch (IOException e) 
		{
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
			Map<String, Object> temp = Maps.newHashMap();
			String temps="START-" + Short.toString(waypointCount);
			temp.put("mission", temps);
			sendOutputJson(getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME), temp);
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
    	String msgFromDrone[] = message.get("mission").toString().split("-") ;
    	if (msgFromDrone[0] == "WAYPOINT_REQUEST") 
    	{
    		
    		try 
    		{
    			br = new BufferedReader(new FileReader(CONFIGURATION_FILE_NAME));
    			
    			currentLine=br.readLine(); //So that it can ignore first line of file, it can be used in checking the integrity of file/version of file 
    			
    			for (int i = 0; i <= Integer.parseInt(msgFromDrone[1]); i++) 
    			{
					currentLine=br.readLine(); // Seek to the current line
					
				}
    			
    			String payLoad[] = currentLine.split(SEPARATOR);
    			
				Map<String, Object> temp = Maps.newHashMap();
				temp.put("mission", payLoad);
				sendOutputJson(getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME), temp);
				br.close();
			} 
    		catch (IOException e) 
    		{
    			getLog().error(e);
			}
		}
    	
    	else if (msgFromDrone[0] == "MISSION_ACCEPTED")
    	{
    		getLog().info("Mission successfully uploaded on the drone");
    	}
    	
    	else
    	{
    		getLog().error("Mission upload failure");
    		getLog().error("REASON : " + msgFromDrone[0]);
    	}
    }
}


