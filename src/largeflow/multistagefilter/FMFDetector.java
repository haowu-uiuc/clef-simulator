package largeflow.multistagefilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import largeflow.datatype.FlowId;
import largeflow.datatype.FutureInfoPacket;
import largeflow.datatype.Packet;
import largeflow.emulator.Logger;

public class FMFDetector extends MultistageFilter {

	private Double T; // period T for periodic check
	private Integer threshold; // threshold of bucket
	private Integer currentPeriod; // current period number (= 0, 1, 2, ...)
	private boolean considerPacketCrossPeriods;
	
	public FMFDetector(String detectorName,
			Integer numOfStages,
			Integer sizeOfStage,
			Integer linkCapacity,
			Double T,
			Integer threshold) throws Exception {
		super(detectorName,
				numOfStages,
				sizeOfStage,
				linkCapacity);
		this.T = T;
		this.threshold = threshold;
		
		int maxPacketSize = 1518;
		if (T < maxPacketSize / linkCapacity) {
			throw new Exception("Period T has to be larger than maxPacketSize / linkCapacity .");
		}
		
		initState();
		
		// default to consider packet across periods.
		considerPacketCrossPeriods = true;
	}
	
	@Override
	public void reset() {
		super.reset();
		initState();
	}
	
	@Override
	public void setNumOfCounters(Integer numOfCounters) throws Exception {
		super.setNumOfCounters(numOfCounters);
		initState();
	}
	
	/**
	 * return false: the flow of the packet is already in the blacklist;
	 * return true: otherwise
	 */
	@Override
	public boolean processPacket(Packet packet) throws Exception {
		
		// check whether the flow is already in the blacklist
		if (blackList.containsKey(packet.flowId)) {
			 return false;
		}
		
		double packetStartTime = packet.time;
		double packetEndTime = packet.time + (double) packet.size / (double) linkCapacity;
		int startPeriod = (int) Math.floor(packetStartTime / T);
		int endPeriod = (int) Math.floor(packetEndTime / T);
		
		int lastPeriod = currentPeriod;
		currentPeriod = startPeriod;
		
		if (lastPeriod < currentPeriod) {
			// if currentPeriod is larger than lastPeriod,
			// check the buckets and reset buckets
			checkAllFlows((lastPeriod + 1) * T);
			resetBucketList();
			if (blackList.containsKey(packet.flowId)) {
				 return false;
			}
		}
		
		if (startPeriod == endPeriod || !considerPacketCrossPeriods) {
			// if the packet does not cross two periods
			// PS: if considerPacketCrossPeriods == true, 
			// then we will ignore the "else" branch anyway.
			processPacketForAllStages(packet);
			
		} else {
			// if the packet cross two periods
			// only add the amount of the packet in the current period for now;
			// check the buckets +  reset buckets;
			// and then add the other part of the packet into the next period;
			
			int nextPeriodPacketAmount = (int) Math.round(packet.size * 
					(packetEndTime - T * endPeriod) / (packetEndTime - packetStartTime));
			
			// add the amount of the packet in the current period
			Packet firstHalfPacket = new Packet(packet.flowId, 
					packet.size - nextPeriodPacketAmount, packet.time);				
			processPacketForAllStages(firstHalfPacket);
			
			// check the bucket value,
			// reset the bucket, and add nextPeriodPacketAmount
			checkAllFlows((currentPeriod + 1) * T);
			resetBucketList();
			
			currentPeriod = endPeriod;

			if (blackList.containsKey(packet.flowId)) {
				 return false;
			}
			
			// add the other part of the packet into the next period
			Packet secondHalfPacket = new Packet(packet.flowId, nextPeriodPacketAmount, packet.time);
			processPacketForAllStages(secondHalfPacket);									
		}

		return true;
	}
	

