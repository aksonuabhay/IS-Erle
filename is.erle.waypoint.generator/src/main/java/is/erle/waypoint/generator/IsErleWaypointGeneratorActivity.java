package is.erle.waypoint.generator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
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
	private static final String FILE_NAME = "mission.txt";
	private static String fileWithDirectory;
	private static final String SEPARATOR ="\t";
	
	private BufferedReader br;
	
	private String currentLine;
	private short waypointCount;
	
	private String publisherName;
	private String subscriberName;
	private boolean wpSendFlag[];
	
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.waypoint.generator setup");
        
        publisherName = getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME);
        subscriberName = getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME);
        fileWithDirectory = getActivityFilesystem().getInstallDirectory().getAbsolutePath()+"/"+FILE_NAME;
        short lineCount=1;
        try 
        {
			br = new BufferedReader(new FileReader(fileWithDirectory));
			while ((currentLine = br.readLine())!=null) 
			{
				if (lineCount != 1) 
				{
					currentLine =currentLine.trim();
					if (!checkColumnLength(currentLine))
					{
						getLog().error("Aborting file read");
						return;
					}
					waypointCount = Short.parseShort(currentLine.substring(0,currentLine.indexOf(SEPARATOR) )); // Not sure that tab is the separator 
				}

				lineCount++;
			}
			if (lineCount != (waypointCount+2)) 
			{
				getLog().info("Waypoint count and number of lines mismatch , recheck mission file ");
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
        waypointCount+=1; //So as to accomodate array index 0
        wpSendFlag = new boolean[waypointCount];
        for (int i = 0; i < wpSendFlag.length; i++) {
			wpSendFlag[i] = false;
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
			sendOutputJson(publisherName, temp);
			getLog().info(temps);
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
		if (channelName.equals(subscriberName)) {

			// To Do
			String msgFromDrone[] = message.get("mission").toString()
					.split("-");
			if (msgFromDrone[0].equals("MISSION_REQUEST")) {
				if (!wpSendFlag[Integer.parseInt(msgFromDrone[1])]) {

					try {
						br = new BufferedReader(new FileReader(
								fileWithDirectory));

						currentLine = br.readLine(); // So that it can ignore
														// first
														// line of file, it can
														// be
														// used in checking the
														// integrity of
														// file/version
														// of file
						//getLog().info("REQUEST NUMBER : " + msgFromDrone[1]);
						for (int i = 0; i <= Integer.parseInt(msgFromDrone[1]); i++) {
							currentLine = br.readLine(); // Seek to the current
															// line

						}

						String payLoad[] = currentLine.split(SEPARATOR);
						//getLog().info("SENDING REQUEST NUMBER : " + payLoad[0]);
						Map<String, Object> temp = Maps.newHashMap();
						temp.put("mission", Arrays.toString(payLoad));
						sendOutputJson(publisherName, temp);
						br.close();
					} catch (IOException e) {
						getLog().error(e);
					}
					wpSendFlag[Integer.parseInt(msgFromDrone[1])] = true;
				}
				
			}
			else if (msgFromDrone[0].equals("MISSION_ACCEPTED")) {
				getLog().info("Mission successfully uploaded on the drone");
			}

			else {
				getLog().error("Mission upload failure");
				getLog().error("REASON : " + msgFromDrone[0]);
			}

		}
	}
    
    private boolean checkColumnLength(String column)
    {
		int separatorCount = 0;
		for (int i = 0; i < column.length(); i++)
		{
			if (column.charAt(i) == SEPARATOR.charAt(0))
			{
				separatorCount++;
			}
		}
		int contentCount = column.split(SEPARATOR).length;
		if (!((separatorCount == 11) | (contentCount == 12)))
		{
			getLog().error(
					"Separator count is not 11 and content count is not 12");
			return false;
		}
		else
		{
			if (separatorCount!=11)
			{
				getLog().warn("Separator count is not equal to 11");
			}
			else if (contentCount !=12) 
			{
				getLog().warn("Content count is not equal to 12");
			}
			else 
			{
				getLog().debug("Column length matches expectations");
			}
		}
		return true;
	}
}


