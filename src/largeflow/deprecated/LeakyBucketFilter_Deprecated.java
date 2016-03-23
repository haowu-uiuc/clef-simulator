package largeflow.deprecated;
import java.util.Map;
import java.util.TreeMap;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

@Deprecated
public class LeakyBucketFilter_Deprecated {

	//parameter to set
	private Integer burst_th;		//Byte
	private Integer linkCapacity; 	//Byte / sec
	private Integer rate_th;		//Byte / sec, the threshold of rate for large flow
	
	private Map<FlowId, Integer> flowToBucketMap;
	private Map<FlowId, Double> flowToLastPacketEndTimeMap;
	private Map<FlowId, Double> blackList; 	//flow ID in blacklist and the time it got caught
	
	public Double getBlackListTime(String flowId){
		// if flow id is not in the map, then return null
		return blackList.get(flowId);
	}	
	
	public Map<FlowId, Double> getBlackList(){
		return blackList;
	}
	
	public LeakyBucketFilter_Deprecated(Integer burst_th, Integer rate_th, Integer linkCapacity){
		this.burst_th = burst_th;
		this.rate_th = rate_th;
		this.linkCapacity = linkCapacity;
		
		flowToBucketMap = new TreeMap<>();
		flowToLastPacketEndTimeMap = new TreeMap<>();
		blackList = new TreeMap<>();
	}
	
	public void processPacket(Packet packet){
		if(blackList.containsKey(packet.flowId)){
			return;
		}
		
		double lastPacketEndTime = packet.time + (double)packet.size / (double)linkCapacity;
		int curDecrement = (int)((double)packet.size / (double)linkCapacity * (double)rate_th + 0.5);
		
		if(!flowToBucketMap.containsKey(packet.flowId)){
			flowToBucketMap.put(packet.flowId, packet.size - curDecrement);
			flowToLastPacketEndTimeMap.put(packet.flowId, lastPacketEndTime);
		} else{
			int decrement = (int)((packet.time - flowToLastPacketEndTimeMap.get(packet.flowId)) * (double)rate_th + 0.5);
			int bucketValue = flowToBucketMap.get(packet.flowId) - decrement;
			if(bucketValue < 0){
				bucketValue = 0;
			}
						
			flowToBucketMap.put(packet.flowId, bucketValue + packet.size - curDecrement);
			flowToLastPacketEndTimeMap.put(packet.flowId, lastPacketEndTime);
			
			
		}
		
		if(flowToBucketMap.get(packet.flowId) > burst_th){
			//put into blacklist
			blackList.put(packet.flowId, lastPacketEndTime);
			flowToBucketMap.remove(packet.flowId);
			flowToLastPacketEndTimeMap.remove(packet.flowId);
		}
		
	}
	
}
