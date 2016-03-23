package largeflow.datatype;

@Deprecated
public class FutureInfoPacket extends Packet {
	
	public Double nextPacketTime;
	
	public FutureInfoPacket(FlowId flowId, Integer packetSize, Double arriveTime){
		super(flowId, packetSize, arriveTime);
		nextPacketTime = null;
	}
	
	public FutureInfoPacket(FlowId flowId, Integer packetSize, Double arriveTime, Double nextPacketTime){
		super(flowId, packetSize, arriveTime);
		this.nextPacketTime = nextPacketTime;
	}
	
	@Override
	public String toString() {
		return flowId + ", " + size + ", " + time + ", (next: " + nextPacketTime + ")" ; 
	}
	
	public boolean hasNextPacket() {
		if (nextPacketTime == null) {
			return false;
		}
		
		return true;
	}
	
}
