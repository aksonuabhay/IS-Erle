package is.erle.comms;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpointService;
import interactivespaces.service.comm.network.server.UdpServerNetworkCommunicationEndpoint;
import interactivespaces.service.comm.network.server.UdpServerNetworkCommunicationEndpointListener;
import interactivespaces.service.comm.network.server.UdpServerNetworkCommunicationEndpointService;
import interactivespaces.service.comm.network.server.UdpServerRequest;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleCommsActivity extends BaseRoutableRosActivity {

	private static final String CONFIGURATION_SERVER_PORT = "space.comm.udp.server.port";
	
//	private static final String CONFIGURATION_CHANNEL_NAME = "space.activity.route.output.output";
	
	private static long jsonOutputCounter = 0;
	private static long jsonInputCounter = 0 ;
	
	private InetSocketAddress udpDroneAddress;
	private boolean droneAddressFlag,sendFlag;
	private UdpServerNetworkCommunicationEndpoint udpDroneServer ;
	private byte [] responseGlobal;
	 
    @Override
	public void onActivitySetup()
	{
		getLog().info("Activity is.erle.comms setup");
		droneAddressFlag = false;
		sendFlag = false;
		responseGlobal = new byte[256];

		UdpServerNetworkCommunicationEndpointService udpServerService = getSpaceEnvironment()
				.getServiceRegistry()
				.getRequiredService(
						UdpServerNetworkCommunicationEndpointService.SERVICE_NAME);
		int port = getConfiguration().getRequiredPropertyInteger(
				CONFIGURATION_SERVER_PORT);
		udpDroneServer = udpServerService.newServer(port, getLog());
		udpDroneServer
				.addListener(new UdpServerNetworkCommunicationEndpointListener()
				{

					public void onUdpRequest(
							UdpServerNetworkCommunicationEndpoint server,
							UdpServerRequest req)
					{
						handleUdpDroneServerResponse(req.getRequest(), server);
						// getLog().info(req.getRemoteAddress()
						// +Arrays.toString(req.getRequest()));
						// req.writeResponse("Server recieved your message and is replying".getBytes());
						// getLog().info(udpDroneAddress+"  " +
						// droneAddressFlag);
						if (!droneAddressFlag)
						{
							// udpDroneAddress = req.getRemoteAddress();
							udpDroneAddress = new InetSocketAddress(req
									.getRemoteAddress().getHostString(), 6000);
							req.writeResponse("sh /etc/init.d/rc.usb"
									.getBytes());
							droneAddressFlag = true;
						}
						if (sendFlag)
						{
							req.writeResponse(responseGlobal);
							synchronized (this)
							{
								sendFlag = false;
							}
						}
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
	public void onNewInputJson(String channelName, Map<String, Object> message)
	{
		getLog().info("Sending to drone");
		byte[] response;
		String items[] = message.get("comm").toString().replaceAll("\\[", "")
				.replaceAll("\\]", "").replaceAll(" ", "").split(",");
		int lenItems = items.length;
		response = new byte[lenItems];
		for (int i = 0; i < lenItems; i++)
		{
			try
			{
				response[i] = Byte.parseByte(items[i]);
			}
			catch (NumberFormatException e)
			{
				getLog().error(e);
			}

		}
		if (droneAddressFlag)
		{
			synchronized (this)
			{
				System.arraycopy(response, 0, responseGlobal, 0, lenItems);
				sendFlag = true;
			}
		}
		else
		{
			getLog().info("No Drones connected now");
		}
		jsonInputCounter++; // Take care of this variable
	}

    protected void handleUdpDroneClientResponse(byte[] response,
			InetSocketAddress address) {
        Map<String,Object> temp=Maps.newHashMap();
        temp.put("comm", Arrays.toString(response));
        sendOutputJson("output", temp);
		
	}
    
    protected void handleUdpDroneServerResponse(byte[] response,
    		UdpServerNetworkCommunicationEndpoint address) {
        Map<String,Object> temp=Maps.newHashMap();
        temp.put("comm", Arrays.toString(response));
        sendOutputJson("output", temp);
		
	}
}
