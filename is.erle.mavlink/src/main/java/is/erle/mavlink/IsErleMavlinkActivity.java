package is.erle.mavlink;

import java.util.Arrays;
import java.util.Map;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;

import com.MAVLink.*;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.*;
import com.MAVLink.enums.MAV_MISSION_RESULT;
import com.MAVLink.pixhawk.*;
import com.google.common.collect.Maps;

import java.io.UnsupportedEncodingException;
import java.lang.Class;
import java.lang.reflect.Field;
/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleMavlinkActivity extends BaseRoutableRosActivity {

	/*
	 * Note - In communication between this activity and any other comms activity, the
	 * message type being used is byte []
	 */
	private static final String CONFIGURATION_PUBLISHER_NAME = "space.activity.routes.outputs";
	private static final String CONFIGURATION_SUBSCRIBER_NAME = "space.activity.routes.inputs";
	
	private static String publishers[];
	private static String subscribers[];
	
	private MAVLinkPacket mavPacket;
	private Parser mavParser;
	private MAVLinkMessage mavMessage;
	
	private static byte targetSystem = 0; // TO DO : Get this from the current drone
	private static byte targetComponent = 0; // TO DO : Get this from the current drone
	
	private byte responseGlobal[];
	
	
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.mavlink setup");
        publishers = getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME).split(":");
        subscribers = getConfiguration().getRequiredPropertyString(CONFIGURATION_SUBSCRIBER_NAME).split(":");
        responseGlobal = new byte[1000];
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
//		Map<String, Object> temp = Maps.newHashMap();
//		temp.put("mission", "START");
//		sendOutputJson(getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME), temp);
//		sendOutputJson("outputCOM_M", temp);
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
    
    @Override
    public void onNewInputJson(String channelName, Map <String , Object> message)
    {
        getLog().debug("Got message on input channel " + channelName);
        getLog().debug(message);
    	if (channelName == subscribers[0]) 
    	{
    		//Data from drone handled here
			responseGlobal = (byte[]) message.get("comm");
			for (int i = 0; i < responseGlobal.length; i++) 
			{
				mavPacket = mavParser.mavlink_parse_char(responseGlobal[i]);
			}
			
			if (mavPacket != null) 
			{
				mavMessage = mavPacket.unpack();
				/*Map<String, Object> temp = Maps.newHashMap();
				temp.put("mavMessage", mavMessage);
				sendOutputJson(publishers[2], temp); */
				hadnleMavMessage(mavMessage);
				mavPacket = null;
			}

		}
    	
    	else if (channelName == subscribers[1])
    	{
    		
    		//Waypoint generator message handling here
    		/* 
    		 * Details : http://qgroundcontrol.org/mavlink/waypoint_protocol
    		 * */
			String tempString[] = message.get("mission").toString().split("-");
    		if (tempString[0] == "START") 
    		{
    			short missionCount = Short.parseShort(tempString[1]);
    			msg_mission_count missionStart = new msg_mission_count();
    			missionStart.count = missionCount;
    			missionStart.target_system = targetSystem;
    			missionStart.target_component = targetComponent;
    			byte tempByte[] = missionStart.pack().encodePacket();
    			Map<String, Object> tempMapMission = Maps.newHashMap();
    			tempMapMission.put("comm", tempByte);
    			sendOutputJson(publishers[0], tempMapMission);
			}
    		
    		else
    		{
				
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
    			
    			// Rest of the messages about the waypoint data
				String missionWP[] = (String[]) message.get("mission");
				msg_mission_item missionItem = new msg_mission_item();
				missionItem.seq = Short.parseShort(missionWP[0]);
				missionItem.current = Byte.parseByte(missionWP[1]);
				missionItem.frame = Byte.parseByte(missionWP[2]);
				missionItem.command = Short.parseShort(missionWP[3]);
				missionItem.param1 = Float.parseFloat(missionWP[4]);
				missionItem.param2 = Float.parseFloat(missionWP[5]);
				missionItem.param3 = Float.parseFloat(missionWP[6]);
				missionItem.param4 = Float.parseFloat(missionWP[7]);
				missionItem.x = Float.parseFloat(missionWP[8]);
				missionItem.y = Float.parseFloat(missionWP[9]);
				missionItem.z = Float.parseFloat(missionWP[10]);
				missionItem.autocontinue = Byte.parseByte(missionWP[11]);
				byte tempByte[] = missionItem.pack().encodePacket();
				Map<String, Object> tempMapMission = Maps.newHashMap();
				tempMapMission.put("comm", tempByte);
				sendOutputJson(publishers[0], tempMapMission);
    		}
    		
    	}
    	
    	else if (channelName == subscribers[2])
    	{
    		//Captain message handling here
    	}
    }
    
    /*
     * Use this function to get a variable name which is equal to certain value
     * Intended for getting variables in the enum folder of mavlink package
     */
    private String getVariableName(String className , int matchVar)
    {
		String variableName = null;
    	Class<?> classVar = null;
		try 
		{
			classVar = Class.forName(className);
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
    	Field[] fields = classVar.getFields();
		try {
			for (int i = 0; i < fields.length; i++) 
			{
				if (matchVar == fields[i].getInt(classVar)) 
				{
					variableName =  fields[i].getName();
					break;
				}
			}
		} 
		catch (IllegalArgumentException e) 
		{
			e.printStackTrace();
		} 
		catch (IllegalAccessException e) 
		{
				e.printStackTrace();
		}
		return variableName;
    }

    /*
     * Use this function to get all the variables of a specified class
     * Intended for getting values from mavlink package
     */
    private String [] getVariableNames(String className )
    {
    	String variableNames [];
		Class<?> classVar = null;
    	try 
    	{
    		classVar= Class.forName(className);
		} 
    	catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		}
    	Field [] fields = classVar.getFields();
    	variableNames = new String[fields.length];
    	for (int i = 0; i < fields.length; i++) 
    	{
			variableNames[i] = fields[i].getName();
		}
		return variableNames;
    }
    
	private void hadnleMavMessage(MAVLinkMessage mavMessage2) 
	{
		//To Do
		switch (mavMessage2.msgid) {
		case msg_set_cam_shutter.MAVLINK_MSG_ID_SET_CAM_SHUTTER:

			break;

		case msg_image_triggered.MAVLINK_MSG_ID_IMAGE_TRIGGERED:
			msg_image_triggered mavImageTriggered;
			if (mavMessage2 instanceof msg_image_triggered) 
			{
				mavImageTriggered = (msg_image_triggered) mavMessage2;
				Map<String, Object> tempMavImageTriggered = Maps.newHashMap();
				String tempImageTriggered = "[" + mavImageTriggered.timestamp
						+ "] , " + "SEQUENCE : " + mavImageTriggered.seq
						+ " , " + "ROLL : " + mavImageTriggered.roll + "rad , "
						+ "PITCH : " + mavImageTriggered.pitch + "rad , "
						+ "YAW : " + mavImageTriggered.yaw + "rad , "
						+ "LOCAL HEIGHT : " + mavImageTriggered.local_z
						+ "metres , " + "LATITUDE : " + mavImageTriggered.lat
						+ "degrees , " + "LONGITUDE : " + mavImageTriggered.lon
						+ "degrees , " + "GLOBAL ALTITUDE : "
						+ mavImageTriggered.alt + "metres , "
						+ "GROUND TRUTH X : " + mavImageTriggered.ground_x
						+ " , " + "GROUND TRUTH Y : "
						+ mavImageTriggered.ground_y + " , "
						+ "GROUND TRUTH Z : " + mavImageTriggered.ground_z;
				tempMavImageTriggered.put("data", tempImageTriggered);
				sendOutputJson(publishers[2], tempMavImageTriggered);
				getLog().info(tempImageTriggered);
			}
			break;

		case msg_image_trigger_control.MAVLINK_MSG_ID_IMAGE_TRIGGER_CONTROL:

			break;

		case msg_image_available.MAVLINK_MSG_ID_IMAGE_AVAILABLE:

			break;

		case msg_set_position_control_offset.MAVLINK_MSG_ID_SET_POSITION_CONTROL_OFFSET:

			break;

		case msg_position_control_setpoint.MAVLINK_MSG_ID_POSITION_CONTROL_SETPOINT:
			msg_position_control_setpoint mavPositionControlSetpoint;
			if (mavMessage2 instanceof msg_position_control_setpoint) 
			{
				mavPositionControlSetpoint = (msg_position_control_setpoint) mavMessage2;
				String tempPositionControlSetpoint = "X : "
						+ mavPositionControlSetpoint.x + "metres , " + "Y : "
						+ mavPositionControlSetpoint.y + "metres , " + "Z : "
						+ mavPositionControlSetpoint.z + "metres , " + "YAW : "
						+ mavPositionControlSetpoint.yaw + "rad , " + "ID : "
						+ mavPositionControlSetpoint.id;
				Map<String, Object> tempMavPositionControlSetpoint = Maps
						.newHashMap();
				tempMavPositionControlSetpoint.put("data",
						tempPositionControlSetpoint);
				sendOutputJson(publishers[2], tempMavPositionControlSetpoint);
				getLog().info(tempPositionControlSetpoint);
			}
			break;

		case msg_marker.MAVLINK_MSG_ID_MARKER:
			msg_marker mavMarker;
			if (mavMessage2 instanceof msg_marker) 
			{
				mavMarker = (msg_marker) mavMessage2;
				String tempMarker = "X : " + mavMarker.x + "metres , " + "Y : "
						+ mavMarker.y + "metres , " + "Z : " + mavMarker.z
						+ "metres , " + "ROLL : " + mavMarker.roll + "rad , "
						+ "PITCH : " + mavMarker.pitch + "rad , " + "YAW : "
						+ mavMarker.yaw + "rad , " + "ID : " + mavMarker.id;
				Map<String, Object> tempMavMarker = Maps.newHashMap();
				tempMavMarker.put("data", tempMarker);
				sendOutputJson(publishers[2], tempMavMarker);
				getLog().info(tempMarker);
			}
			break;

		case msg_raw_aux.MAVLINK_MSG_ID_RAW_AUX:
			msg_raw_aux mavRawAux;
			if (mavMessage2 instanceof msg_raw_aux) 
			{
				mavRawAux = (msg_raw_aux) mavMessage2;
				String tempRawAux = "PRESSURE : " + mavRawAux.baro * 100.0
						+ "Pascal , " + "ADC1 (AD0.6) : " + mavRawAux.adc1
						+ " , " + "ADC2 (AD0.2) : " + mavRawAux.adc2 + " , "
						+ "ADC3 (AD0.1) : " + mavRawAux.adc3 + " , "
						+ "ADC4 (AD1.3) : " + mavRawAux.adc4 + " , "
						+ "BATTERY VOLTAGE : " + mavRawAux.vbat + " , "
						+ "TEMPERATURE : " + mavRawAux.temp;
				Map<String, Object> tempMavRawAux = Maps.newHashMap();
				tempMavRawAux.put("data", tempRawAux);
				sendOutputJson(publishers[2], tempMavRawAux);
				getLog().info(tempRawAux);
			}
			break;

		case msg_watchdog_heartbeat.MAVLINK_MSG_ID_WATCHDOG_HEARTBEAT:
			msg_watchdog_heartbeat mavWatchdogHeartbeat;
			if (mavMessage2 instanceof msg_watchdog_heartbeat) 
			{
				mavWatchdogHeartbeat = (msg_watchdog_heartbeat) mavMessage2;
				String tempWatchdogHeartbeat = "WATCHDOG ID : "
						+ mavWatchdogHeartbeat.watchdog_id + " , "
						+ "PROCESS COUNT : "
						+ mavWatchdogHeartbeat.process_count;
				Map<String, Object> tempMavWatchdogHeartbeat = Maps
						.newHashMap();
				tempMavWatchdogHeartbeat.put("data", tempWatchdogHeartbeat);
				sendOutputJson(publishers[2], tempMavWatchdogHeartbeat);
				getLog().info(tempWatchdogHeartbeat);
			}
			break;

		case msg_watchdog_process_info.MAVLINK_MSG_ID_WATCHDOG_PROCESS_INFO:
			msg_watchdog_process_info mavWatchdogProcessInfo;
			if (mavMessage2 instanceof msg_watchdog_process_info) 
			{
				mavWatchdogProcessInfo = (msg_watchdog_process_info) mavMessage2;

				String processName = null;
				String processArguments = null;
				try 
				{
					processName = new String(mavWatchdogProcessInfo.name,
							"UTF-8");
					processArguments = new String(
							mavWatchdogProcessInfo.arguments, "UTF-8");
				} 
				catch (UnsupportedEncodingException e) 
				{
					getLog().error(e);
				}
				String tempWatchdogProcessInfo = "TIMEOUT : "
						+ mavWatchdogProcessInfo.timeout + " , "
						+ "WATCHDOG ID : " + mavWatchdogProcessInfo.watchdog_id
						+ " , " + "PROCESS ID : "
						+ mavWatchdogProcessInfo.process_id + " , "
						+ "PROCESS NAME : " + processName + " , "
						+ "PROCESS ARGUMENTS : " + processArguments;
				Map<String, Object> tempMavWatchdogProcessInfo = Maps
						.newHashMap();
				tempMavWatchdogProcessInfo.put("data", tempWatchdogProcessInfo);
				sendOutputJson(publishers[2], tempMavWatchdogProcessInfo);
				getLog().info(tempWatchdogProcessInfo);
			}
			break;

		case msg_watchdog_process_status.MAVLINK_MSG_ID_WATCHDOG_PROCESS_STATUS:
			msg_watchdog_process_status mavWatchdogProcessStatus;
			if (mavMessage2 instanceof msg_watchdog_process_status) 
			{
				mavWatchdogProcessStatus = (msg_watchdog_process_status) mavMessage2;
				String[] processStatus = { "RUNNING", "FINISHED", "SUSPENDED",
						"CRASHED" };
				String tempWatchdogProcessStatus = "PID : "
						+ mavWatchdogProcessStatus.pid + " , "
						+ "WATCHDOG ID : "
						+ mavWatchdogProcessStatus.watchdog_id + " , "
						+ "PROCESS ID : " + mavWatchdogProcessStatus.process_id
						+ " , " + "CRASH COUNT : "
						+ mavWatchdogProcessStatus.crashes + " , "
						+ "PRESENT STATE : " + processStatus[mavWatchdogProcessStatus.state]
						+ " , " + "IS MUTED : "
						+ mavWatchdogProcessStatus.muted;
				
				/**
				 * State : Is running / finished / suspended / crashed
				 */
				
				Map<String, Object> tempMavWatchdogProcessStatus = Maps
						.newHashMap();
				tempMavWatchdogProcessStatus.put("data",
						tempWatchdogProcessStatus);
				sendOutputJson(publishers[2], tempMavWatchdogProcessStatus);
				getLog().info(tempWatchdogProcessStatus);
			}
			break;

		case msg_watchdog_command.MAVLINK_MSG_ID_WATCHDOG_COMMAND:

			break;

		case msg_pattern_detected.MAVLINK_MSG_ID_PATTERN_DETECTED:
			msg_pattern_detected mavPatternDetected;
			if (mavMessage2 instanceof msg_pattern_detected) {
				mavPatternDetected = (msg_pattern_detected) mavMessage2;

				String fileName = null;
				try 
				{
					fileName = new String(mavPatternDetected.file, "UTF-8");
				} 
				catch (UnsupportedEncodingException e) 
				{
					getLog().error(e);
				}
				String tempPatternDetected = "CONFIDENCE : "
						+ mavPatternDetected.confidence + " , " + "TYPE : "
						+ mavPatternDetected.type + " , " + "FILE NAME : "
						+ fileName + " , " + "DETECTED : "
						+ mavPatternDetected.detected;
				Map<String, Object> tempMavPatternDetected = Maps.newHashMap();
				tempMavPatternDetected.put("data", tempPatternDetected);
				sendOutputJson(publishers[2], tempMavPatternDetected);
				getLog().info(tempPatternDetected);
			}
			break;

		case msg_point_of_interest.MAVLINK_MSG_ID_POINT_OF_INTEREST:
			msg_point_of_interest mavPointOfInterest;
			if (mavMessage2 instanceof msg_point_of_interest) {
				mavPointOfInterest = (msg_point_of_interest) mavMessage2;
				String[] PoiColor = { "BLUE", "YELLOW", "RED", "ORANGE",
						"GREEN", "MAGNETA" };
				String[] PoiType = { "NOTICE", "WARNING", "CRITICAL",
						"EMERGENCY", "DEBUG" };
				String[] PoiCoordinateSystem = { "GLOBAL", "LOCAL" };
				String poiName = null;
				try 
				{
					poiName = new String(mavPointOfInterest.name, "UTF-8");
				} 
				catch (UnsupportedEncodingException e)
				{
					getLog().error(e);
				}
				String tempPointOfInterest = "X : "
						+ mavPointOfInterest.x
						+ "metres , "
						+ "Y : "
						+ mavPointOfInterest.y
						+ "metres , "
						+ "Z : "
						+ mavPointOfInterest.z
						+ "metres , "
						+ "TIMEOUT : "
						+ mavPointOfInterest.timeout
						+ "s , "
						+ "TYPE : "
						+ PoiType[mavPointOfInterest.type]
						+ " , "
						+ "COLOR : "
						+ PoiColor[mavPointOfInterest.color]
						+ " , "
						+ "COORDINATE SYSTEM : "
						+ PoiCoordinateSystem[mavPointOfInterest.coordinate_system]
						+ " , " + "POI NAME : " + poiName;
				Map<String, Object> tempMavPointOfInterest = Maps.newHashMap();
				tempMavPointOfInterest.put("data", tempPointOfInterest);
				sendOutputJson(publishers[2], tempMavPointOfInterest);
				getLog().info(tempPointOfInterest);
			}
			break;

		case msg_point_of_interest_connection.MAVLINK_MSG_ID_POINT_OF_INTEREST_CONNECTION:
			msg_point_of_interest_connection mavPointOfInterestConnection;
			if (mavMessage2 instanceof msg_point_of_interest_connection) {
				mavPointOfInterestConnection = (msg_point_of_interest_connection) mavMessage2;
				String[] PoiColor = { "BLUE", "YELLOW", "RED", "ORANGE",
						"GREEN", "MAGNETA" };
				String[] PoiType = { "NOTICE", "WARNING", "CRITICAL",
						"EMERGENCY", "DEBUG" };
				String[] PoiCoordinateSystem = { "GLOBAL", "LOCAL" };
				String poiName = null;
				try 
				{
					poiName = new String(mavPointOfInterestConnection.name, "UTF-8");
				} 
				catch (UnsupportedEncodingException e)
				{
					getLog().error(e);
				}
				String tempPointOfInterestConnection = "X1 : "
						+ mavPointOfInterestConnection.xp1
						+ "metres , "
						+ "Y1 : "
						+ mavPointOfInterestConnection.yp1
						+ "metres , "
						+ "Z1 : "
						+ mavPointOfInterestConnection.zp1
						+ "metres , "
						+ "X2 : "
						+ mavPointOfInterestConnection.xp2
						+ "metres , "
						+ "Y2 : "
						+ mavPointOfInterestConnection.yp2
						+ "metres , "
						+ "Z2 : "
						+ mavPointOfInterestConnection.zp2
						+ "metres , "
						+ "TIMEOUT : "
						+ mavPointOfInterestConnection.timeout
						+ "s , "
						+ "TYPE : "
						+ PoiType[mavPointOfInterestConnection.type]
						+ " , "
						+ "COLOR : "
						+ PoiColor[mavPointOfInterestConnection.color]
						+ " , "
						+ "COORDINATE SYSTEM : "
						+ PoiCoordinateSystem[mavPointOfInterestConnection.coordinate_system]
						+ " , " + "POI NAME : " + poiName;
				Map<String, Object> tempMavPointOfInterestConnection = Maps.newHashMap();
				tempMavPointOfInterestConnection.put("data", tempPointOfInterestConnection);
				sendOutputJson(publishers[2], tempMavPointOfInterestConnection);
				getLog().info(tempPointOfInterestConnection);
			}
			break;

		case msg_brief_feature.MAVLINK_MSG_ID_BRIEF_FEATURE:
			msg_brief_feature mavBriefFeature;
			if (mavMessage2 instanceof msg_brief_feature) 
			{
				mavBriefFeature = (msg_brief_feature) mavMessage2;
				String featureDescriptor = null;
				try 
				{
					featureDescriptor = new String(mavBriefFeature.descriptor,
							"UTF-8");
				} 
				catch (UnsupportedEncodingException e) 
				{
					getLog().error(e);
				}
				String tempBriefFeature = "X : " + mavBriefFeature.x
						+ "metres , " + "Y : " + mavBriefFeature.y
						+ "metres , " + "Z : " + mavBriefFeature.z
						+ "metres , " + "RESPONSE : "
						+ mavBriefFeature.response + " , " + "SIZE : "
						+ mavBriefFeature.size + "pixels , " + "ORIENTATION : "
						+ mavBriefFeature.orientation + " , "
						+ "ORIENTATION ASSIGNMENT : "
						+ mavBriefFeature.orientation_assignment + " , "
						+ "DESCRIPTOR : " + featureDescriptor;
				Map<String, Object> tempMavBriefFeature = Maps.newHashMap();
				tempMavBriefFeature.put("data", tempBriefFeature);
				sendOutputJson(publishers[2], tempMavBriefFeature);
				getLog().info(tempBriefFeature);
			}
			break;

		case msg_attitude_control.MAVLINK_MSG_ID_ATTITUDE_CONTROL:

			break;

		case msg_detection_stats.MAVLINK_MSG_ID_DETECTION_STATS:
			msg_detection_stats mavDetectionStats;
			if (mavMessage2 instanceof msg_detection_stats) 
			{
				mavDetectionStats = (msg_detection_stats) mavMessage2;
				Map<String, Object> tempMavDetectionStats = Maps.newHashMap();
				String tempDetectionStats = "NUMBER OF DETECTIONS : "
						+ mavDetectionStats.detections + " ,"
						+ "NUMBER OF CLUSTER ITERATIONS : "
						+ mavDetectionStats.cluster_iters + " , "
						+ "BEST SCORE : " + mavDetectionStats.best_score
						+ " , " + "BEST LATITUDE : "
						+ mavDetectionStats.best_lat / 10000000.0
						+ "degrees , " + "BEST LONGITUDE : "
						+ mavDetectionStats.best_lon / 10000000.0
						+ "degrees , " + "BEST ALTITUDE : "
						+ mavDetectionStats.best_alt / 1000.0 + "metres , "
						+ "BEST DETECTION ID : "
						+ mavDetectionStats.best_detection_id + " , "
						+ "BEST CLUSTER ID : "
						+ mavDetectionStats.best_cluster_id + " , "
						+ "BEST CLUSTER ITERATION ID : "
						+ mavDetectionStats.best_cluster_iter_id + " , "
						+ "NUMBER OF IMAGE PROCESSED : "
						+ mavDetectionStats.images_done + " , "
						+ "NUMBER OF IMAGES TO PROCESS : "
						+ mavDetectionStats.images_todo + " , " + "FPS : "
						+ mavDetectionStats.fps;
				tempMavDetectionStats.put("data", tempDetectionStats);
				sendOutputJson(publishers[2], tempMavDetectionStats);
				getLog().info(tempDetectionStats);
			}
			break;

		case msg_onboard_health.MAVLINK_MSG_ID_ONBOARD_HEALTH:
			msg_onboard_health mavOnboardHealth;
			if (mavMessage2 instanceof msg_onboard_health) 
			{
				mavOnboardHealth = (msg_onboard_health) mavMessage2;
				String[] tempDiskHealth = { "N/A", "ERROR", "READ ONLY",
						"READ WRITE" };
				Map<String, Object> tempMavOnboardHealth = Maps.newHashMap();
				String tempOnboardHealth = "UPTIME : "
						+ mavOnboardHealth.uptime + "s , " + "TOTAL RAM : "
						+ mavOnboardHealth.ram_total + "GB , "
						+ "TOTAL SWAP : " + mavOnboardHealth.swap_total
						+ "GB , " + "TOTAL DISK : "
						+ mavOnboardHealth.disk_total + "GB , "
						+ "TEMPERATURE : " + mavOnboardHealth.temp
						+ "degree Celsius , " + "SUPPLY VOLTAGE : "
						+ mavOnboardHealth.voltage + "V , "
						+ "NETWORK INBOUND : "
						+ mavOnboardHealth.network_load_in + "KB/s , "
						+ "NETWORK OUTBOUND : "
						+ mavOnboardHealth.network_load_out + "KB/s , "
						+ "CPU FREQUENCY : " + mavOnboardHealth.cpu_freq
						+ "MHz , " + "CPU LOAD : " + mavOnboardHealth.cpu_load
						+ "% , " + "RAM USED : " + mavOnboardHealth.ram_usage
						+ "% , " + "SWAP USED : " + mavOnboardHealth.swap_usage
						+ "% , " + "DISK HEALTH : "
						+ tempDiskHealth[mavOnboardHealth.disk_health] + " , "
						+ "DISK USED : " + mavOnboardHealth.disk_usage + "% ";
				tempMavOnboardHealth.put("data", tempOnboardHealth);
				sendOutputJson(publishers[2], tempMavOnboardHealth);
				getLog().info(tempOnboardHealth);
			}
			break;

		case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
			msg_heartbeat mavHeartbeat;
			if (mavMessage2 instanceof msg_heartbeat) 
			{
				mavHeartbeat = (msg_heartbeat) mavMessage2;
				Map<String, Object> tempMavHeartbeat = Maps.newHashMap();
				String tempHeartbeat = "TYPE : "
						+ getVariableName("MAV_TYPE", mavHeartbeat.type)
						+ ","
						+ "AUTOPILOT : "
						+ getVariableName("MAV_MODE_FLAG",
								mavHeartbeat.autopilot) + ","
						+ "BASE MODE : "
						+ getVariableName("MA_MODE_FLAG",
								mavHeartbeat.base_mode) + ","
						+ "STATUS : "
						+ getVariableName("MAV_STATE",
								mavHeartbeat.system_status)+ ","
						+ "MAVLINK VERSION : "
						+ Byte.toString(mavHeartbeat.mavlink_version);
				tempMavHeartbeat.put("data", tempHeartbeat);
				sendOutputJson(publishers[2], tempMavHeartbeat);
				getLog().info(tempHeartbeat);
			}
			break;

		case msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS:
			Map<String,	Object> tempMavSysStatus = Maps.newHashMap();
			tempMavSysStatus.put("status", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavSysStatus);
			getLog().info(mavMessage2.toString());
			break;

		case msg_system_time.MAVLINK_MSG_ID_SYSTEM_TIME:
			Map<String,	Object> tempMavSysTime = Maps.newHashMap();
			tempMavSysTime.put("status", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavSysTime);
			getLog().info(mavMessage2.toString());
			break;

		case msg_ping.MAVLINK_MSG_ID_PING:

			break;

		case msg_change_operator_control.MAVLINK_MSG_ID_CHANGE_OPERATOR_CONTROL:

			break;

		case msg_change_operator_control_ack.MAVLINK_MSG_ID_CHANGE_OPERATOR_CONTROL_ACK:

			break;

		case msg_auth_key.MAVLINK_MSG_ID_AUTH_KEY:

			break;

		case msg_set_mode.MAVLINK_MSG_ID_SET_MODE:

			break;

		case msg_param_request_read.MAVLINK_MSG_ID_PARAM_REQUEST_READ:

			break;

		case msg_param_request_list.MAVLINK_MSG_ID_PARAM_REQUEST_LIST:

			break;

		case msg_param_value.MAVLINK_MSG_ID_PARAM_VALUE:

			break;

		case msg_param_set.MAVLINK_MSG_ID_PARAM_SET:

			break;

		case msg_gps_raw_int.MAVLINK_MSG_ID_GPS_RAW_INT:
			msg_gps_raw_int mavGps;
			if (mavMessage2 instanceof msg_gps_raw_int) 
			{
				mavGps = (msg_gps_raw_int) mavMessage2;
				Map<String, Object> tempMavGps = Maps.newHashMap();
				String tempGps = "[" + mavGps.time_usec + "] ," + "LATITUDE : "
						+ mavGps.lat / 10000000.0 + "degrees , "
						+ "LONGITUDE : " + mavGps.lon / 10000000.0
						+ "degrees , " + "ALTITUDE : " + mavGps.alt / 1000.0
						+ "metres , " + "HORIZONTAL DILUTION : "
						+ mavGps.eph / 100.0 + "metres , "
						+ "VERTICAL DILUTION : " + mavGps.epv / 100.0
						+ "metres , " + "VELOCITY : " + mavGps.vel / 100.0
						+ "m/s , " + "COURSE OVER GROUND : " + mavGps.cog
						/ 100.0 + "degrees , " + "FIX TYPE : "
						+ mavGps.fix_type + "D , " + "SATELLITES VISIBLE : "
						+ mavGps.satellites_visible;
				tempMavGps.put("data", tempGps);
				sendOutputJson(publishers[2], tempMavGps);
				getLog().info(tempGps);
			}
			break;

		case msg_gps_status.MAVLINK_MSG_ID_GPS_STATUS:
			getLog().info(((msg_gps_status) mavMessage2).toString());
			break;

		case msg_scaled_imu.MAVLINK_MSG_ID_SCALED_IMU:
			msg_scaled_imu mavScaledImu;
			if (mavMessage2 instanceof msg_scaled_imu) 
			{
				mavScaledImu = (msg_scaled_imu) mavMessage2;
				String tempScaledImu = "[" + mavScaledImu.time_boot_ms + "] , "
						+ "ACCELARATION X : " + mavScaledImu.xacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavScaledImu.yacc / 100000.0 + "metres/sec2 , "
						+ "ACCELARATION Z : " + mavScaledImu.zacc / 100000.0
						+ "metres/sec2 , " + "OMEGA X : "
						+ mavScaledImu.xgyro / 1000.0 + "rad/s , "
						+ "OMEGA Y : " + mavScaledImu.ygyro / 1000.0
						+ "rad/s , " + "OMEGA Z : "
						+ mavScaledImu.zgyro / 1000.0 + "rad/s , "
						+ "MAGNETIC FIELD X : " + mavScaledImu.xmag / 1000.0
						+ "Tesla , " + "MAGNETIC FIELD Y : "
						+ mavScaledImu.ymag / 1000.0 + "Tesla , "
						+ "MAGNETIC FIELD Z : " + mavScaledImu.zmag / 1000.0
						+ "Tesla";
				Map<String, Object> tempMavScaledImu = Maps.newHashMap();
				tempMavScaledImu.put("data", tempScaledImu);
				sendOutputJson(publishers[2], tempMavScaledImu);
				getLog().info(tempScaledImu);
			}
			break;

		case msg_raw_imu.MAVLINK_MSG_ID_RAW_IMU:
			msg_raw_imu mavRawImu;
			if (mavMessage2 instanceof msg_raw_imu) 
			{
				mavRawImu = (msg_raw_imu) mavMessage2;
				String tempRawImu = "[" + mavRawImu.time_usec + "] , "
						+ "ACCELARATION X : " + mavRawImu.xacc + "raw , "
						+ "ACCELARATION Y : " + mavRawImu.yacc + "raw , "
						+ "ACCELARATION Z : " + mavRawImu.zacc + "raw , "
						+ "OMEGA X : " + mavRawImu.xgyro + "raw , "
						+ "OMEGA Y : " + mavRawImu.ygyro + "raw , "
						+ "OMEGA Z : " + mavRawImu.zgyro + "raw , "
						+ "MAGNETIC FIELD X : " + mavRawImu.xmag + "raw , "
						+ "MAGNETIC FIELD Y : " + mavRawImu.ymag + "raw , "
						+ "MAGNETIC FIELD Z : " + mavRawImu.zmag + "raw";
				Map<String, Object> tempMavRawImu = Maps.newHashMap();
				tempMavRawImu.put("data", tempRawImu);
				sendOutputJson(publishers[2], tempMavRawImu);
				getLog().info(tempRawImu);
			}
			break;

		case msg_raw_pressure.MAVLINK_MSG_ID_RAW_PRESSURE:
			msg_raw_pressure mavRawPressure;
			if (mavMessage2 instanceof msg_raw_pressure) 
			{
				mavRawPressure = (msg_raw_pressure) mavMessage2;
				String tempRawPressure = "[" + mavRawPressure.time_usec
						+ "] , " + "ABSOLUTE PRESSURE : "
						+ mavRawPressure.press_abs + "raw , "
						+ "DIFFERENTIAL PRESSURE 1 : "
						+ mavRawPressure.press_diff1 + "raw , "
						+ "DIFFERENTIAL PRESSURE 2 : "
						+ mavRawPressure.press_diff2 + "raw , "
						+ "TEMPERATURE : " + mavRawPressure.temperature
						+ "raw ";
				Map<String, Object> tempMavRawPressure = Maps.newHashMap();
				tempMavRawPressure.put("data", tempRawPressure);
				sendOutputJson(publishers[2], tempMavRawPressure);
				getLog().info(tempRawPressure);
			}
			break;

		case msg_scaled_pressure.MAVLINK_MSG_ID_SCALED_PRESSURE:
			msg_scaled_pressure  mavScaledPressure;
			if (mavMessage2 instanceof msg_scaled_pressure) 
			{
				mavScaledPressure = (msg_scaled_pressure) mavMessage2;
				String tempScaledPressure = "["
						+ mavScaledPressure.time_boot_ms + "] , "
						+ "ABSOLUTE PRESSURE : " + mavScaledPressure.press_abs
						* 100.0 + "Pascal , " + "DIFFERENTIAL PRESSURE 1 : "
						+ mavScaledPressure.press_diff * 100 + "Pascal , "
						+ "TEMPERATURE : " + mavScaledPressure.temperature
						/ 100.0 + "degree Cesius ";
				Map<String, Object> tempMavScaledPressure = Maps.newHashMap();
				tempMavScaledPressure.put("data", tempScaledPressure);
				sendOutputJson(publishers[2], tempMavScaledPressure);
				getLog().info(tempScaledPressure);
			}
			break;

		case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
			msg_attitude  mavAttitude;
			if (mavMessage2 instanceof msg_attitude) 
			{
				mavAttitude = (msg_attitude) mavMessage2 ;
				String tempAttitude = "[" + mavAttitude.time_boot_ms + "] , "
						+ "ROLL : " + mavAttitude.roll + "rad , " + "PITCH : "
						+ mavAttitude.pitch + "rad , " + "YAW : "
						+ mavAttitude.yaw + "rad , " + "ROLL SPEED : "
						+ mavAttitude.rollspeed + "rad/s , " + "PITCH SPEED : "
						+ mavAttitude.pitchspeed + "rad/s , " + "YAW SPEED : "
						+ mavAttitude.yawspeed + "rad/s";
				Map<String , Object> tempMavAttitude = Maps.newHashMap();
				tempMavAttitude.put("data", tempAttitude);
				sendOutputJson(publishers[2], tempMavAttitude);
				getLog().info(tempAttitude);
			}
			break;

		case msg_attitude_quaternion.MAVLINK_MSG_ID_ATTITUDE_QUATERNION:
			msg_attitude_quaternion  mavAttitudeQuaternion;
			if (mavMessage2 instanceof msg_attitude_quaternion) 
			{
				mavAttitudeQuaternion = (msg_attitude_quaternion) mavMessage2 ;
				String tempAttitudeQuaternion = "["
						+ mavAttitudeQuaternion.time_boot_ms + "] , "
						+ "QUATERNION COMPONENT 1 : "
						+ mavAttitudeQuaternion.q1 + " , "
						+ "QUATERNION COMPONENT 2 : "
						+ mavAttitudeQuaternion.q2 + " , "
						+ "QUATERNION COMPONENT 3 : "
						+ mavAttitudeQuaternion.q3 + " , "
						+ "QUATERNION COMPONENT 4 : "
						+ mavAttitudeQuaternion.q4 + " , " + "ROLL SPEED : "
						+ mavAttitudeQuaternion.rollspeed + "rad/s , "
						+ "PITCH SPEED : " + mavAttitudeQuaternion.pitchspeed
						+ "rad/s , " + "YAW SPEED : "
						+ mavAttitudeQuaternion.yawspeed + "rad/s";
				Map<String , Object> tempMavAttitudeQuaternion = Maps.newHashMap();
				tempMavAttitudeQuaternion.put("data", tempAttitudeQuaternion);
				sendOutputJson(publishers[2], tempMavAttitudeQuaternion);
				getLog().info(tempAttitudeQuaternion);
			}
			break;

		case msg_local_position_ned.MAVLINK_MSG_ID_LOCAL_POSITION_NED:
			msg_local_position_ned mavLocalPosition;
			if (mavMessage2 instanceof msg_local_position_ned) 
			{
				mavLocalPosition = (msg_local_position_ned) mavMessage2;
				String tempLocalPosition = "[" + mavLocalPosition.time_boot_ms
						+ "]," + "X : " + mavLocalPosition.x + "metres , "
						+ "Y : " + mavLocalPosition.y + "metres , " + "Z : "
						+ mavLocalPosition.z + "metres , " + "VELOCITY X : "
						+ mavLocalPosition.vx + "m/s , " + "VELOCITY Y : "
						+ mavLocalPosition.vy + "m/s , " + "VELOCITY Z : "
						+ mavLocalPosition.vz + "m/s ";
				Map<String, Object> tempMavLocalPosition = Maps.newHashMap();
				tempMavLocalPosition.put("data", tempLocalPosition);
				sendOutputJson(publishers[2], tempMavLocalPosition);
				getLog().info(tempLocalPosition);
			}
			break;

		case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
			msg_global_position_int mavGlobalPosition;
			if (mavMessage2 instanceof msg_global_position_int) 
			{
				mavGlobalPosition = (msg_global_position_int) mavMessage2;
				String tempGlobalPosition = "["
						+ mavGlobalPosition.time_boot_ms + "]," + "LATITUDE : "
						+ mavGlobalPosition.lat / 10000000.0 + "degrees , "
						+ "LONGITUDE : " + mavGlobalPosition.lon / 10000000.0
						+ "degrees , " + "ALTITUDE : "
						+ mavGlobalPosition.alt / 1000.0 + "metres , "
						+ "RELATIVE ALTITUDE : "
						+ mavGlobalPosition.relative_alt / 1000.0 + "metres , "
						+ "VELOCITY X : " + mavGlobalPosition.vx / 100.0
						+ "m/s , " + "VELOCITY Y : "
						+ mavGlobalPosition.vy / 100.0 + "m/s , "
						+ "VELOCITY Z : " + mavGlobalPosition.vz / 100.0
						+ "m/s , " + "HEADING : " + mavGlobalPosition.hdg
						/ 100.0 + "degrees";
				Map<String, Object> tempMavGlobalPosition = Maps.newHashMap();
				tempMavGlobalPosition.put("data", tempGlobalPosition);
				sendOutputJson(publishers[2], tempMavGlobalPosition);
				getLog().info(tempGlobalPosition);
			}
			break;

		case msg_rc_channels_scaled.MAVLINK_MSG_ID_RC_CHANNELS_SCALED:
			msg_rc_channels_scaled mavRcChannelScaled;
			if (mavMessage2 instanceof msg_rc_channels_scaled) 
			{
				mavRcChannelScaled = (msg_rc_channels_scaled) mavMessage2;
				String tempRcChannelScaled = "["
						+ mavRcChannelScaled.time_boot_ms + "],"
						+ "CHANNEL 1 : " + mavRcChannelScaled.chan1_scaled
						+ " , " + "CHANNEL 2 : "
						+ mavRcChannelScaled.chan2_scaled + " , "
						+ "CHANNEL 3 : " + mavRcChannelScaled.chan3_scaled
						+ " , " + "CHANNEL 4 : "
						+ mavRcChannelScaled.chan4_scaled + " , "
						+ "CHANNEL 5 : " + mavRcChannelScaled.chan5_scaled
						+ " , " + "CHANNEL 6 : "
						+ mavRcChannelScaled.chan6_scaled + " , "
						+ "CHANNEL 7 : " + mavRcChannelScaled.chan7_scaled
						+ " , " + "CHANNEL 8 : "
						+ mavRcChannelScaled.chan8_scaled + " , " + "PORT : "
						+ mavRcChannelScaled.port + " , "
						+ "SIGNAL STRENGTH : " + mavRcChannelScaled.rssi;

				/**
				 * RC channels value scaled, (-100%) -10000, (0%) 0, (100%)
				 * 10000, (invalid) INT16_MAX.
				 */

				/**
				 * Receive signal strength indicator, 0: 0%, 100: 100%, 255:
				 * invalid/unknown.
				 */

				Map<String, Object> tempMavRcChannelScaled = Maps.newHashMap();
				tempMavRcChannelScaled.put("data", tempRcChannelScaled);
				sendOutputJson(publishers[2], tempMavRcChannelScaled);
				getLog().info(tempRcChannelScaled);
			}
			break;

		case msg_rc_channels_raw.MAVLINK_MSG_ID_RC_CHANNELS_RAW:
			msg_rc_channels_raw mavRcChannelRaw;
			if (mavMessage2 instanceof msg_rc_channels_raw) 
			{
				mavRcChannelRaw = (msg_rc_channels_raw) mavMessage2;
				String tempRcChannelRaw = "[" + mavRcChannelRaw.time_boot_ms
						+ "] ," + "CHANNEL 1 : " + mavRcChannelRaw.chan1_raw
						+ " , " + "CHANNEL 2 : " + mavRcChannelRaw.chan2_raw
						+ " , " + "CHANNEL 3 : " + mavRcChannelRaw.chan3_raw
						+ " , " + "CHANNEL 4 : " + mavRcChannelRaw.chan4_raw
						+ " , " + "CHANNEL 5 : " + mavRcChannelRaw.chan5_raw
						+ " , " + "CHANNEL 6 : " + mavRcChannelRaw.chan6_raw
						+ " , " + "CHANNEL 7 : " + mavRcChannelRaw.chan7_raw
						+ " , " + "CHANNEL 8 : " + mavRcChannelRaw.chan8_raw
						+ " , " + "PORT : " + mavRcChannelRaw.port + " , "
						+ "SIGNAL STRENGTH : " + mavRcChannelRaw.rssi;

				/**
				 * RC channels value in microseconds. A value of UINT16_MAX
				 * implies the channel is unused.
				 */

				/**
				 * Receive signal strength indicator, 0: 0%, 100: 100%, 255:
				 * invalid/unknown.
				 */

				Map<String, Object> tempMavRcChannelRaw = Maps.newHashMap();
				tempMavRcChannelRaw.put("data", tempRcChannelRaw);
				sendOutputJson(publishers[2], tempMavRcChannelRaw);
				getLog().info(tempRcChannelRaw);
			}
			break;

		case msg_servo_output_raw.MAVLINK_MSG_ID_SERVO_OUTPUT_RAW:

			break;

		case msg_mission_request_partial_list.MAVLINK_MSG_ID_MISSION_REQUEST_PARTIAL_LIST:

			break;

		case msg_mission_write_partial_list.MAVLINK_MSG_ID_MISSION_WRITE_PARTIAL_LIST:

			break;

		case msg_mission_item.MAVLINK_MSG_ID_MISSION_ITEM:

			break;

		case msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST:
			/*
			 * This mavlink message is sent on receipt of waypoint count and 
			 * when asking for the next waypoint 
			 */
			msg_mission_request mavMissionRequest;
			if (mavMessage2 instanceof msg_mission_request) 
			{
				mavMissionRequest = (msg_mission_request) mavMessage2;
				String tempStringRequest = "MISSION_REQUEST-" + Short.toString(mavMissionRequest.seq);
				Map<String, Object> tempMapMissionRequest = Maps.newHashMap();
				tempMapMissionRequest.put("mission", tempStringRequest);
				sendOutputJson(publishers[1], tempMapMissionRequest);
			}
			break;

		case msg_mission_set_current.MAVLINK_MSG_ID_MISSION_SET_CURRENT:

			break;

		case msg_mission_current.MAVLINK_MSG_ID_MISSION_CURRENT:

			break;

		case msg_mission_request_list.MAVLINK_MSG_ID_MISSION_REQUEST_LIST:

			break;

		case msg_mission_count.MAVLINK_MSG_ID_MISSION_COUNT:

			break;

		case msg_mission_clear_all.MAVLINK_MSG_ID_MISSION_CLEAR_ALL:

			break;

		case msg_mission_item_reached.MAVLINK_MSG_ID_MISSION_ITEM_REACHED:

			break;

		case msg_mission_ack.MAVLINK_MSG_ID_MISSION_ACK:
			msg_mission_ack mavMissionAck;
			if (mavMessage2 instanceof msg_mission_ack) 
			{
				Map<String, Object> tempMapMissionAck = Maps.newHashMap();
				mavMissionAck = (msg_mission_ack) mavMessage2;
				switch (mavMissionAck.type) 
				{
				case MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED:
					/*
					 *  mission accepted OK | 
					 *  */
					tempMapMissionAck.put("mission", "MISSION_ACCEPTED");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_ERROR:
					/* 
					 * generic error / not accepting mission commands at all right now | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_ERROR");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_UNSUPPORTED_FRAME:
					/* 
					 * coordinate frame is not supported | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_UNSUPPORTED_FRAME");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_UNSUPPORTED:
					/* 
					 * command is not supported | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_UNSUPPORTED");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_NO_SPACE:
					/* 
					 * mission item exceeds storage space | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_NO_SPACE");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_INVALID:
					/* 
					 * one of the parameters has an invalid value | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_INVALID");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM1:
					/* 
					 * param1 has an invalid value | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_INVALID_PARAM1");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM2:
					/* 
					 * param2 has an invalid value | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_INVALID_PARAM2");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM3:
					/* 
					 * param3 has an invalid value | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_INVALID_PARAM3");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM4:
					/* 
					 * param4 has an invalid value | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_INVALID_PARAM4");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM5_X:
					/* 
					 * x/param5 has an invalid value | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_INVALID_PARAM5_X");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM6_Y:
					/* 
					 * y/param6 has an invalid value | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_INVALID_PARAM6_Y");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_INVALID_PARAM7:
					/* 
					 * param7 has an invalid value | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_INVALID_PARAM7");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_INVALID_SEQUENCE :
					/* 
					 * received waypoint out of sequence | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_INVALID_SEQUENCE");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				case MAV_MISSION_RESULT.MAV_MISSION_RESULT_ENUM_END:
					/* 
					 * not accepting any mission commands from this communication partner | 
					 * */
					tempMapMissionAck.put("mission", "MISSION_RESULT_ENUM_END");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;

				default:
					tempMapMissionAck.put("mission", "MISSION_RESULT_UNKNOWN");
					sendOutputJson(publishers[1], tempMapMissionAck);
					break;
				}
			}
			break;

		case msg_set_gps_global_origin.MAVLINK_MSG_ID_SET_GPS_GLOBAL_ORIGIN:

			break;

		case msg_gps_global_origin.MAVLINK_MSG_ID_GPS_GLOBAL_ORIGIN:
			msg_gps_global_origin mavGpsGlobalOrigin;
			if (mavMessage2 instanceof msg_gps_global_origin) 
			{
				mavGpsGlobalOrigin = (msg_gps_global_origin) mavMessage2 ;
				Map<String, Object> tempMavGpsGlobalOrigin = Maps.newHashMap();
				String tempGpsGlobalOrigin = "LATITUDE : "
						+ mavGpsGlobalOrigin.latitude / 10000000.0
						+ "degrees , " + "LONGITUDE : "
						+ mavGpsGlobalOrigin.longitude / 10000000.0
						+ "degrees , " + "ALTITUDE : "
						+ mavGpsGlobalOrigin.altitude / 1000.0 + "metres";
				tempMavGpsGlobalOrigin.put("gps" , tempGpsGlobalOrigin);
				sendOutputJson(publishers[2], tempMavGpsGlobalOrigin);
				getLog().info(tempGpsGlobalOrigin);
			}
			break;

		case msg_param_map_rc.MAVLINK_MSG_ID_PARAM_MAP_RC:

			break;

		case msg_safety_set_allowed_area.MAVLINK_MSG_ID_SAFETY_SET_ALLOWED_AREA:

			break;

		case msg_safety_allowed_area.MAVLINK_MSG_ID_SAFETY_ALLOWED_AREA:

			break;

		case msg_attitude_quaternion_cov.MAVLINK_MSG_ID_ATTITUDE_QUATERNION_COV:
			msg_attitude_quaternion_cov  mavAttitudeQuaternionCov;
			if (mavMessage2 instanceof msg_attitude_quaternion_cov) 
			{
				mavAttitudeQuaternionCov = (msg_attitude_quaternion_cov) mavMessage2 ;
				String tempAttitudeQuaternionCov = "["
						+ mavAttitudeQuaternionCov.time_boot_ms + "] , "
						+ "QUATERNION COMPONENT 1 : "
						+ mavAttitudeQuaternionCov.q[1] + " , "
						+ "QUATERNION COMPONENT 2 : "
						+ mavAttitudeQuaternionCov.q[2] + " , "
						+ "QUATERNION COMPONENT 3 : "
						+ mavAttitudeQuaternionCov.q[3] + " , "
						+ "QUATERNION COMPONENT 4 : "
						+ mavAttitudeQuaternionCov.q[4] + " , "
						+ "ROLL SPEED : " + mavAttitudeQuaternionCov.rollspeed
						+ "rad/s , " + "PITCH SPEED : "
						+ mavAttitudeQuaternionCov.pitchspeed + "rad/s , "
						+ "YAW SPEED : " + mavAttitudeQuaternionCov.yawspeed
						+ "rad/s" + "COVARIANCE MATRIX : "
						+ Arrays.toString(mavAttitudeQuaternionCov.covariance);
				Map<String , Object> tempMavAttitudeQuaternionCov = Maps.newHashMap();
				tempMavAttitudeQuaternionCov.put("data", tempAttitudeQuaternionCov);
				sendOutputJson(publishers[2], tempMavAttitudeQuaternionCov);
				getLog().info(tempAttitudeQuaternionCov);
			}
			break;

		case msg_nav_controller_output.MAVLINK_MSG_ID_NAV_CONTROLLER_OUTPUT:

			break;

		case msg_global_position_int_cov.MAVLINK_MSG_ID_GLOBAL_POSITION_INT_COV:
			msg_global_position_int_cov mavGlobalPositionIntCov;
			if (mavMessage2 instanceof msg_global_position_int_cov) 
			{
				mavGlobalPositionIntCov = (msg_global_position_int_cov) mavMessage2;
				String tempGlobalPositionIntCov = "["
						+ mavGlobalPositionIntCov.time_boot_ms + "],"
						+ "LATITUDE : " + mavGlobalPositionIntCov.lat
						+ "degrees , " + "LONGITUDE : "
						+ mavGlobalPositionIntCov.lon + "degrees , "
						+ "ALTITUDE : " + mavGlobalPositionIntCov.alt
						+ "metres , " + "RELATIVE ALTITUDE : "
						+ mavGlobalPositionIntCov.relative_alt + "metres , "
						+ "VELOCITY X : " + mavGlobalPositionIntCov.vx
						+ "m/s , " + "VELOCITY Y : "
						+ mavGlobalPositionIntCov.vy + "m/s , "
						+ "VELOCITY Z : " + mavGlobalPositionIntCov.vz
						+ "m/s , " + "COVARIANCE : "
						+ Arrays.toString(mavGlobalPositionIntCov.covariance)
						+ " , " + "TYPE : "
						+ mavGlobalPositionIntCov.estimator_type
						+ "UTC TIME : " + mavGlobalPositionIntCov.time_utc;
				/**
				 * Covariance matrix (first six entries are the first ROW, next
				 * six entries are the second row, etc.)
				 */
				Map<String, Object> tempMavGlobalPositionIntCov = Maps
						.newHashMap();
				tempMavGlobalPositionIntCov.put("data",
						tempGlobalPositionIntCov);
				sendOutputJson(publishers[2], tempMavGlobalPositionIntCov);
				getLog().info(tempGlobalPositionIntCov);
			}
			break;

		case msg_local_position_ned_cov.MAVLINK_MSG_ID_LOCAL_POSITION_NED_COV:
			msg_local_position_ned_cov mavLocalPositionCov;
			if (mavMessage2 instanceof msg_local_position_ned_cov) 
			{
				mavLocalPositionCov = (msg_local_position_ned_cov) mavMessage2;
				String tempLocalPositionCov = "["
						+ mavLocalPositionCov.time_boot_ms + "]," + "X : "
						+ mavLocalPositionCov.x + "metres , " + "Y : "
						+ mavLocalPositionCov.y + "metres , " + "Z : "
						+ mavLocalPositionCov.z + "metres , " + "VELOCITY X : "
						+ mavLocalPositionCov.vx + "m/s , " + "VELOCITY Y : "
						+ mavLocalPositionCov.vy + "m/s , " + "VELOCITY Z : "
						+ mavLocalPositionCov.vz + "m/s , "
						+ "ACCELARATION X : " + mavLocalPositionCov.ax
						+ "m/s2 , " + "ACCELARATION Y : "
						+ mavLocalPositionCov.ay + "m/s2 , "
						+ "ACCELARATION Z : " + mavLocalPositionCov.az
						+ "m/s2 , " + "COVARIANCE : "
						+ Arrays.toString(mavLocalPositionCov.covariance)
						+ " , " + "TYPE : "
						+ mavLocalPositionCov.estimator_type;
				/**
				 * Covariance matrix upper right triangular (first nine entries
				 * are the first ROW, next eight entries are the second row,
				 * etc.)
				 */
				Map<String, Object> tempMavLocalPositionCov = Maps.newHashMap();
				tempMavLocalPositionCov.put("data", tempLocalPositionCov);
				sendOutputJson(publishers[2], tempMavLocalPositionCov);
				getLog().info(tempLocalPositionCov);
			}
			break;

		case msg_rc_channels.MAVLINK_MSG_ID_RC_CHANNELS:
			msg_rc_channels mavRcChannels;
			if (mavMessage2 instanceof msg_rc_channels) 
			{
				mavRcChannels = (msg_rc_channels) mavMessage2;
				String tempRcChannels = "[" + mavRcChannels.time_boot_ms
						+ "] ," + "CHANNEL 1 : " + mavRcChannels.chan1_raw
						+ " , " + "CHANNEL 2 : " + mavRcChannels.chan2_raw
						+ " , " + "CHANNEL 3 : " + mavRcChannels.chan3_raw
						+ " , " + "CHANNEL 4 : " + mavRcChannels.chan4_raw
						+ " , " + "CHANNEL 5 : " + mavRcChannels.chan5_raw
						+ " , " + "CHANNEL 6 : " + mavRcChannels.chan6_raw
						+ " , " + "CHANNEL 7 : " + mavRcChannels.chan7_raw
						+ " , " + "CHANNEL 8 : " + mavRcChannels.chan8_raw
						+ " , " + "CHANNEL 9 : " + mavRcChannels.chan9_raw
						+ " , " + "CHANNEL 10 : " + mavRcChannels.chan10_raw
						+ " , " + "CHANNEL 12 : " + mavRcChannels.chan12_raw
						+ " , " + "CHANNEL 13 : " + mavRcChannels.chan13_raw
						+ " , " + "CHANNEL 14 : " + mavRcChannels.chan14_raw
						+ " , " + "CHANNEL 15 : " + mavRcChannels.chan15_raw
						+ " , " + "CHANNEL 16 : " + mavRcChannels.chan16_raw
						+ " , " + "CHANNEL 17 : " + mavRcChannels.chan17_raw
						+ " , " + "CHANNEL 18 : " + mavRcChannels.chan18_raw
						+ " , " + "CHANNEL COUNT : "
						+ mavRcChannels.chancount + " , "
						+ "SIGNAL STRENGTH : " + mavRcChannels.rssi;

				/**
				 * RC channels value in microseconds. A value of UINT16_MAX
				 * implies the channel is unused.
				 */

				/**
				 * Receive signal strength indicator, 0: 0%, 100: 100%, 255:
				 * invalid/unknown.
				 */

				Map<String, Object> tempMavRcChannels = Maps.newHashMap();
				tempMavRcChannels.put("data", tempRcChannels);
				sendOutputJson(publishers[2], tempMavRcChannels);
				getLog().info(tempRcChannels);
			}
			break;

		case msg_request_data_stream.MAVLINK_MSG_ID_REQUEST_DATA_STREAM:

			break;

		case msg_data_stream.MAVLINK_MSG_ID_DATA_STREAM:

			break;

		case msg_manual_control.MAVLINK_MSG_ID_MANUAL_CONTROL:

			break;

		case msg_rc_channels_override.MAVLINK_MSG_ID_RC_CHANNELS_OVERRIDE:

			break;

		case msg_mission_item_int.MAVLINK_MSG_ID_MISSION_ITEM_INT:

			break;

		case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD:

			break;

		case msg_command_int.MAVLINK_MSG_ID_COMMAND_INT:

			break;

		case msg_command_long.MAVLINK_MSG_ID_COMMAND_LONG:

			break;

		case msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK:

			break;

		case msg_manual_setpoint.MAVLINK_MSG_ID_MANUAL_SETPOINT:

			break;

		case msg_set_attitude_target.MAVLINK_MSG_ID_SET_ATTITUDE_TARGET:

			break;

		case msg_attitude_target.MAVLINK_MSG_ID_ATTITUDE_TARGET:
			msg_attitude_target  mavAttitudeTarget;
			if (mavMessage2 instanceof msg_attitude_target) 
			{
				mavAttitudeTarget = (msg_attitude_target) mavMessage2;
				String tempAttitudeTarget = "["
						+ mavAttitudeTarget.time_boot_ms + "] , "
						+ "QUATERNION COMPONENT 1 : " + mavAttitudeTarget.q[1]
						+ " , " + "QUATERNION COMPONENT 2 : "
						+ mavAttitudeTarget.q[2] + " , "
						+ "QUATERNION COMPONENT 3 : " + mavAttitudeTarget.q[3]
						+ " , " + "QUATERNION COMPONENT 4 : "
						+ mavAttitudeTarget.q[4] + " , " + "BODY ROLL SPEED : "
						+ mavAttitudeTarget.body_roll_rate + "rad/s , "
						+ "BODY PITCH SPEED : "
						+ mavAttitudeTarget.body_pitch_rate + "rad/s , "
						+ "BODY YAW SPEED : " + mavAttitudeTarget.body_yaw_rate
						+ "rad/s" + "THRUST : " + mavAttitudeTarget.thrust;
				/**
				 * mavAttitudeTarget.type_mask
				 * Mappings: If any of these bits are set, the corresponding
				 * input should be ignored: bit 1: body roll rate, bit 2: body
				 * pitch rate, bit 3: body yaw rate. bit 4-bit 7: reserved, bit
				 * 8: attitude
				 */
				Map<String , Object> tempMavAttitudeTarget = Maps.newHashMap();
				tempMavAttitudeTarget.put("data", tempAttitudeTarget);
				sendOutputJson(publishers[2], tempMavAttitudeTarget);
				getLog().info(tempAttitudeTarget);
			}
			break;

		case msg_set_position_target_local_ned.MAVLINK_MSG_ID_SET_POSITION_TARGET_LOCAL_NED:

			break;

		case msg_position_target_local_ned.MAVLINK_MSG_ID_POSITION_TARGET_LOCAL_NED:
			msg_position_target_local_ned mavPositionTargetLocalNed;
			if (mavMessage2 instanceof msg_position_target_local_ned) 
			{
				mavPositionTargetLocalNed = (msg_position_target_local_ned) mavMessage2;
				String tempPositionTargetLocalNed = "["
						+ mavPositionTargetLocalNed.time_boot_ms + "],"
						+ "X : " + mavPositionTargetLocalNed.x + "metres , "
						+ "Y : " + mavPositionTargetLocalNed.y + "metres , "
						+ "Z : " + mavPositionTargetLocalNed.z + "metres , "
						+ "VELOCITY X : " + mavPositionTargetLocalNed.vx
						+ "m/s , " + "VELOCITY Y : "
						+ mavPositionTargetLocalNed.vy + "m/s , "
						+ "VELOCITY Z : " + mavPositionTargetLocalNed.vz
						+ "m/s , " + "ACCELARATION X : "
						+ mavPositionTargetLocalNed.afx + "m/s2 , "
						+ "ACCELARATION Y : " + mavPositionTargetLocalNed.afy
						+ "m/s2 , " + "ACCELARATION Z : "
						+ mavPositionTargetLocalNed.afz + "m/s2 , " + "YAW : "
						+ mavPositionTargetLocalNed.yaw + "rad , "
						+ "YAW RATE : " + mavPositionTargetLocalNed.yaw_rate
						+ "rad/s , " + "TYPE MASK : " + " , "
						+ mavPositionTargetLocalNed.type_mask
						+ "COORDINATE FRAME : "
						+ mavPositionTargetLocalNed.coordinate_frame;
				/**
				 * Coordinate Frame Options Valid options are:
				 * MAV_FRAME_LOCAL_NED = 1, MAV_FRAME_LOCAL_OFFSET_NED = 7,
				 * MAV_FRAME_BODY_NED = 8, MAV_FRAME_BODY_OFFSET_NED = 9
				 */
				Map<String, Object> tempMavPositionTargetLocalNed = Maps
						.newHashMap();
				tempMavPositionTargetLocalNed.put("data",
						tempPositionTargetLocalNed);
				sendOutputJson(publishers[2], tempMavPositionTargetLocalNed);
				getLog().info(tempPositionTargetLocalNed);
			}
			break;

		case msg_set_position_target_global_int.MAVLINK_MSG_ID_SET_POSITION_TARGET_GLOBAL_INT:

			break;

		case msg_position_target_global_int.MAVLINK_MSG_ID_POSITION_TARGET_GLOBAL_INT:
			msg_position_target_global_int mavPositionTargetGlobalInt;
			if (mavMessage2 instanceof msg_position_target_global_int) 
			{
				mavPositionTargetGlobalInt = (msg_position_target_global_int) mavMessage2;
				String tempPositionTargetGlobalInt = "["
						+ mavPositionTargetGlobalInt.time_boot_ms + "],"
						+ "LATITUDE : "
						+ mavPositionTargetGlobalInt.lat_int / 10000000.0
						+ "degrees , " + "LONGITUDE : "
						+ mavPositionTargetGlobalInt.lon_int / 10000000.0
						+ "degrees , " + "ALTITUDE : "
						+ mavPositionTargetGlobalInt.alt + "metres , "
						+ "VELOCITY X : " + mavPositionTargetGlobalInt.vx
						+ "m/s , " + "VELOCITY Y : "
						+ mavPositionTargetGlobalInt.vy + "m/s , "
						+ "VELOCITY Z : " + mavPositionTargetGlobalInt.vz
						+ "m/s , " + "ACCELARATION X : "
						+ mavPositionTargetGlobalInt.afx + "m/s2 , "
						+ "ACCELARATION Y : " + mavPositionTargetGlobalInt.afy
						+ "m/s2 , " + "ACCELARATION Z : "
						+ mavPositionTargetGlobalInt.afz + "m/s2 , " + "YAW : "
						+ mavPositionTargetGlobalInt.yaw + "rad , "
						+ "YAW RATE : " + mavPositionTargetGlobalInt.yaw_rate
						+ "rad/s , " + "TYPE MASK : " + " , "
						+ mavPositionTargetGlobalInt.type_mask
						+ "COORDINATE FRAME : "
						+ mavPositionTargetGlobalInt.coordinate_frame;
				/**
				 * Valid options are: MAV_FRAME_GLOBAL_INT = 5,
				 * MAV_FRAME_GLOBAL_RELATIVE_ALT_INT = 6,
				 * MAV_FRAME_GLOBAL_TERRAIN_ALT_INT = 11
				 */
				Map<String, Object> tempMavPositionTargetGlobalInt = Maps
						.newHashMap();
				tempMavPositionTargetGlobalInt.put("data",
						tempPositionTargetGlobalInt);
				sendOutputJson(publishers[2], tempMavPositionTargetGlobalInt);
				getLog().info(tempPositionTargetGlobalInt);
			}
			break;

		case msg_local_position_ned_system_global_offset.MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET:
			msg_local_position_ned_system_global_offset mavOffsetPositionLocalGlobal;
			if (mavMessage2 instanceof msg_local_position_ned_system_global_offset) 
			{
				mavOffsetPositionLocalGlobal = (msg_local_position_ned_system_global_offset) mavMessage2;
				String tempOffsetPositionLocalGlobal = "["
						+ mavOffsetPositionLocalGlobal.time_boot_ms + "],"
						+ "OFFSET X : " + mavOffsetPositionLocalGlobal.x
						+ "metres , " + "OFFSET Y : "
						+ mavOffsetPositionLocalGlobal.y + "metres , "
						+ "OFFSET Z : " + mavOffsetPositionLocalGlobal.z
						+ "metres , " + "OFFSET ROLL : "
						+ mavOffsetPositionLocalGlobal.roll + "rad , "
						+ "OFFSET PITCH : "
						+ mavOffsetPositionLocalGlobal.pitch + "rad , "
						+ "OFFSET YAW : " + mavOffsetPositionLocalGlobal.yaw
						+ "rad ";
				Map<String, Object> tempMavOffsetPositionLocalGlobal = Maps
						.newHashMap();
				tempMavOffsetPositionLocalGlobal.put("data",
						tempOffsetPositionLocalGlobal);
				sendOutputJson(publishers[2], tempMavOffsetPositionLocalGlobal);
				getLog().info(tempOffsetPositionLocalGlobal);
			}
			break;

		case msg_hil_state.MAVLINK_MSG_ID_HIL_STATE:
			msg_hil_state  mavHilState;
			if (mavMessage2 instanceof msg_hil_state) 
			{
				mavHilState = (msg_hil_state) mavMessage2;
				String tempHilState = "[" + mavHilState.time_usec + "] , "
						+ "ROLL : " + mavHilState.roll + "rad , " + "PITCH : "
						+ mavHilState.pitch + "rad , " + "YAW : "
						+ mavHilState.yaw + "rad , " + "ROLL SPEED : "
						+ mavHilState.rollspeed + "rad/s , " + "PITCH SPEED : "
						+ mavHilState.pitchspeed + "rad/s , " + "YAW SPEED : "
						+ mavHilState.yawspeed + "LATITUDE : "
						+ mavHilState.lat / 10000000.0 + "degrees , "
						+ "LONGITUDE : " + mavHilState.lon / 10000000.0
						+ "degrees , " + "ALTITUDE : " + mavHilState.alt
						/ 1000.0 + "metres , " + "VELOCITY X : "
						+ mavHilState.vx / 100.0 + "m/s , " + "VELOCITY Y : "
						+ mavHilState.vy / 100.0 + "m/s , " + "VELOCITY Z : "
						+ mavHilState.vz / 100.0 + "m/s , "
						+ "ACCELARATION X : " + mavHilState.xacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavHilState.yacc / 100000.0 + "metres/sec2 , "
						+ "ACCELARATION Z : " + mavHilState.zacc / 100000.0
						+ "metres/sec2";
				Map<String, Object> tempMavHilState = Maps.newHashMap();
				tempMavHilState.put("data", tempHilState);
				sendOutputJson(publishers[2], tempMavHilState);
				getLog().info(tempHilState);
			}
			break;

		case msg_hil_controls.MAVLINK_MSG_ID_HIL_CONTROLS:

			break;

		case msg_hil_rc_inputs_raw.MAVLINK_MSG_ID_HIL_RC_INPUTS_RAW:
			msg_hil_rc_inputs_raw mavHilRcInputRaw;
			if (mavMessage2 instanceof msg_hil_rc_inputs_raw) 
			{
				mavHilRcInputRaw = (msg_hil_rc_inputs_raw) mavMessage2;
				String tempHilRcInputRaw = "[" + mavHilRcInputRaw.time_usec
						+ "] ," + "CHANNEL 1 : " + mavHilRcInputRaw.chan1_raw
						+ " , " + "CHANNEL 2 : " + mavHilRcInputRaw.chan2_raw
						+ " , " + "CHANNEL 3 : " + mavHilRcInputRaw.chan3_raw
						+ " , " + "CHANNEL 4 : " + mavHilRcInputRaw.chan4_raw
						+ " , " + "CHANNEL 5 : " + mavHilRcInputRaw.chan5_raw
						+ " , " + "CHANNEL 6 : " + mavHilRcInputRaw.chan6_raw
						+ " , " + "CHANNEL 7 : " + mavHilRcInputRaw.chan7_raw
						+ " , " + "CHANNEL 8 : " + mavHilRcInputRaw.chan8_raw
						+ " , " + "CHANNEL 9 : " + mavHilRcInputRaw.chan9_raw
						+ " , " + "CHANNEL 10 : " + mavHilRcInputRaw.chan10_raw
						+ " , " + "CHANNEL 12 : " + mavHilRcInputRaw.chan12_raw
						+ " , " + "SIGNAL STRENGTH : " + mavHilRcInputRaw.rssi;

				/**
				 * RC channels value in microseconds. A value of UINT16_MAX
				 * implies the channel is unused.
				 */

				/**
				 * Receive signal strength indicator, 0: 0%, 100: 100%, 255:
				 * invalid/unknown.
				 */

				Map<String, Object> tempMavHilRcInputRaw = Maps.newHashMap();
				tempMavHilRcInputRaw.put("data", tempHilRcInputRaw);
				sendOutputJson(publishers[2], tempMavHilRcInputRaw);
				getLog().info(tempHilRcInputRaw);
			}
			break;

		case msg_optical_flow.MAVLINK_MSG_ID_OPTICAL_FLOW:
			msg_optical_flow mavOpticalFlow;
			if (mavMessage2 instanceof msg_optical_flow) 
			{
				mavOpticalFlow = (msg_optical_flow) mavMessage2;
				String tempOpticalFlow = "[" + mavOpticalFlow.time_usec
						+ "] , " + "FLOW X : " + mavOpticalFlow.flow_comp_m_x
						+ "metres , " + "FLOW Y : "
						+ mavOpticalFlow.flow_comp_m_y + "metres , "
						+ "DISTANCE : " + mavOpticalFlow.ground_distance
						+ "metres , " + "FLOW PIXELS X : "
						+ mavOpticalFlow.flow_x + "metres , "
						+ "FLOW PIXELS Y : " + mavOpticalFlow.flow_y
						+ "metres , " + "SENSOR ID : "
						+ mavOpticalFlow.sensor_id + " , " + "QUALITY : "
						+ mavOpticalFlow.quality;
				Map<String, Object> tempMavOpticalFlow = Maps.newHashMap();
				tempMavOpticalFlow.put("data", tempOpticalFlow);
				sendOutputJson(publishers[2], tempMavOpticalFlow);
				getLog().info(tempOpticalFlow);
			}
			break;

		case msg_global_vision_position_estimate.MAVLINK_MSG_ID_GLOBAL_VISION_POSITION_ESTIMATE:
			msg_global_vision_position_estimate mavGlobalVisionPositionEstimate;
			if (mavMessage2 instanceof msg_global_vision_position_estimate) 
			{
				mavGlobalVisionPositionEstimate = (msg_global_vision_position_estimate) mavMessage2;
				String tempGlobalVisionPositionEstimate = "["
						+ mavGlobalVisionPositionEstimate.usec + "]," + "X : "
						+ mavGlobalVisionPositionEstimate.x + "metres , "
						+ "Y : " + mavGlobalVisionPositionEstimate.y
						+ "metres , " + "Z : "
						+ mavGlobalVisionPositionEstimate.z + "metres , "
						+ "ROLL : " + mavGlobalVisionPositionEstimate.roll
						+ "rad , " + "PITCH : "
						+ mavGlobalVisionPositionEstimate.pitch + "rad , "
						+ "YAW : " + mavGlobalVisionPositionEstimate.yaw
						+ "rad ";
				Map<String, Object> tempMavGlobalVisionPositionEstimate = Maps
						.newHashMap();
				tempMavGlobalVisionPositionEstimate.put("data",
						tempGlobalVisionPositionEstimate);
				sendOutputJson(publishers[2],
						tempMavGlobalVisionPositionEstimate);
				getLog().info(tempGlobalVisionPositionEstimate);
			}
			break;

		case msg_vision_position_estimate.MAVLINK_MSG_ID_VISION_POSITION_ESTIMATE:
			msg_vision_position_estimate mavVisionPositionEstimate;
			if (mavMessage2 instanceof msg_vision_position_estimate) 
			{
				mavVisionPositionEstimate = (msg_vision_position_estimate) mavMessage2;
				String tempVisionPositionEstimate = "["
						+ mavVisionPositionEstimate.usec + "]," + "X : "
						+ mavVisionPositionEstimate.x + "metres , " + "Y : "
						+ mavVisionPositionEstimate.y + "metres , " + "Z : "
						+ mavVisionPositionEstimate.z + "metres , " + "ROLL : "
						+ mavVisionPositionEstimate.roll + "rad , "
						+ "PITCH : " + mavVisionPositionEstimate.pitch
						+ "rad , " + "YAW : " + mavVisionPositionEstimate.yaw
						+ "rad ";
				Map<String, Object> tempMavVisionPositionEstimate = Maps
						.newHashMap();
				tempMavVisionPositionEstimate.put("data",
						tempVisionPositionEstimate);
				sendOutputJson(publishers[2], tempMavVisionPositionEstimate);
				getLog().info(tempVisionPositionEstimate);
			}
			break;

		case msg_vision_speed_estimate.MAVLINK_MSG_ID_VISION_SPEED_ESTIMATE:
			msg_vision_speed_estimate mavVisionSpeedEstimate;
			if (mavMessage2 instanceof msg_vision_speed_estimate) 
			{
				mavVisionSpeedEstimate = (msg_vision_speed_estimate) mavMessage2;
				String tempVisionSpeedEstimate = "["
						+ mavVisionSpeedEstimate.usec + "]," + "SPEED X : "
						+ mavVisionSpeedEstimate.x + "m/s , " + "SPEED Y : "
						+ mavVisionSpeedEstimate.y + "m/s , " + "SPEED Z : "
						+ mavVisionSpeedEstimate.z + "m/s , ";
				Map<String, Object> tempMavVisionSpeedEstimate = Maps
						.newHashMap();
				tempMavVisionSpeedEstimate.put("data", tempVisionSpeedEstimate);
				sendOutputJson(publishers[2], tempMavVisionSpeedEstimate);
				getLog().info(tempVisionSpeedEstimate);
			}
			break;

		case msg_vicon_position_estimate.MAVLINK_MSG_ID_VICON_POSITION_ESTIMATE:
			msg_vicon_position_estimate mavViconPositionEstimate;
			if (mavMessage2 instanceof msg_vicon_position_estimate) 
			{
				mavViconPositionEstimate = (msg_vicon_position_estimate) mavMessage2;
				String tempViconPositionEstimate = "["
						+ mavViconPositionEstimate.usec + "]," + "X : "
						+ mavViconPositionEstimate.x + "metres , " + "Y : "
						+ mavViconPositionEstimate.y + "metres , " + "Z : "
						+ mavViconPositionEstimate.z + "metres , " + "ROLL : "
						+ mavViconPositionEstimate.roll + "rad , "
						+ "PITCH : " + mavViconPositionEstimate.pitch
						+ "rad , " + "YAW : " + mavViconPositionEstimate.yaw
						+ "rad ";
				Map<String, Object> tempMavViconPositionEstimate = Maps
						.newHashMap();
				tempMavViconPositionEstimate.put("data",
						tempViconPositionEstimate);
				sendOutputJson(publishers[2], tempMavViconPositionEstimate);
				getLog().info(tempViconPositionEstimate);
			}
			break;

		case msg_highres_imu.MAVLINK_MSG_ID_HIGHRES_IMU:
			msg_highres_imu mavHighresImu;
			if (mavMessage2 instanceof msg_highres_imu) 
			{
				mavHighresImu = (msg_highres_imu) mavMessage2;
				String tempHighresImu = "["
						+ mavHighresImu.time_usec
						+ "] , "
						+ "ACCELARATION X : "
						+ mavHighresImu.xacc
						+ "metres/sec2 , "
						+ "ACCELARATION Y : "
						+ mavHighresImu.yacc
						+ "metres/sec2 , "
						+ "ACCELARATION Z : "
						+ mavHighresImu.zacc
						+ "metres/sec2 , "
						+ "OMEGA X : "
						+ mavHighresImu.xgyro
						+ "rad/s , "
						+ "OMEGA Y : "
						+ mavHighresImu.ygyro
						+ "rad/s , "
						+ "OMEGA Z : "
						+ mavHighresImu.zgyro
						+ "rad/s , "
						+ "MAGNETIC FIELD X : "
						+ mavHighresImu.xmag
						+ "Gauss , "
						+ "MAGNETIC FIELD Y : "
						+ mavHighresImu.ymag
						+ "Gauss , "
						+ "MAGNETIC FIELD Z : "
						+ mavHighresImu.zmag
						+ "Gauss , "
						+ "ABSOLUTE PRESSURE : "
						+ mavHighresImu.abs_pressure
						+ "millibar , "
						+ "DIFFERENTIAL PRESSURE : "
						+ mavHighresImu.diff_pressure
						+ "millibar , "
						+ "ALTITUDE FROM PRESSURE : "
						+ mavHighresImu.pressure_alt
						+ "metres , "
						+ "TEMPERATURE : "
						+ mavHighresImu.temperature
						+ "degree Celsius , "
						+ "UPDATED FIELDS : "
						+ Integer.toBinaryString(0xFFFF & mavHighresImu.fields_updated);
				Map<String, Object> tempMavHighresImu = Maps.newHashMap();
				tempMavHighresImu.put("data", tempHighresImu);
				sendOutputJson(publishers[2], tempMavHighresImu);
				getLog().info(tempHighresImu);
			}
			break;

		case msg_optical_flow_rad.MAVLINK_MSG_ID_OPTICAL_FLOW_RAD:
			msg_optical_flow_rad mavOpticalFlowRad;
			if (mavMessage2 instanceof msg_optical_flow_rad) 
			{
				mavOpticalFlowRad = (msg_optical_flow_rad) mavMessage2;
				String tempOpticalFlowRad = "[" + mavOpticalFlowRad.time_usec
						+ "] , " + "INTEGRATION TIME : "
						+ mavOpticalFlowRad.integration_time_us
						+ "micro seconds , " + "FLOW X : "
						+ mavOpticalFlowRad.integrated_x + "rad , "
						+ "FLOW Y : " + mavOpticalFlowRad.integrated_y
						+ "rad , " + "RH ROTATION X : "
						+ mavOpticalFlowRad.integrated_xgyro + "rad , "
						+ "RH ROTATION Y : "
						+ mavOpticalFlowRad.integrated_ygyro + "rad , "
						+ "RH ROTATION Z : "
						+ mavOpticalFlowRad.integrated_zgyro + "rad , "
						+ "DELTA TIME : "
						+ mavOpticalFlowRad.time_delta_distance_us
						+ "micro seconds , " + "DISTANCE : "
						+ mavOpticalFlowRad.distance + "metres , "
						+ "TEMPERATURE : " + mavOpticalFlowRad.temperature
						/ 100.0 + "degree Celsius , " + "SENSOR ID : "
						+ mavOpticalFlowRad.sensor_id + " , " + "QUALITY : "
						+ mavOpticalFlowRad.quality;
				Map<String, Object> tempMavOpticalFlowRad = Maps.newHashMap();
				tempMavOpticalFlowRad.put("data", tempOpticalFlowRad);
				sendOutputJson(publishers[2], tempMavOpticalFlowRad);
				getLog().info(tempOpticalFlowRad);
			}
			break;

		case msg_hil_sensor.MAVLINK_MSG_ID_HIL_SENSOR:
			msg_hil_sensor mavHilSensor;
			if (mavMessage2 instanceof msg_hil_sensor) 
			{
				mavHilSensor = (msg_hil_sensor) mavMessage2;
				String tempHilSensor = "["
						+ mavHilSensor.time_usec
						+ "] , "
						+ "ACCELARATION X : "
						+ mavHilSensor.xacc
						+ "metres/sec2 , "
						+ "ACCELARATION Y : "
						+ mavHilSensor.yacc
						+ "metres/sec2 , "
						+ "ACCELARATION Z : "
						+ mavHilSensor.zacc
						+ "metres/sec2 , "
						+ "OMEGA X : "
						+ mavHilSensor.xgyro
						+ "rad/s , "
						+ "OMEGA Y : "
						+ mavHilSensor.ygyro
						+ "rad/s , "
						+ "OMEGA Z : "
						+ mavHilSensor.zgyro
						+ "rad/s , "
						+ "MAGNETIC FIELD X : "
						+ mavHilSensor.xmag
						+ "Gauss , "
						+ "MAGNETIC FIELD Y : "
						+ mavHilSensor.ymag
						+ "Gauss , "
						+ "MAGNETIC FIELD Z : "
						+ mavHilSensor.zmag
						+ "Gauss , "
						+ "ABSOLUTE PRESSURE : "
						+ mavHilSensor.abs_pressure
						+ "millibar , "
						+ "DIFFERENTIAL PRESSURE : "
						+ mavHilSensor.diff_pressure
						+ "millibar , "
						+ "ALTITUDE FROM PRESSURE : "
						+ mavHilSensor.pressure_alt
						+ "metres , "
						+ "TEMPERATURE : "
						+ mavHilSensor.temperature
						+ "degree Celsius , "
						+ "UPDATED FIELDS : "
						+ Integer.toBinaryString(0xFFFF & mavHilSensor.fields_updated);
				Map<String, Object> tempMavHilSensor = Maps.newHashMap();
				tempMavHilSensor.put("data", tempHilSensor);
				sendOutputJson(publishers[2], tempMavHilSensor);
				getLog().info(tempHilSensor);
			}
			break;

		case msg_sim_state.MAVLINK_MSG_ID_SIM_STATE:

			break;

		case msg_radio_status.MAVLINK_MSG_ID_RADIO_STATUS:

			break;

		case msg_file_transfer_protocol.MAVLINK_MSG_ID_FILE_TRANSFER_PROTOCOL:

			break;

		case msg_timesync.MAVLINK_MSG_ID_TIMESYNC:

			break;

		case msg_camera_trigger.MAVLINK_MSG_ID_CAMERA_TRIGGER:

			break;

		case msg_hil_gps.MAVLINK_MSG_ID_HIL_GPS:
			msg_hil_gps mavHilGps;
			if (mavMessage2 instanceof msg_hil_gps) 
			{
				mavHilGps = (msg_hil_gps) mavMessage2;
				Map<String, Object> tempMavHilGps = Maps.newHashMap();
				String tempHilGps = "[" + mavHilGps.time_usec + "] ,"
						+ "LATITUDE : " + mavHilGps.lat / 10000000.0
						+ "degrees , " + "LONGITUDE : "
						+ mavHilGps.lon / 10000000.0 + "degrees , "
						+ "ALTITUDE : " + mavHilGps.alt / 1000.0 + "metres , "
						+ "HORIZONTAL DILUTION : " + mavHilGps.eph / 100.0
						+ "metres , " + "VERTICAL DILUTION : "
						+ mavHilGps.epv / 100.0 + "metres , " + "VELOCITY : "
						+ mavHilGps.vel / 100.0 + "m/s , "
						+ "COURSE OVER GROUND : " + mavHilGps.cog / 100.0
						+ "degrees , " + "FIX TYPE : " + mavHilGps.fix_type
						+ "D , " + "SATELLITES VISIBLE : "
						+ mavHilGps.satellites_visible + "VELOCITY NORTH : "
						+ mavHilGps.vn + "m/s , " + "VELOCITY EAST : "
						+ mavHilGps.ve + "m/s , " + "VELOCITY DOWN : "
						+ mavHilGps.vd + "m/s ";
				tempMavHilGps.put("data", tempHilGps);
				sendOutputJson(publishers[2], tempMavHilGps);
				getLog().info(tempHilGps);
			}
			break;

		case msg_hil_optical_flow.MAVLINK_MSG_ID_HIL_OPTICAL_FLOW:
			msg_hil_optical_flow mavHilOpticalFlowRad;
			if (mavMessage2 instanceof msg_hil_optical_flow) 
			{
				mavHilOpticalFlowRad = (msg_hil_optical_flow) mavMessage2;
				String tempHilOpticalFlowRad = "["
						+ mavHilOpticalFlowRad.time_usec + "] , "
						+ "INTEGRATION TIME : "
						+ mavHilOpticalFlowRad.integration_time_us
						+ "micro seconds , " + "FLOW X : "
						+ mavHilOpticalFlowRad.integrated_x + "rad , "
						+ "FLOW Y : " + mavHilOpticalFlowRad.integrated_y
						+ "rad , " + "RH ROTATION X : "
						+ mavHilOpticalFlowRad.integrated_xgyro + "rad , "
						+ "RH ROTATION Y : "
						+ mavHilOpticalFlowRad.integrated_ygyro + "rad , "
						+ "RH ROTATION Z : "
						+ mavHilOpticalFlowRad.integrated_zgyro + "rad , "
						+ "DELTA TIME : "
						+ mavHilOpticalFlowRad.time_delta_distance_us
						+ "micro seconds , " + "DISTANCE : "
						+ mavHilOpticalFlowRad.distance + "metres , "
						+ "TEMPERATURE : " + mavHilOpticalFlowRad.temperature
						/ 100.0 + "degree Celsius , " + "SENSOR ID : "
						+ mavHilOpticalFlowRad.sensor_id + " , " + "QUALITY : "
						+ mavHilOpticalFlowRad.quality;
				Map<String, Object> tempMavHilOpticalFlowRad = Maps
						.newHashMap();
				tempMavHilOpticalFlowRad.put("data", tempHilOpticalFlowRad);
				sendOutputJson(publishers[2], tempMavHilOpticalFlowRad);
				getLog().info(tempHilOpticalFlowRad);
			}
			break;

		case msg_hil_state_quaternion.MAVLINK_MSG_ID_HIL_STATE_QUATERNION:
			msg_hil_state_quaternion  mavHilStateQuaternion;
			if (mavMessage2 instanceof msg_hil_state_quaternion) 
			{
				mavHilStateQuaternion = (msg_hil_state_quaternion) mavMessage2;
				String tempHilStateQuaternion = "["
						+ mavHilStateQuaternion.time_usec + "] , "
						+ "QUATERNION COMPONENT 1 : "
						+ mavHilStateQuaternion.attitude_quaternion[1] + " , "
						+ "QUATERNION COMPONENT 2 : "
						+ mavHilStateQuaternion.attitude_quaternion[2] + " , "
						+ "QUATERNION COMPONENT 3 : "
						+ mavHilStateQuaternion.attitude_quaternion[3] + " , "
						+ "QUATERNION COMPONENT 4 : "
						+ mavHilStateQuaternion.attitude_quaternion[4] + " , "
						+ "ROLL SPEED : " + mavHilStateQuaternion.rollspeed
						+ "rad/s , " + "PITCH SPEED : "
						+ mavHilStateQuaternion.pitchspeed + "rad/s , "
						+ "YAW SPEED : " + mavHilStateQuaternion.yawspeed
						+ "LATITUDE : " + mavHilStateQuaternion.lat
						/ 10000000.0 + "degrees , " + "LONGITUDE : "
						+ mavHilStateQuaternion.lon / 10000000.0 + "degrees , "
						+ "ALTITUDE : " + mavHilStateQuaternion.alt / 1000.0
						+ "metres , " + "VELOCITY X : "
						+ mavHilStateQuaternion.vx / 100.0 + "m/s , "
						+ "VELOCITY Y : " + mavHilStateQuaternion.vy / 100.0
						+ "m/s , " + "VELOCITY Z : " + mavHilStateQuaternion.vz
						/ 100.0 + "m/s , " + "INDICATED AIRSPEED : "
						+ mavHilStateQuaternion.ind_airspeed / 100.0 + "m/s , "
						+ "TRUE AIRSPEED : "
						+ mavHilStateQuaternion.true_airspeed / 100.0
						+ "m/s , " + "ACCELARATION X : "
						+ mavHilStateQuaternion.xacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavHilStateQuaternion.yacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Z : "
						+ mavHilStateQuaternion.zacc / 100000.0
						+ "metres/sec2";
				Map<String, Object> tempMavHilStateQuaternion = Maps
						.newHashMap();
				tempMavHilStateQuaternion.put("data", tempHilStateQuaternion);
				sendOutputJson(publishers[2], tempMavHilStateQuaternion);
				getLog().info(tempHilStateQuaternion);
			}
			break;

		case msg_scaled_imu2.MAVLINK_MSG_ID_SCALED_IMU2:
			msg_scaled_imu2 mavScaledImu2;
			if (mavMessage2 instanceof msg_scaled_imu2) 
			{
				mavScaledImu2 = (msg_scaled_imu2) mavMessage2;
				String tempScaledImu2 = "[" + mavScaledImu2.time_boot_ms + "] , "
						+ "ACCELARATION X : " + mavScaledImu2.xacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavScaledImu2.yacc / 100000.0 + "metres/sec2 , "
						+ "ACCELARATION Z : " + mavScaledImu2.zacc / 100000.0
						+ "metres/sec2 , " + "OMEGA X : "
						+ mavScaledImu2.xgyro / 1000.0 + "rad/s , "
						+ "OMEGA Y : " + mavScaledImu2.ygyro / 1000.0
						+ "rad/s , " + "OMEGA Z : "
						+ mavScaledImu2.zgyro / 1000.0 + "rad/s , "
						+ "MAGNETIC FIELD X : " + mavScaledImu2.xmag / 1000.0
						+ "Tesla , " + "MAGNETIC FIELD Y : "
						+ mavScaledImu2.ymag / 1000.0 + "Tesla , "
						+ "MAGNETIC FIELD Z : " + mavScaledImu2.zmag / 1000.0
						+ "Tesla";
				Map<String , Object> tempMavScaledImu2 = Maps.newHashMap();
				tempMavScaledImu2.put("data", tempScaledImu2);
				sendOutputJson(publishers[2], tempMavScaledImu2);
				getLog().info(tempScaledImu2);
			}
			break;

		case msg_log_request_list.MAVLINK_MSG_ID_LOG_REQUEST_LIST:

			break;

		case msg_log_entry.MAVLINK_MSG_ID_LOG_ENTRY:

			break;

		case msg_log_request_data.MAVLINK_MSG_ID_LOG_REQUEST_DATA:

			break;

		case msg_log_data.MAVLINK_MSG_ID_LOG_DATA:

			break;

		case msg_log_erase.MAVLINK_MSG_ID_LOG_ERASE:

			break;

		case msg_log_request_end.MAVLINK_MSG_ID_LOG_REQUEST_END:

			break;

		case msg_gps_inject_data.MAVLINK_MSG_ID_GPS_INJECT_DATA:

			break;

		case msg_gps2_raw.MAVLINK_MSG_ID_GPS2_RAW:
			msg_gps2_raw mavGps2;
			if (mavMessage2 instanceof msg_gps2_raw) 
			{
				mavGps2 = (msg_gps2_raw) mavMessage2;
				Map<String, Object> tempMavGps2 = Maps.newHashMap();
				String tempGps2 = "[" + mavGps2.time_usec + "] ,"
						+ "LATITUDE : " + mavGps2.lat / 10000000.0
						+ "degrees , " + "LONGITUDE : "
						+ mavGps2.lon / 10000000.0 + "degrees , "
						+ "ALTITUDE : " + mavGps2.alt / 1000.0 + "metres , "
						+ "HORIZONTAL DILUTION : " + mavGps2.eph / 100.0
						+ "metres , " + "VERTICAL DILUTION : "
						+ mavGps2.epv / 100.0 + "metres , " + "VELOCITY : "
						+ mavGps2.vel / 100.0 + "m/s , "
						+ "COURSE OVER GROUND : " + mavGps2.cog / 100.0
						+ "degrees , " + "FIX TYPE : " + mavGps2.fix_type
						+ "D , " + "SATELLITES VISIBLE : "
						+ mavGps2.satellites_visible + "DGPS INFO AGE : "
						+ mavGps2.dgps_age + "DGPS SATELLITE NUMBER : "
						+ mavGps2.dgps_numch;
				tempMavGps2.put("data", tempGps2);
				sendOutputJson(publishers[2], tempMavGps2);
				getLog().info(tempGps2);
			}
			break;

		case msg_power_status.MAVLINK_MSG_ID_POWER_STATUS:
			Map<String,	Object> tempMavPowerStatus= Maps.newHashMap();
			tempMavPowerStatus.put("status", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavPowerStatus);
			getLog().info(mavMessage2.toString());
			break;

		case msg_serial_control.MAVLINK_MSG_ID_SERIAL_CONTROL:

			break;

		case msg_gps_rtk.MAVLINK_MSG_ID_GPS_RTK:

			break;

		case msg_gps2_rtk.MAVLINK_MSG_ID_GPS2_RTK:

			break;

		case msg_scaled_imu3.MAVLINK_MSG_ID_SCALED_IMU3:
			msg_scaled_imu3 mavScaledImu3;
			if (mavMessage2 instanceof msg_scaled_imu3) 
			{
				mavScaledImu3 = (msg_scaled_imu3) mavMessage2;
				String tempScaledImu3 = "[" + mavScaledImu3.time_boot_ms + "] , "
						+ "ACCELARATION X : " + mavScaledImu3.xacc / 100000.0
						+ "metres/sec2 , " + "ACCELARATION Y : "
						+ mavScaledImu3.yacc / 100000.0 + "metres/sec2 , "
						+ "ACCELARATION Z : " + mavScaledImu3.zacc / 100000.0
						+ "metres/sec2 , " + "OMEGA X : "
						+ mavScaledImu3.xgyro / 1000.0 + "rad/s , "
						+ "OMEGA Y : " + mavScaledImu3.ygyro / 1000.0
						+ "rad/s , " + "OMEGA Z : "
						+ mavScaledImu3.zgyro / 1000.0 + "rad/s , "
						+ "MAGNETIC FIELD X : " + mavScaledImu3.xmag / 1000.0
						+ "Tesla , " + "MAGNETIC FIELD Y : "
						+ mavScaledImu3.ymag / 1000.0 + "Tesla , "
						+ "MAGNETIC FIELD Z : " + mavScaledImu3.zmag / 1000.0
						+ "Tesla";
				Map<String , Object> tempMavScaledImu3 = Maps.newHashMap();
				tempMavScaledImu3.put("data", tempScaledImu3);
				sendOutputJson(publishers[2], tempMavScaledImu3);
				getLog().info(tempScaledImu3);
			}
			break;

		case msg_data_transmission_handshake.MAVLINK_MSG_ID_DATA_TRANSMISSION_HANDSHAKE:

			break;

		case msg_encapsulated_data.MAVLINK_MSG_ID_ENCAPSULATED_DATA:

			break;

		case msg_distance_sensor.MAVLINK_MSG_ID_DISTANCE_SENSOR:
			msg_distance_sensor mavDistanceSensor;
			if (mavMessage2 instanceof msg_distance_sensor) 
			{
				mavDistanceSensor = (msg_distance_sensor) mavMessage2;
				String tempDistanceSensor = "["
						+ mavDistanceSensor.time_boot_ms
						+ "] , "
						+ "MINIMUM DISTANCE : "
						+ mavDistanceSensor.min_distance
						+ "cm , "
						+ "MAXIMUM DISTANCE : "
						+ mavDistanceSensor.max_distance
						+ "cm , "
						+ "CURRENT DISTANCE : "
						+ mavDistanceSensor.current_distance
						+ "cm , "
						+ "SENSOR TYPE : "
						+ getVariableName("MAV_DISTANCE_SENSOR",
								mavDistanceSensor.type)
						+ "SENSOR ID : "
						+ mavDistanceSensor.id
						+ "SENSOR ORIENTATION : "
						+ getVariableName("MAV_SENSOR_ORIENTATION",
								mavDistanceSensor.orientation)
						+ "COVARIANCE : " + mavDistanceSensor.covariance + "cm";
				Map<String , Object> tempMavDistanceSensor = Maps.newHashMap();
				tempMavDistanceSensor.put("data", tempDistanceSensor);
				sendOutputJson(publishers[2], tempMavDistanceSensor);
				getLog().info(tempDistanceSensor);
			}
			break;

		case msg_terrain_request.MAVLINK_MSG_ID_TERRAIN_REQUEST:

			break;

		case msg_terrain_data.MAVLINK_MSG_ID_TERRAIN_DATA:

			break;

		case msg_terrain_check.MAVLINK_MSG_ID_TERRAIN_CHECK:

			break;

		case msg_terrain_report.MAVLINK_MSG_ID_TERRAIN_REPORT:

			break;

		case msg_scaled_pressure2.MAVLINK_MSG_ID_SCALED_PRESSURE2:
			msg_scaled_pressure2  mavScaledPressure2;
			if (mavMessage2 instanceof msg_scaled_pressure2) 
			{
				mavScaledPressure2 = (msg_scaled_pressure2) mavMessage2;
				String tempScaledPressure2 = "["
						+ mavScaledPressure2.time_boot_ms + "] , "
						+ "ABSOLUTE PRESSURE : " + mavScaledPressure2.press_abs
						* 100.0 + "Pascal , " + "DIFFERENTIAL PRESSURE 1 : "
						+ mavScaledPressure2.press_diff * 100 + "Pascal , "
						+ "TEMPERATURE : " + mavScaledPressure2.temperature
						/ 100.0 + "degree Cesius ";
				Map<String, Object> tempMavScaledPressure2 = Maps.newHashMap();
				tempMavScaledPressure2.put("data", tempScaledPressure2);
				sendOutputJson(publishers[2], tempMavScaledPressure2);
				getLog().info(tempScaledPressure2);
			}
			break;

		case msg_att_pos_mocap.MAVLINK_MSG_ID_ATT_POS_MOCAP:
			msg_att_pos_mocap  mavAttPosMocap;
			if (mavMessage2 instanceof msg_att_pos_mocap) 
			{
				mavAttPosMocap = (msg_att_pos_mocap) mavMessage2;
				String tempAttPosMocap = "[" + mavAttPosMocap.time_usec
						+ "] , " + "QUATERNION COMPONENT 1 : "
						+ mavAttPosMocap.q[1] + " , "
						+ "QUATERNION COMPONENT 2 : " + mavAttPosMocap.q[2]
						+ " , " + "QUATERNION COMPONENT 3 : "
						+ mavAttPosMocap.q[3] + " , "
						+ "QUATERNION COMPONENT 4 : " + mavAttPosMocap.q[4]
						+ " , " + "X : " + mavAttPosMocap.x + "metres , "
						+ "Y : " + mavAttPosMocap.y + "metres , " + "Z : "
						+ mavAttPosMocap.z + "metres";
				Map<String, Object> tempMavAttPosMocap = Maps.newHashMap();
				tempMavAttPosMocap.put("data", tempAttPosMocap);
				sendOutputJson(publishers[2], tempMavAttPosMocap);
				getLog().info(tempAttPosMocap);
			}
			break;

		case msg_set_actuator_control_target.MAVLINK_MSG_ID_SET_ACTUATOR_CONTROL_TARGET:

			break;

		case msg_actuator_control_target.MAVLINK_MSG_ID_ACTUATOR_CONTROL_TARGET:
			/**
			 * Actuator controls. Normed to -1..+1 where 0 is neutral position.
			 * Throttle for single rotation direction motors is 0..1, negative
			 * range for reverse direction. Standard mapping for attitude
			 * controls (group 0): (index 0-7): roll, pitch, yaw, throttle,
			 * flaps, spoilers, airbrakes, landing gear. Load a pass-through
			 * mixer to repurpose them as generic outputs.
			 */
			msg_actuator_control_target mavActuatorControlTarget;
			if (mavMessage2 instanceof msg_actuator_control_target) 
			{
				mavActuatorControlTarget = (msg_actuator_control_target) mavMessage2;
				String[] actuatorMapping = { "ROLL", "PITCH", "YAW",
						"THROTTLE", "FLAPS", "SPOILERS", "AIRBRAKES",
						"LANDING GEAR" };
				String tempActuatorControlTarget = "["
						+ mavActuatorControlTarget.time_usec + " ] , "
						+ actuatorMapping[0] + "CONTROL : "
						+ mavActuatorControlTarget.controls[0] + " , "
						+ actuatorMapping[1] + "CONTROL : "
						+ mavActuatorControlTarget.controls[1] + " , "
						+ actuatorMapping[2] + "CONTROL : "
						+ mavActuatorControlTarget.controls[2] + " , "
						+ actuatorMapping[3] + "CONTROL : "
						+ mavActuatorControlTarget.controls[3] + " , "
						+ actuatorMapping[4] + "CONTROL : "
						+ mavActuatorControlTarget.controls[4] + " , "
						+ actuatorMapping[5] + "CONTROL : "
						+ mavActuatorControlTarget.controls[5] + " , "
						+ actuatorMapping[6] + "CONTROL : "
						+ mavActuatorControlTarget.controls[6] + " , "
						+ actuatorMapping[7] + "CONTROL : "
						+ mavActuatorControlTarget.controls[7] + " , "
						+ "GROUP MIX : " + mavActuatorControlTarget.group_mlx;
				Map<String, Object> tempMavActuatorControlTarget = Maps
						.newHashMap();
				tempMavActuatorControlTarget.put("data",
						tempActuatorControlTarget);
				sendOutputJson(publishers[2], tempMavActuatorControlTarget);
				getLog().info(tempActuatorControlTarget);
			}
			break;

		case msg_battery_status.MAVLINK_MSG_ID_BATTERY_STATUS:
			msg_battery_status mavBatteryStatus;
			if (mavMessage2 instanceof msg_battery_status) 
			{
				mavBatteryStatus = (msg_battery_status) mavMessage2;
				String tempBatteryStatus = "CHARGE CONSUMED : "
						+ mavBatteryStatus.current_consumed + " mAh , "
						+ "ENERGY CONSUMED : "
						+ mavBatteryStatus.energy_consumed / 100.0
						+ "Joules , " + "TEMPERATURE : "
						+ mavBatteryStatus.temperature + "degree Celsius , "
						+ "VOLTAGES : "
						+ Arrays.toString(mavBatteryStatus.voltages) + "mV , "
						+ "BATTERY CURRENT : "
						+ mavBatteryStatus.current_battery / 10.0 + "mA , "
						+ "BATTERY ID : " + mavBatteryStatus.id
						+ "BATTERY FUNCTION : "
						+ mavBatteryStatus.battery_function + "BATTERY TYPE : "
						+ mavBatteryStatus.type + "REMAINING BATTERY : "
						+ mavBatteryStatus.battery_remaining + "%";
				Map<String, Object> tempMavBatteryStatus = Maps.newHashMap();
				tempMavBatteryStatus.put("data" , tempBatteryStatus);
				sendOutputJson(publishers[2], tempMavBatteryStatus);
				getLog().info(tempBatteryStatus);
			}
			break;

		case msg_autopilot_version.MAVLINK_MSG_ID_AUTOPILOT_VERSION:
			Map<String,	Object> tempMavAutopilotVersion= Maps.newHashMap();
			tempMavAutopilotVersion.put("status", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavAutopilotVersion);
			getLog().info(mavMessage2.toString());
			break;

		case msg_landing_target.MAVLINK_MSG_ID_LANDING_TARGET:
			msg_landing_target mavLandingTarget;
			if (mavMessage2 instanceof msg_landing_target) 
			{
				mavLandingTarget = (msg_landing_target) mavMessage2;
				String tempLandingTarget = "ANGLE X : "
						+ mavLandingTarget.angle_x + "rad , " + "ANGLE Y : "
						+ mavLandingTarget.angle_y + "rad , " + "DISTANCE : "
						+ mavLandingTarget.distance + "metres , "
						+ "TARGET ID : " + mavLandingTarget.target_num + " , "
						+ "FRAME : "
						+ getVariableName("MAV_FRAME", mavLandingTarget.frame);
				Map<String, Object> tempMavLandingTarget = Maps.newHashMap();
				tempMavLandingTarget.put("data", tempLandingTarget);
				sendOutputJson(publishers[2], tempMavLandingTarget);
				getLog().info(tempLandingTarget);
			}
			break;

		case msg_v2_extension.MAVLINK_MSG_ID_V2_EXTENSION:

			break;

		case msg_memory_vect.MAVLINK_MSG_ID_MEMORY_VECT:

			break;

		case msg_debug_vect.MAVLINK_MSG_ID_DEBUG_VECT:

			break;

		case msg_named_value_float.MAVLINK_MSG_ID_NAMED_VALUE_FLOAT:
			Map<String,	Object> tempMavNamedValueFloat= Maps.newHashMap();
			tempMavNamedValueFloat.put("data", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavNamedValueFloat);
			getLog().info(mavMessage2.toString());
			break;

		case msg_named_value_int.MAVLINK_MSG_ID_NAMED_VALUE_INT:
			Map<String,	Object> tempMavNamedValueInt= Maps.newHashMap();
			tempMavNamedValueInt.put("data", mavMessage2.toString());
			sendOutputJson(publishers[2], tempMavNamedValueInt);
			getLog().info(mavMessage2.toString());
			break;

		case msg_statustext.MAVLINK_MSG_ID_STATUSTEXT:

			break;

		case msg_debug.MAVLINK_MSG_ID_DEBUG:

			break;

		default:
			break;
		}
		
	}
}
