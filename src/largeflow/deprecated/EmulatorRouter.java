package largeflow.deprecated;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.PacketReader;

@Deprecated
public class EmulatorRouter {
	
	private List<Detector> detectorsToRunList;
	private PacketReader packetReader;
	
	public EmulatorRouter(){
		detectorsToRunList = new ArrayList<>();
	}
	
	public EmulatorRouter(PacketReader packetReader){
		detectorsToRunList = new ArrayList<>();
		this.packetReader = packetReader;
	}
	
	public void addDetector(Detector detector){
		detectorsToRunList.add(detector);
	}
	
	public void setPacketReader(PacketReader packetReader){
		this.packetReader = packetReader;
	}
	
	public void reset() throws IOException{
		packetReader.rewind();
		for(Detector detector : detectorsToRunList){
			detector.reset();
		}
	}
	
	public void run() throws Exception{
		reset();
		
		if(detectorsToRunList.isEmpty()){
			throw new Exception("Please add large flow detector into the emulator");
		}
		
		if(packetReader == null){
			throw new Exception("Please set the packet reader");
		}
				
		Packet packet;
		while((packet = packetReader.getNextPacket()) != null){
			for(Detector detector : detectorsToRunList){
				detector.processPacket(packet);
			}
		}
		
		packetReader.close();
	}
	
}
