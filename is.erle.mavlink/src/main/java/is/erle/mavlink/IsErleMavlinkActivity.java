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
import java.lang.Class;
import java.lang.reflect.Field;
/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleMavlinkActivity extends BaseRoutableRosActivity {

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
		Map<String, Object> temp = Maps.newHashMap();
		temp.put("mission", "START");
		//sendOutputJson(getConfiguration().getRequiredPropertyString(CONFIGURATION_PUBLISHER_NAME), temp);
		//sendOutputJson("outputCOM_M", temp);
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
    			tempMapMission.put("mission", tempByte);
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
				tempMapMission.put("mission", tempByte);
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

			break;

		case msg_image_trigger_control.MAVLINK_MSG_ID_IMAGE_TRIGGER_CONTROL:

			break;

		case msg_image_available.MAVLINK_MSG_ID_IMAGE_AVAILABLE:

			break;

		case msg_set_position_control_offset.MAVLINK_MSG_ID_SET_POSITION_CONTROL_OFFSET:

			break;

		case msg_position_control_setpoint.MAVLINK_MSG_ID_POSITION_CONTROL_SETPOINT:

			break;

		case msg_marker.MAVLINK_MSG_ID_MARKER:

			break;

		case msg_raw_aux.MAVLINK_MSG_ID_RAW_AUX:

			break;

		case msg_watchdog_heartbeat.MAVLINK_MSG_ID_WATCHDOG_HEARTBEAT:

			break;

		case msg_watchdog_process_info.MAVLINK_MSG_ID_WATCHDOG_PROCESS_INFO:

			break;

		case msg_watchdog_process_status.MAVLINK_MSG_ID_WATCHDOG_PROCESS_STATUS:

			break;

		case msg_watchdog_command.MAVLINK_MSG_ID_WATCHDOG_COMMAND:

			break;

		case msg_pattern_detected.MAVLINK_MSG_ID_PATTERN_DETECTED:

			break;

		case msg_point_of_interest.MAVLINK_MSG_ID_POINT_OF_INTEREST:

			break;

		case msg_point_of_interest_connection.MAVLINK_MSG_ID_POINT_OF_INTEREST_CONNECTION:

			break;

		case msg_brief_feature.MAVLINK_MSG_ID_BRIEF_FEATURE:

			break;

		case msg_attitude_control.MAVLINK_MSG_ID_ATTITUDE_CONTROL:

			break;

		case msg_detection_stats.MAVLINK_MSG_ID_DETECTION_STATS:

			break;

		case msg_onboard_health.MAVLINK_MSG_ID_ONBOARD_HEALTH:

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
				tempMavHeartbeat.put("gps", tempHeartbeat);
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
				mavGps = (msg_gps_raw_int) mavMessage2 ;
				Map<String, Object> tempMavGps = Maps.newHashMap();
				String tempGps = "[" + mavGps.time_usec + "] ," + "LATITUDE : "
						+ mavGps.lat/10000000.0 + "degrees , " + "LONGITUDE : " + mavGps.lon/10000000.0 + "degrees , "
						+ "ALTITUDE : " + mavGps.alt/1000.0 + "metres , "
						+ "HORIZONTAL DILUTION : " + mavGps.eph/100.0 + "metres , "
						+ "VERTICAL DILUTION : " + mavGps.epv/100.0 + "metres , "
						+ "VELOCITY : " + mavGps.vel/100.0 + "m/s , "
						+ "COURSE OVER GROUND : " + mavGps.cog/100.0 + "degrees , "
						+ "FIX TYPE : " + mavGps.fix_type + "D , "
						+ "SATELLITES VISIBLE : " + mavGps.satellites_visible;
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
				Map<String , Object> tempMavScaledImu = Maps.newHashMap();
				tempMavScaledImu.put("data", tempScaledImu);
				sendOutputJson(publishers[2], tempMavScaledImu);
				getLog().info(tempScaledImu);
			}
			break;

		case msg_raw_imu.MAVLINK_MSG_ID_RAW_IMU:
			msg_raw_imu mavRawImu;
			if (mavMessage2 instanceof msg_raw_imu) 
			{
				mavRawImu = (msg_raw_imu) mavMessage2 ;
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
				Map<String , Object> tempMavRawImu = Maps.newHashMap();
				tempMavRawImu.put("data", tempRawImu);
				sendOutputJson(publishers[2], tempMavRawImu);
				getLog().info(tempRawImu);
			}
			break;

		case msg_raw_pressure.MAVLINK_MSG_ID_RAW_PRESSURE:
			msg_raw_pressure mavRawPressure;
			if (mavMessage2 instanceof msg_raw_pressure) 
			{
				mavRawPressure = (msg_raw_pressure) mavMessage2 ;
				
			}
			break;

		case msg_scaled_pressure.MAVLINK_MSG_ID_SCALED_PRESSURE:
			msg_scaled_pressure  mavScaledPressure;
			if (mavMessage2 instanceof msg_scaled_pressure) 
			{
				mavScaledPressure = (msg_scaled_pressure) mavMessage2 ;
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
				Map<String , Object> tempmavAttitude = Maps.newHashMap();
				tempmavAttitude.put("data", tempAttitude);
				sendOutputJson(publishers[2], tempmavAttitude);
				getLog().info(tempAttitude);
			}
			break;

		case msg_attitude_quaternion.MAVLINK_MSG_ID_ATTITUDE_QUATERNION:
			msg_attitude_quaternion  mavAttitudeQuaternion;
			if (mavMessage2 instanceof msg_attitude_quaternion) 
			{
				mavAttitudeQuaternion = (msg_attitude_quaternion) mavMessage2 ;
			}
			break;

		case msg_local_position_ned.MAVLINK_MSG_ID_LOCAL_POSITION_NED:

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

			break;

		case msg_rc_channels_raw.MAVLINK_MSG_ID_RC_CHANNELS_RAW:

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

			break;

		case msg_nav_controller_output.MAVLINK_MSG_ID_NAV_CONTROLLER_OUTPUT:

			break;

		case msg_global_position_int_cov.MAVLINK_MSG_ID_GLOBAL_POSITION_INT_COV:

			break;

		case msg_local_position_ned_cov.MAVLINK_MSG_ID_LOCAL_POSITION_NED_COV:

			break;

		case msg_rc_channels.MAVLINK_MSG_ID_RC_CHANNELS:

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

			break;

		case msg_set_position_target_local_ned.MAVLINK_MSG_ID_SET_POSITION_TARGET_LOCAL_NED:

			break;

		case msg_position_target_local_ned.MAVLINK_MSG_ID_POSITION_TARGET_LOCAL_NED:

			break;

		case msg_set_position_target_global_int.MAVLINK_MSG_ID_SET_POSITION_TARGET_GLOBAL_INT:

			break;

		case msg_position_target_global_int.MAVLINK_MSG_ID_POSITION_TARGET_GLOBAL_INT:

			break;

		case msg_local_position_ned_system_global_offset.MAVLINK_MSG_ID_LOCAL_POSITION_NED_SYSTEM_GLOBAL_OFFSET:

			break;

		case msg_hil_state.MAVLINK_MSG_ID_HIL_STATE:

			break;

		case msg_hil_controls.MAVLINK_MSG_ID_HIL_CONTROLS:

			break;

		case msg_hil_rc_inputs_raw.MAVLINK_MSG_ID_HIL_RC_INPUTS_RAW:

			break;

		case msg_optical_flow.MAVLINK_MSG_ID_OPTICAL_FLOW:

			break;

		case msg_global_vision_position_estimate.MAVLINK_MSG_ID_GLOBAL_VISION_POSITION_ESTIMATE:

			break;

		case msg_vision_position_estimate.MAVLINK_MSG_ID_VISION_POSITION_ESTIMATE:

			break;

		case msg_vision_speed_estimate.MAVLINK_MSG_ID_VISION_SPEED_ESTIMATE:

			break;

		case msg_vicon_position_estimate.MAVLINK_MSG_ID_VICON_POSITION_ESTIMATE:

			break;

		case msg_highres_imu.MAVLINK_MSG_ID_HIGHRES_IMU:

			break;

		case msg_optical_flow_rad.MAVLINK_MSG_ID_OPTICAL_FLOW_RAD:

			break;

		case msg_hil_sensor.MAVLINK_MSG_ID_HIL_SENSOR:

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

			break;

		case msg_hil_optical_flow.MAVLINK_MSG_ID_HIL_OPTICAL_FLOW:

			break;

		case msg_hil_state_quaternion.MAVLINK_MSG_ID_HIL_STATE_QUATERNION:

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

			break;

		case msg_power_status.MAVLINK_MSG_ID_POWER_STATUS:

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

			break;

		case msg_att_pos_mocap.MAVLINK_MSG_ID_ATT_POS_MOCAP:

			break;

		case msg_set_actuator_control_target.MAVLINK_MSG_ID_SET_ACTUATOR_CONTROL_TARGET:

			break;

		case msg_actuator_control_target.MAVLINK_MSG_ID_ACTUATOR_CONTROL_TARGET:

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

			break;

		case msg_landing_target.MAVLINK_MSG_ID_LANDING_TARGET:

			break;

		case msg_v2_extension.MAVLINK_MSG_ID_V2_EXTENSION:

			break;

		case msg_memory_vect.MAVLINK_MSG_ID_MEMORY_VECT:

			break;

		case msg_debug_vect.MAVLINK_MSG_ID_DEBUG_VECT:

			break;

		case msg_named_value_float.MAVLINK_MSG_ID_NAMED_VALUE_FLOAT:

			break;

		case msg_named_value_int.MAVLINK_MSG_ID_NAMED_VALUE_INT:

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
