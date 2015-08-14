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
	/**
	 * The name of the config property for obtaining the publisher List.
	 */
	private static final String CONFIGURATION_PUBLISHER_NAME = "space.activity.routes.outputs";
	
	/**
	 * The name of the config property for obtaining the subscriber List.
	 */
	private static final String CONFIGURATION_SUBSCRIBER_NAME = "space.activity.routes.inputs";
	
	/**
	 * The topic names for publishing data
	 * PUBLISHER MAPPING
	 * 
	 * publishers[0] -> outputWP 
	 * Topic Name : waypoint/output
	 * Usage : Send output to the mavlink activity after reading from file/request.
	 */
	private static String publishers[];

	/**
	 * The topic names for subscribing data 
	 * SUBSCRIBER MAPPING
	 * 
	 * subscribers[0] -> inputWP 
	 * Topic Name : waypoint/input
	 * Usage : Receive data from mavlink activity and process request.
	 */
	private static String subscribers[];
	
	/**
	 * File name constant.
	 */
	private static final String FILE_NAME = "mission.txt";
	
	/**
	 * Contains the directory of the file associated with FILE_NAME
	 */
	private static String fileWithDirectory;
	
	/**
	 * Separator used in the mission text file.
	 */
	private static final String SEPARATOR ="\t";
	
	/**
	 * Buffered Reader instance for reading from file.
	 */
	private BufferedReader br;
	
	/**
	 * String containing the present line.
	 */
	private String currentLine;
	
	/**
	 * Total number of waypoint data in the mission file.
	 */
	private short waypointCount;
	
	/**
	 * Flag array to store which waypoint data has been sent.
	 */
	private boolean wpSendFlag[];
	
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.waypoint.generator setup");
        
        publishers = getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME).split(":");
        subscribers = getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME).split(":");
        fileWithDirectory = getActivityFilesystem().getInstallDirectory().getAbsolutePath()+"/"+FILE_NAME;
        
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
		/*Map<String, Object> temp = Maps.newHashMap();
		String temps = "START-" + Short.toString(waypointCount);
		temp.put("mission", temps);
		sendOutputJson(publisherName, temp);
		getLog().info(temps);*/
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
	public void onNewInputJson(String channelName, Map<String, Object> message)
	{
		if (channelName.equals(subscribers[0]))
		{

			// To Do
			String msgFromDrone[] = message.get("mission").toString()
					.split("-");
			if (msgFromDrone[0].equals("START"))
			{
				processFile();
				Map<String, Object> temp = Maps.newHashMap();
				String temps = "START-" + Short.toString(waypointCount);
				temp.put("mission", temps);
				sendOutputJson(publishers[0], temp);
				getLog().debug(temps);
			}
			
			else if (msgFromDrone[0].equals("MISSION_REQUEST"))
			{
				if (!wpSendFlag[Integer.parseInt(msgFromDrone[1])])
				{

					try
					{
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
						// getLog().info("REQUEST NUMBER : " + msgFromDrone[1]);
						for (int i = 0; i <= Integer.parseInt(msgFromDrone[1]); i++)
						{
							currentLine = br.readLine(); // Seek to the current
															// line

						}

						String payLoad[] = currentLine.split(SEPARATOR);
						// getLog().info("SENDING REQUEST NUMBER : " +
						// payLoad[0]);
						Map<String, Object> temp = Maps.newHashMap();
						temp.put("mission", Arrays.toString(payLoad));
						sendOutputJson(publishers[0], temp);
						br.close();
					}
					catch (IOException e)
					{
						getLog().error(e);
					}
					wpSendFlag[Integer.parseInt(msgFromDrone[1])] = true;
				}
				
				if (Integer.parseInt(msgFromDrone[1]) == (waypointCount-1))
				{
					for (int i = 0; i < wpSendFlag.length; i++)
					{
						wpSendFlag[i] = false;
					}
				}
			}
			
			else if (msgFromDrone[0].equals("MISSION_ACCEPTED"))
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

	private void processFile()
	{
		int lineCount = 1;
		try
		{
			br = new BufferedReader(new FileReader(fileWithDirectory));
			while ((currentLine = br.readLine()) != null)
			{
				if (lineCount != 1)
				{
					currentLine = currentLine.trim();
					if (!checkRowLength(currentLine))
					{
						getLog().error(
								"Aborting file read due to row length inconsistency");
						return;
					}

					if (!checkRowContent(currentLine))
					{
						getLog().error(
								"Aborting file read due to row content inconsistency");
						return;
					}

					if (!checkCoordinateFrame(currentLine))
					{
						getLog().error(
								"Aborting file read due to coordinate frame inconsistency");
						return;
					}

					waypointCount = Short.parseShort(currentLine.substring(0,
							currentLine.indexOf(SEPARATOR))); // Not sure that
																// tab is the
																// separator
				}

				lineCount++;
			}
			if (lineCount != (waypointCount + 3))
			{
				getLog().warn(
						"Waypoint count and number of lines mismatch , recheck mission file ");
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
		waypointCount += 1; // So as to accommodate array index 0
		wpSendFlag = new boolean[waypointCount];
		for (int i = 0; i < wpSendFlag.length; i++)
		{
			wpSendFlag[i] = false;
		}
	}
    
	private boolean checkRowLength(String row)
	{
		int separatorCount = 0;
		for (int i = 0; i < row.length(); i++)
		{
			if (row.charAt(i) == SEPARATOR.charAt(0))
			{
				separatorCount++;
			}
		}
		int contentCount = row.split(SEPARATOR).length;
		if (!((separatorCount == 11) | (contentCount == 12)))
		{
			getLog().error(
					"Separator count is not 11 and content count is not 12");
			return false;
		}
		else
		{
			if (separatorCount != 11)
			{
				getLog().warn("Separator count is not equal to 11");
			}
			else if (contentCount != 12)
			{
				getLog().warn("Content count is not equal to 12");
			}
			else
			{
				getLog().debug("Row length matches expectations");
			}
		}
		return true;
	}
    
	private boolean checkRowContent(String row)
	{
		row = row.trim();
		String[] splitRow = row.split(SEPARATOR);
		boolean[] isCorrect = new boolean[splitRow.length];
		boolean flagAbort = false;
		for (int i = 0; i < splitRow.length; i++)
		{
			isCorrect[i] = isDouble(splitRow[i]);
			if (!isCorrect[i])
			{
				flagAbort = true;
			}

		}
		if (flagAbort)
		{
			String msgAbort = "The following rows are not convertible to double : ";
			for (int i = 0; i < isCorrect.length; i++)
			{
				if (isCorrect[i])
				{
					msgAbort += Integer.toString(i) + " ";
				}
			}
			getLog().error(msgAbort);
			return false;
		}
		getLog().debug("All the values in the row are parseable to double");
		return true;
	}

	private boolean isDouble(String value)
	{
		try
		{
			Double.parseDouble(value);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	private boolean isInteger(String value)
	{
		try
		{
			Integer.parseInt(value);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	private boolean checkCoordinateFrame(String row)
	{
		/*
		 * MAV_FRAME_GLOBAL = 0; Global coordinate frame, WGS84 coordinate
		 * system. First value / x: latitude, second value / y: longitude, third
		 * value / z: positive altitude over mean sea level (MSL)
		 * 
		 * MAV_FRAME_LOCAL_NED = 1; Local coordinate frame, Z-up (x: north, y:
		 * east, z: down).
		 * 
		 * MAV_FRAME_MISSION = 2; NOT a coordinate frame, indicates a mission
		 * command.
		 * 
		 * MAV_FRAME_GLOBAL_RELATIVE_ALT = 3; Global coordinate frame, WGS84
		 * coordinate system, relative altitude over ground with respect to the
		 * home position. First value / x: latitude, second value / y:
		 * longitude, third value / z: positive altitude with 0 being at the
		 * altitude of the home location.
		 * 
		 * MAV_FRAME_LOCAL_ENU = 4; Local coordinate frame, Z-down (x: east, y:
		 * north, z: up)
		 * 
		 * MAV_FRAME_GLOBAL_INT = 5; Global coordinate frame, WGS84 coordinate
		 * system. First value / x: latitude in degrees*1.0e-7, second value /
		 * y: longitude in degrees*1.0e-7, third value / z: positive altitude
		 * over mean sea level (MSL)
		 * 
		 * MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6; Global coordinate frame, WGS84
		 * coordinate system, relative altitude over ground with respect to the
		 * home position. First value / x: latitude in degrees*10e-7, second
		 * value / y: longitude in degrees*10e-7, third value / z: positive
		 * altitude with 0 being at the altitude of the home location.
		 * 
		 * MAV_FRAME_LOCAL_OFFSET_NED = 7; Offset to the current local frame.
		 * Anything expressed in this frame should be added to the current local
		 * frame position.
		 * 
		 * MAV_FRAME_BODY_NED = 8; Setpoint in body NED frame. This makes sense
		 * if all position control is externalized - e.g. useful to command 2
		 * m/s^2 acceleration to the right.
		 * 
		 * MAV_FRAME_BODY_OFFSET_NED = 9; Offset in body NED frame. This makes
		 * sense if adding setpoints to the current flight path, to avoid an
		 * obstacle - e.g. useful to command 2 m/s^2 acceleration to the east.
		 * 
		 * MAV_FRAME_GLOBAL_TERRAIN_ALT = 10; Global coordinate frame with above
		 * terrain level altitude. WGS84 coordinate system, relative altitude
		 * over terrain with respect to the waypoint coordinate. First value /
		 * x: latitude in degrees, second value / y: longitude in degrees, third
		 * value / z: positive altitude in meters with 0 being at ground level
		 * in terrain model.
		 * 
		 * MAV_FRAME_GLOBAL_TERRAIN_ALT_INT = 11; Global coordinate frame with
		 * above terrain level altitude. WGS84 coordinate system, relative
		 * altitude over terrain with respect to the waypoint coordinate. First
		 * value / x: latitude in degrees*10e-7, second value / y: longitude in
		 * degrees*10e-7, third value / z: positive altitude in meters with 0
		 * being at ground level in terrain model.
		 * 
		 * MAV_FRAME_ENUM_END = 12;
		 */
		String[] splitRow = row.trim().split(SEPARATOR);
		int coordinateFrame;

		if (isInteger(splitRow[2]))
		{
			coordinateFrame = Integer.parseInt(splitRow[2]);
		}
		else
		{
			getLog().error("Coordinate frame not parseable to int.");
			return false;
		}
		if (coordinateFrame < 0 | coordinateFrame >= 12 | coordinateFrame == 2)
		{
			getLog().error(
					splitRow[0]
							+ " row does not contain a valid coordinate frame.");
			return false;
		}

		if (splitRow[0].equals("0"))
		{
			if ((coordinateFrame != 0) & (coordinateFrame != 5))
			{
				getLog().error(
						"First row does not contain home coordinates in Global Coordinate system.");
				return false;
			}
			else
			{
				if (coordinateFrame == 5)
				{
					if (!isLatLonAltInt(splitRow))
					{
						return false;
					}
				}
			}
		}
		else
		{
			if ((coordinateFrame != 3) & (coordinateFrame != 6))
			{
				getLog().warn(
						splitRow[0]
								+ " row does not contain coordinates in Global Relative Altitude Coordinate system.");
			}
			else
			{
				if (coordinateFrame == 6 | coordinateFrame == 11 | coordinateFrame == 5)
				{
					if (!isLatLonAltInt(splitRow))
					{
						getLog().error(
								splitRow[0]
										+ " row has double values but coordinate frame specified as integer.");
						return false;
					}
				}
				if (coordinateFrame == 8)
				{
					getLog().error(
							splitRow[0]
									+ " row uses BODY_NED coordinate frame. This is to be used in cases involving external position control using accelaration control and not a mission.");
					return false;
				}
				if (coordinateFrame == 9)
				{
					getLog().error(
							splitRow[0]
									+ " row uses BODY_OFFSET_NED coordinate frame. This is to be used in cases involving obstacle avoidance using accelearation control and not a mission.");
					return false;
				}
				if (coordinateFrame == 7)
				{
					getLog().error(
							splitRow[0]
									+ " row uses LOCAL_OFFSET_NED coordinate frame. This is to be used in a mission.");
					return false;
				}
			}
		}
		return true;
	}

	private boolean isLatLonAltInt(String [] rowArray)
	{
		boolean flag = true;
		if (!isInteger(rowArray[8]))
		{
			getLog().error("Longitude in double but coordinate frame in Global Integer.");
			flag = false;
		}
		if (!isInteger(rowArray[9]))
		{
			getLog().error("Latitude in double but coordinate frame in Global Integer.");
			flag = false;
		}
		if (!isInteger(rowArray[10]))
		{
			getLog().error("Altitude in double but coordinate frame in Global Integer.");
			flag = false;
		}
		return flag;
	}
}


