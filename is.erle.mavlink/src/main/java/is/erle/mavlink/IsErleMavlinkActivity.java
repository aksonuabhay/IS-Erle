package is.erle.mavlink;

import java.util.Map;


import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;

import com.MAVLink.*;
import com.MAVLink.Messages.MAVLinkMessage;
import com.google.common.collect.Maps;
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
		sendOutputJson("outputCOM_M", temp);
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
    	}
    	
    	else if (channelName == subscribers[2])
    	{
    		//Captain message handling here
    	}
    }

	private void hadnleMavMessage(MAVLinkMessage mavMessage2) 
	{
		//To Do
		switch (mavPacket.msgid) {
		case value:
			
			break;

		default:
			break;
		}
		
	}
}
