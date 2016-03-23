package largeflow.multistagefilter;

import largeflow.datatype.FutureInfoPacket;

@Deprecated
class PeriodicBucket extends Bucket {

	private Double T; // period for periodic check
	private Integer linkCapacity;
	private Integer currentPeriod; // current period number (= 0, 1, 2, ...)
	private boolean considerPacketCrossPeriods;

	/**
	 * 
	 * @param T : period of periodic check. 
	 * It has to be larger than maxPacketSize / linkCapacity
	 * @param threshold
	 * @param linkCapacity
	 * @throws Exception 
	 */
	PeriodicBucket(Double T,
			Integer threshold,
			Integer linkCapacity) throws Exception {
		super(threshold);
		currentPeriod = 0;
		
		this.T = T;
		this.linkCapacity = linkCapacity;
		
		int maxPacketSize = 1518;
		if (T < maxPacketSize / linkCapacity) {
			throw new Exception("Period T has to be larger than maxPacketSize / linkCapacity .");
		}
		
		// default to consider packet across periods.
		considerPacketCrossPeriods = true;
	}

	void notConsiderPacketCrossPerids() {
		considerPacketCrossPeriods = false;
	}
	
	void considerPacketCrossPeriods() {
		considerPacketCrossPeriods = true;
	}
	
	@Override
	void reset() {
		super.reset();
		currentPeriod = 0;
	}

	boolean processAndCheckPacket(FutureInfoPacket packet) {
		double packetStartTime = packet.time;
		double packetEndTime = packet.time + (double) packet.size / (double) linkCapacity;
		int startPeriod = (int) Math.floor(packetStartTime / T);
		int endPeriod = (int) Math.floor(packetEndTime / T);
		boolean shouldBlacklist = false;
		
		currentPeriod = startPeriod;
				
		int nextPeriodPacketAmount = 0;
		if (startPeriod == endPeriod || !considerPacketCrossPeriods) {
			// if the packet does not cross two periods
			// PS: if considerPacketCrossPeriods == true, 
			// then we will ignore the "else" branch anyway.
			value += packet.size;
			
			if (packet.nextPacketTime != null) {
				// if there is a following packet, get the period of next packet
				int nextPacketPeriod = (int) Math.floor(packet.nextPacketTime / T);
				
				if (nextPacketPeriod > currentPeriod) {
					// if the next packet happens in the next period,
					// then check the bucket value with the threshold,
					// and reset the bucket value to zero
					if (value > threshold) {
						shouldBlacklist = true;
					}
					value = 0;
				}
			} else {
				// if no more following packet, check the bucket value now
				// and reset the bucket
				if (value > threshold) {
					shouldBlacklist = true;
				}
				value = 0;
			}
			
		} else {
			// if the packet cross two periods,
			// only add the amount of the packet in the current period for now;
			// and then add the other part of the packet into the next period;
			nextPeriodPacketAmount = (int) Math.round(packet.size * 
					(packetEndTime - T * endPeriod) / (packetEndTime - packetStartTime));
			value += (packet.size - nextPeriodPacketAmount); // amount of the packet in this period
			
			// check the bucket value,
			// reset the bucket, and add nextPeriodPacketAmount
			if (value > threshold) {
				shouldBlacklist = true;
			}
			
			value = nextPeriodPacketAmount;			
			currentPeriod = endPeriod;
			
		}
				
		System.out.println("Periodic Bucket: shouldBlock = " + shouldBlacklist);
		
		return !shouldBlacklist;
	}

}
