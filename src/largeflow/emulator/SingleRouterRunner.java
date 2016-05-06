package largeflow.emulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import largeflow.datatype.Packet;

public class SingleRouterRunner {
	
	private List<Router> routersToRunList;
	private PacketReader packetReader;
	private Detector baseDetector;
	
	public SingleRouterRunner(){
		routersToRunList = new ArrayList<>();
	}
	
	public SingleRouterRunner(PacketReader packetReader){
		routersToRunList = new ArrayList<>();
		this.packetReader = packetReader;
	}
	
	public void addRouter(Router router){
		routersToRunList.add(router);
	}
	
	public void setBaseDetector(Detector baseDetector) {
	    this.baseDetector = baseDetector;
	}
	
	public void setPacketReader(PacketReader packetReader){
		this.packetReader = packetReader;
	}
	
	public void reset() throws IOException{
		packetReader.rewind();
		baseDetector.reset();
		for(Router router : routersToRunList){
			router.reset();
		}
	}
	
	public void run() throws Exception{
	    if (baseDetector == null) {
            throw new Exception("Please set base detector in the router runner");
        }

        if (routersToRunList.isEmpty()) {
            throw new Exception("No router to run, please add router(s)");
        }
	    		
		if(packetReader == null){
			throw new Exception("Please set the packet reader");
		}
		
	    reset();
				
		Packet packet;
		while((packet = packetReader.getNextPacket()) != null){
		    baseDetector.processPacket(packet);
			for(Router router : routersToRunList){
				router.processPacket(packet);
			}
		}
		
		for(Router router : routersToRunList){
            router.processEnd();
        }
		
		packetReader.close();
	}	
}
