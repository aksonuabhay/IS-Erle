package is.erle.comms;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpoint;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpointListener;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpointService;
import interactivespaces.service.comm.network.server.UdpServerNetworkCommunicationEndpoint;
import interactivespaces.service.comm.network.server.UdpServerNetworkCommunicationEndpointListener;
import interactivespaces.service.comm.network.server.UdpServerNetworkCommunicationEndpointService;
import interactivespaces.service.comm.network.server.UdpServerRequest;

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
	
	 UdpServerNetworkCommunicationEndpoint udpDroneServer ;
	 
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
        
        
        UdpServerNetworkCommunicationEndpointService udpServerService = getSpaceEnvironment().getServiceRegistry().getRequiredService(UdpServerNetworkCommunicationEndpointService.SERVICE_NAME);
        int port = getConfiguration().getRequiredPropertyInteger("space.comm.udp.server.port");
        udpDroneServer = udpServerService.newServer(port, getLog());
        udpDroneServer.addListener(new UdpServerNetworkCommunicationEndpointListener() {
			
			public void onUdpRequest(UdpServerNetworkCommunicationEndpoint server,
					UdpServerRequest req) {
				handleUdpDroneResponse(req.getRequest() , server);
//				getLog().info(req.getRemoteAddress() + "<- client sent server -> ");
//				req.writeResponse("Server recieved your message and is replying".getBytes());
			}
		});
       addManagedResource(udpDroneServer);
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
    	byte [] responseGlobal ;
		String items[] = message.get("comm").toString()
				.replaceAll("\\[", "").replaceAll("\\]", "")
				.replaceAll(" ", "").split(",");
		int lenItems = items.length;
		responseGlobal = new byte[lenItems];
    	for (int i = 0; i < lenItems; i++) {
    		try 
    		{
        		responseGlobal[i] = Byte.parseByte(items[i]);
			}
    		catch (NumberFormatException e) 
    		{
				getLog().error(e);
			}

		}
    	udpDroneClient.write(udpDroneServerAddress, responseGlobal);
		jsonInputCounter++; // Take care of this variable
    }
    
    protected void handleUdpDroneResponse(byte[] response,
			InetSocketAddress address) {
        Map<String,Object> temp=Maps.newHashMap();
        temp.put("comm", Arrays.toString(response));
        sendOutputJson("output", temp);
		
	}
    
    protected void handleUdpDroneResponse(byte[] response,
    		UdpServerNetworkCommunicationEndpoint address) {
        Map<String,Object> temp=Maps.newHashMap();
        temp.put("comm", Arrays.toString(response));
        sendOutputJson("output", temp);
		
	}
}
