package is.erle.comms;

import java.net.InetSocketAddress;
import java.util.Map;

import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpoint;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpointListener;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpointService;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleCommsActivity extends BaseRoutableRosActivity {

	private static final String CONFIGURATION_SERVER_HOST = "space.comm.udp.server.host";
	private static final String CONFIGURATION_SERVER_PORT = "space.comm.udp.server.port";
	
//	private static final String CONFIGURATION_CHANNEL_NAME = "space.activity.route.output.output";
	
	private static long jsonOutputCounter = 0;
	private static long jsonInputCounter = 0 ;
	
	private UdpClientNetworkCommunicationEndpoint udpDroneClient;
	private InetSocketAddress udpDroneServerAddress;
	
	
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.comms setup");
        UdpClientNetworkCommunicationEndpointService udpDroneClientService = 
        		getSpaceEnvironment().getServiceRegistry().getRequiredService(UdpClientNetworkCommunicationEndpointService.SERVICE_NAME);
        
        String udpDroneServerHost = getConfiguration().getRequiredPropertyString(CONFIGURATION_SERVER_HOST);
        int udpDroneServerPort = getConfiguration().getRequiredPropertyInteger(CONFIGURATION_SERVER_PORT);
        
        udpDroneClient = udpDroneClientService.newClient(getLog());
        udpDroneClient.addListener(new UdpClientNetworkCommunicationEndpointListener() {
			
			public void onUdpResponse(UdpClientNetworkCommunicationEndpoint arg0,
					byte[] response, InetSocketAddress address) {
				handleUdpDroneResponse(response , address);
			}
		});
        addManagedResource(udpDroneClient);
        
        udpDroneServerAddress = new InetSocketAddress(udpDroneServerHost, udpDroneServerPort);
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
        jsonOutputCounter = 0;
//        Map<String,Object> temp=Maps.newHashMap();
//        temp.put(Long.toString(jsonOutputCounter++), "ACTIVATE");
//        sendOutputJson("output", temp);
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.comms deactivate");
//        Map<String,Object> temp=Maps.newHashMap();
//        temp.put(Long.toString(jsonOutputCounter), "DEACTIVATE");
//        sendOutputJson("output", temp);
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
    
    @Override
    public void onNewInputJson(String channelName, Map <String , Object> message)
    {
    	byte [] temp = (byte[]) message.get("comm");
    	udpDroneClient.write(udpDroneServerAddress, temp);
    	jsonInputCounter++;
    }
    
    protected void handleUdpDroneResponse(byte[] response,
			InetSocketAddress address) {
        Map<String,Object> temp=Maps.newHashMap();
        temp.put("comm", response);
        sendOutputJson("output", temp);
		
	}
}
