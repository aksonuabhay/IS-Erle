package is.erle.comms;

import java.net.InetSocketAddress;

import interactivespaces.activity.impl.BaseActivity;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpoint;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpointListener;
import interactivespaces.service.comm.network.client.UdpClientNetworkCommunicationEndpointService;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleCommsActivity extends BaseActivity {

	private static final String CONFIGURATION_SERVER_HOST = "space.comm.udp.server.host";
	private static final String CONFIGURATION_SERVER_PORT = "space.comm.udp.server.port";
	
	
	
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
				// TODO Auto-generated method stub
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
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.comms deactivate");
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
    
    protected void handleUdpDroneResponse(byte[] response,
			InetSocketAddress address) {
		// TODO Auto-generated method stub
		
	}
}
