package largeflow.datatype;

/**
 * Stateless packet structure, only keep the current packet info.
 * @author HaoWu
 *
 */
public class Packet {
	
	public FlowId flowId;
	public Integer size;
	public Double time;
	
	public Packet(FlowId flowId, Integer packetSize, Double arriveTime){
		this.flowId = flowId;
		size = packetSize;
		time = arriveTime;
	}
	
	@Override
	public String toString() {
		return flowId + ", " + size + ", " + time; 
	}
	
}
