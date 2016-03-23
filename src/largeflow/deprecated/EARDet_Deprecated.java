package largeflow.deprecated;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

@Deprecated
public class EARDet_Deprecated {
	
	//parameter to set
	private Integer alpha;			//max packet size, Byte
	private Integer numOfCounters;
	private Integer beta_th;		//Byte
	private Integer linkCapacity; 	//Byte / sec
	private Integer maxVirtualPacketSize;	//Byte   e.g. = beta_th - 1
	private Integer curVirtualFlowIdValue;
	private Double maxIncubationTime;	//sec
	
	//EARDet configs to read
	private Integer beta_h;			//Byte
	private Integer beta_l;			//Byte
	private Integer gamma_h;		//Byte / sec
	private Integer gamma_l;		//Byte / sec
	
	private Double currentTime;		//sec
	private List<Integer> counterList;
	private Map<FlowId, Integer> flowToCounterMap;
	private Map<Integer, FlowId> counterToFlowMap;
	private Map<FlowId, Double> blackList; 	//flow ID in blacklist and the time it got caught
	
	public EARDet_Deprecated(){
		alpha = 0;
		numOfCounters = 0;
		beta_th = 0;
		gamma_h = 0;
		gamma_l = 0;
		beta_h = 0;
		beta_l = 0;
		linkCapacity = 0;
		curVirtualFlowIdValue = 0;
		maxIncubationTime = 0.;
		
		currentTime = 0.;
		flowToCounterMap = new TreeMap<>();
		counterToFlowMap = new HashMap<>();
		blackList = new TreeMap<>();
	}
	
	public void setAlpha(Integer alpha){
		this.alpha = alpha;
	}
	
	public void setNumOfCounters(Integer numOfCounters){
		this.numOfCounters = numOfCounters;
		reset();
	}
	
	public void setBetaTh(Integer beta_th){
		this.beta_th = beta_th;
		maxVirtualPacketSize = beta_th - 1;
	}
	
	public void setLinkCapacity(Integer linkCapacity){
		this.linkCapacity = linkCapacity;
	}
	
	public void initEARDet(Integer alpha,
						   Integer beta_th,
						   Integer numOfCounters,
						   Integer linkCapacity){
		this.linkCapacity = linkCapacity;
		this.alpha = alpha;
		this.beta_th = beta_th;
		this.numOfCounters = numOfCounters;
		maxVirtualPacketSize = beta_th - 1;

		reset();
	}
	
	public void initEARDet(Integer alpha, 
						   Integer beta_l, 
						   Integer gamma_h, 
						   Integer gamma_l, 
						   Double maxIncubationTime,
						   Integer linkCapacity
						   ){
		this.beta_l = beta_l;
		this.gamma_h = gamma_h;
		this.gamma_l = gamma_l;
		this.alpha = alpha;
		this.maxIncubationTime = maxIncubationTime;
		this.linkCapacity = linkCapacity;
		
		
		double M = (double)gamma_h + (double)gamma_l - 2. * (double)(alpha + beta_l) / this.maxIncubationTime;
		double n = (double)linkCapacity / (M + Math.sqrt(M * M - (double)4 * (double)gamma_h * (double)gamma_l)) * 2. - 1.;
		
		numOfCounters = (int)n + 1;
		beta_th = (int)((double)beta_l + ((double)gamma_l * (double)(alpha + beta_l)) / ((double)linkCapacity / (double)(numOfCounters + 1) - (double)gamma_l)) + 1;
		beta_h = 2 * beta_th + alpha;
		maxVirtualPacketSize = beta_th - 1;
//		maxVirtualPacketSize = 5000;
		
		reset();

	}
	
	public void reset(){
		counterList = new ArrayList<>();
		for (int i = 0; i < numOfCounters; i++){
			counterList.add(0);
		}
		
		currentTime = 0.;
		curVirtualFlowIdValue = 0;
		flowToCounterMap = new HashMap<>();
		counterToFlowMap = new HashMap<>();
		blackList = new HashMap<>();
	}
	
	public Integer getAlpha() {
		return alpha;
	}
	
	public Integer getBetaH(){
		return beta_h;
	}
	
	public Integer getBetaL(){
		return beta_l;
	}
	
	public Integer getGammaH(){
		
		return gamma_h;
	}
	