	/**
	 * return false: the flow of the packet is already in the blacklist;
	 * return true: otherwise
	 */
	@Deprecated
	public boolean processPacketUseNextPacketTime(FutureInfoPacket packet) throws Exception {

		boolean isInBlackList = false;
		// check whether the flow is already in the blacklist
		if (blackList.containsKey(packet.flowId)) {
			isInBlackList = true;
		}
		
		double packetStartTime = packet.time;
		double packetEndTime = packet.time + (double) packet.size / (double) linkCapacity;
		int startPeriod = (int) Math.floor(packetStartTime / T);
		int endPeriod = (int) Math.floor(packetEndTime / T);
		
		currentPeriod = startPeriod;
		
		int nextPeriodPacketAmount = 0;
		if (startPeriod == endPeriod || !considerPacketCrossPeriods) {
			// if the packet does not cross two periods
			// PS: if considerPacketCrossPeriods == true, 
			// then we will ignore the "else" branch anyway.
			if (!isInBlackList) {
				processPacketForAllStages(packet);
			}
			
			if (packet.nextPacketTime != null) {
				// if there is a following packet, get the period of next packet
				int nextPacketPeriod = (int) Math.floor(packet.nextPacketTime / T);
				
				if (nextPacketPeriod > currentPeriod) {
					// if the next packet happens in the next period,
					// then check the bucket value with the threshold,
					// and reset the buckets' value to zero					
					checkAllFlows((currentPeriod + 1) * T);
					resetBucketList();
				}
			} else {
				// if no more following packet, check the bucket value now
				// and reset the bucket
				checkAllFlows((currentPeriod + 1) * T);
				resetBucketList();
			}
			
		} else {
			// if the packet cross two periods,
			// only add the amount of the packet in the current period for now;
			// and then add the other part of the packet into the next period;
			nextPeriodPacketAmount = (int) Math.round(packet.size * 
					(packetEndTime - T * endPeriod) / (packetEndTime - packetStartTime));
			
			
			
			if (!isInBlackList) {
				Packet firstHalfPacket = new Packet(packet.flowId, 
						packet.size - nextPeriodPacketAmount, packet.time);				
				processPacketForAllStages(firstHalfPacket);
			}
			
			// check the bucket value,
			// reset the bucket, and add nextPeriodPacketAmount
			checkAllFlows((currentPeriod + 1) * T);
			resetBucketList();
			
			if (!isInBlackList) {
				Packet secondHalfPacket = new Packet(packet.flowId, nextPeriodPacketAmount, packet.time);
				processPacketForAllStages(secondHalfPacket);
			}
									
			currentPeriod = endPeriod;
		}

		return !isInBlackList;
	}
	
	private void initState() {
		stages = new ArrayList<>();
		currentPeriod = 0;

		for (int i = 0; i < numOfStages; i++) {
			List<Bucket> tmpStage = new ArrayList<>();
			for (int j = 0; j < sizeOfStage; j++) {
				tmpStage.add(new Bucket(threshold));
			}
			stages.add(tmpStage);
		}
	}
	
	private void resetBucketList() {
		for (int i = 0; i < stages.size(); i++) {
			List<Bucket> tmpStage = stages.get(i);
			for (int j = 0; j < tmpStage.size(); j++) {
				tmpStage.get(j).value = 0;
			}
		}
	}
	
	public Double getT() {
		return T;
	}
	
	public Integer getThreshold() {
		return threshold;
	}
	
	public void notConsiderPacketCrossPerids() {
		considerPacketCrossPeriods = false;
	}
	
	public void considerPacketCrossPeriods() {
		considerPacketCrossPeriods = true;
	}
	
	@Override
	public void logConfig(Logger logger) throws IOException {
		super.logConfig(logger);
		logger.logConfigMsg("Period: " + getT() + " sec \n");
		logger.logConfigMsg("Bucket threshold: " + getThreshold() + " Byte \n");
		logger.logConfigMsg("Consider packets crossing periods: " + considerPacketCrossPeriods + "\n");
	}
	
	private void processPacketForAllStages(Packet packet) {
		for (int i = 0; i < numOfStages; i++) {
			// get the buckets of the flow
			Integer index = hashFuncs.get(i).getHashCode(packet.flowId);

			// process packet into its buckets
			Bucket bucket = stages.get(i).get(index);
			bucket.processPacket(packet);
		}
	}
	
	private void checkAllFlows(double checkTime) {
		for (int i = 0; i < sizeOfStage; i++) {
			if (!stages.get(0).get(i).check()) {
				continue;
			}
			
			List<FlowId> flowIds = hashFuncs.get(0).getKeys(i);
			for (FlowId flowId : flowIds) {
			    if (blackList.containsKey(flowId)) {
			        // if the flow is already in blacklist, skip
			        continue;
			    }
			    
				boolean shouldBlackList = true;
				for (int j = 0; j < numOfStages; j++) {
					int bucketIndex = hashFuncs.get(j).getHashCode(flowId);
					if (!stages.get(j).get(bucketIndex).check()) {
						shouldBlackList = false;
						break;
					}
				}
				
				if (shouldBlackList) {
					// blacklist the flowId
		            blackList.put(flowId, checkTime);
				}
				
			}
		}
	}

}
