package is.erle.comm.serial;

import java.util.Map;

import com.google.common.collect.Maps;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.service.comm.serial.SerialCommunicationEndpoint;
import interactivespaces.service.comm.serial.SerialCommunicationEndpointService;
import interactivespaces.util.concurrency.CancellableLoop;
import interactivespaces.util.resource.ManagedResourceWithTask;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class IsErleCommSerialActivity extends BaseRoutableRosActivity {

	private static long jsonOutputCounter = 0;
	private static long jsonInputCounter = 0 ;
	
	private SerialCommunicationEndpoint serial;
	private byte [] serialData ;
	
    @Override
    public void onActivitySetup() {
        getLog().info("Activity is.erle.comm.serial setup");
        
        SerialCommunicationEndpointService serialService= getSpaceEnvironment().getServiceRegistry().getRequiredService(SerialCommunicationEndpointService.SERVICE_NAME);
        String portName = getConfiguration().getRequiredPropertyString("space.hardware.serial.port");
        serial = serialService.newSerialEndpoint(portName);
        serial.setBaud(115200);
        serial.setInputBufferSize(5000);
        serial.setOutputBufferSize(1000);
        
        ManagedResourceWithTask serialTask = new ManagedResourceWithTask(serial, new CancellableLoop() {
			
			@Override
			protected void loop() throws InterruptedException {
				handleSerialInput();
			}
			protected void handleException(Exception e)
			{
				getLog().error("Error " +e);
			}
		}, getSpaceEnvironment());
        addManagedResource(serialTask);
    }

    @Override
    public void onActivityStartup() {
        getLog().info("Activity is.erle.comm.serial startup");
    }

    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity is.erle.comm.serial post startup");
    }

    @Override
    public void onActivityActivate() {
        getLog().info("Activity is.erle.comm.serial activate");
        jsonOutputCounter = 0;
        Map<String,Object> temp=Maps.newHashMap();
        temp.put(Long.toString(jsonOutputCounter++), "ACTIVATE");
        sendOutputJson("output", temp);
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity is.erle.comm.serial deactivate");
        Map<String,Object> temp=Maps.newHashMap();
        temp.put(Long.toString(jsonOutputCounter), "DEACTIVATE");
        sendOutputJson("output", temp);
    }

    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity is.erle.comm.serial pre shutdown");
    }

    @Override
    public void onActivityShutdown() {
        getLog().info("Activity is.erle.comm.serial shutdown");
    }

    @Override
    public void onActivityCleanup() {
        getLog().info("Activity is.erle.comm.serial cleanup");
    }
    
    @Override
    public void onNewInputJson(String channelName, Map <String , Object> message)
    {
    	String temp = message.get(Long.toString(jsonInputCounter)).toString();
    	serial.write(temp.getBytes());
    	jsonInputCounter++;
    }
    
    private void handleSerialInput()
    {
		serialData =new byte[1000];
		serial.read(serialData);
        Map<String,Object> temp=Maps.newHashMap();
        temp.put("comm", serialData);
        sendOutputJson("output", temp);
    }
}