	public Integer getGammaL(){
		return gamma_l;
	}
	
	public Integer getBetaTh(){
		return beta_th;
	}
	
	public Integer getNumOfCounters(){
		return numOfCounters;
	}
	
	
	public Double getBlackListTime(String flowId){
		// if flow id is not in the map, then return null
		return blackList.get(flowId);
	}	
	
	public Map<FlowId, Double> getBlackList(){
		return blackList;
	}
	
	public boolean processPacket(Packet packet){
				
		//if the flow is in blackList, then ignore
		if(blackList.containsKey(packet.flowId)){
			return false;
		}
		
		//process virtual flow
		int virtualTrafficSize = (int)((packet.time - currentTime) * linkCapacity) + 1;
		while(virtualTrafficSize >= maxVirtualPacketSize){
			virtualTrafficSize -= maxVirtualPacketSize;
			Packet virPacket = new Packet(getNextVirtualFlowId(), maxVirtualPacketSize, currentTime); //time doesnt matter here
			addPacketToCounter(virPacket);
		}
		
		if(virtualTrafficSize > 0){
			Packet virPacket = new Packet(getNextVirtualFlowId(), virtualTrafficSize, currentTime); //time doesnt matter here
			addPacketToCounter(virPacket);
		}
		
		//process this real packet
		int counterIndex = addPacketToCounter(packet);
		
		//
		if (counterIndex >= 0 && isCounterOverflow(counterIndex)){
			blackList.put(packet.flowId, packet.time + (double)packet.size / (double)linkCapacity);
			currentTime = packet.time + (double)packet.size / (double)linkCapacity;
			return false;
		}
		
		currentTime = packet.time + (double)packet.size / (double)linkCapacity;
		
		return true;
	}
	
	private Integer addPacketToCounter(Packet packet){
		//return the index of the counter associated to the packet, -1 if no counter associated to.
		
		//if the flow is associated with counter, just increase this counter
		if(flowToCounterMap.containsKey(packet.flowId)){
			int counterIndex = flowToCounterMap.get(packet.flowId);
			counterList.set(counterIndex, counterList.get(counterIndex) + packet.size);
			return counterIndex;
		} else{ //if the flow is new to counter
			//find the counter with min value
			int minIndex = 0;
			int minValue = counterList.get(minIndex);
			
			for(int i = 1; i < numOfCounters; i++){
				int curValue = counterList.get(i);
				if(curValue < minValue){
					minIndex = i;
					minValue = curValue;
				}
			}
			
			if(minValue == 0){
				flowToCounterMap.put(packet.flowId, minIndex);
				counterToFlowMap.put(minIndex, packet.flowId);
				counterList.set(minIndex, packet.size);
				return minIndex;
			} else if(minValue > 0){
				int valueLeft = packet.size - minValue;
				if(valueLeft > 0){
					decreaseAllCountersBy(minValue);
					
					//update the counter
					counterList.set(minIndex, valueLeft);
					
					//update the counter flow maps
					FlowId preFlowId = counterToFlowMap.get(minIndex);
					flowToCounterMap.remove(preFlowId);
					flowToCounterMap.put(packet.flowId, minIndex);
					counterToFlowMap.put(minIndex, packet.flowId);
					
					return minIndex;
				} else if (valueLeft < 0){
					decreaseAllCountersBy(packet.size);
					return -1;
				} else{
					decreaseAllCountersBy(packet.size);
					FlowId preFlowId = counterToFlowMap.get(minIndex);
					flowToCounterMap.remove(preFlowId);
					counterToFlowMap.remove(minIndex);
					
					return -1;
				}
				
			}
			
		}
		
		
		return -1;
	}
	
	private boolean isCounterOverflow(Integer counterIndex){
		if(counterList.get(counterIndex) > beta_th){
			return true;
		}
		
		return false;
	}
	
	private FlowId getNextVirtualFlowId() {
		if (curVirtualFlowIdValue == FlowId.MAX_VALUE) {
			curVirtualFlowIdValue = 1;
		}
		return new FlowId(++curVirtualFlowIdValue, true);
	}
	
	private void decreaseAllCountersBy(Integer valueToDecrease){
		for(int i = 0; i < numOfCounters; i++){
			counterList.set(i, counterList.get(i) - valueToDecrease);
		}
	}
	
	
	
}
