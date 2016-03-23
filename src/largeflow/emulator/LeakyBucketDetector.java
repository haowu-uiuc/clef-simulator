package largeflow.emulator;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

public class LeakyBucketDetector extends Detector {

	// parameter to set
	private Integer burst_th; // Byte
	private Integer linkCapacity; // Byte / sec
	private Integer rate_th; // Byte / sec, the threshold of rate for large flow

	private Map<FlowId, Integer> flowToBucketMap;
	private Map<FlowId, Double> flowToLastPacketEndTimeMap;

	private void initStates() {
		flowToBucketMap = new TreeMap<>();
		flowToLastPacketEndTimeMap = new TreeMap<>();
	}

	public LeakyBucketDetector(String detectorName, Integer burst_th,
			Integer rate_th, Integer linkCapacity) {
		super(detectorName);
		this.burst_th = burst_th;
		this.rate_th = rate_th;
		this.linkCapacity = linkCapacity;

		initStates();
	}

	public int getRateThreshold() {
		return rate_th;
	}

	public int getBurstThreshold() {
		return burst_th;
	}

	public void reset() {
		super.reset();
		initStates();
	}

	@Override
	public void logConfig(Logger logger) throws IOException{
		super.logConfig(logger);
		logger.logConfigMsg("Leaky Rate: " + getRateThreshold() + " Byte / sec\n");
		logger.logConfigMsg("Bucket Size: " + getBurstThreshold() + " Byte\n");
	}
	
	@Override
	public boolean processPacket(Packet packet) {
		if (blackList.containsKey(packet.flowId)) {
			return false;
		}

		double lastPacketEndTime = packet.time + (double) packet.size
				/ (double) linkCapacity;
		int curDecrement = (int) Math.round((double) packet.size / (double) linkCapacity
				* (double) rate_th);

		if (!flowToBucketMap.containsKey(packet.flowId)) {
			flowToBucketMap.put(packet.flowId, packet.size - curDecrement);
			flowToLastPacketEndTimeMap.put(packet.flowId, lastPacketEndTime);
		} else {
			int decrement = (int) Math.round((packet.time - flowToLastPacketEndTimeMap
					.get(packet.flowId)) * (double) rate_th);
			int bucketValue = flowToBucketMap.get(packet.flowId) - decrement;
			if (bucketValue < 0) {
				bucketValue = 0;
			}

			flowToBucketMap.put(packet.flowId, bucketValue + packet.size
					- curDecrement);
			flowToLastPacketEndTimeMap.put(packet.flowId, lastPacketEndTime);
		}

		if (flowToBucketMap.get(packet.flowId) > burst_th) {
			// put into blacklist
			blackList.put(packet.flowId, lastPacketEndTime);
			flowToBucketMap.remove(packet.flowId);
			flowToLastPacketEndTimeMap.remove(packet.flowId);
			return false;
		}

		return true;
	}

	/**
	 * this method does nothing for leaky bucket detector
	 */
	@Override
	public void setNumOfCounters(Integer numOfCounters) {
		return;
	}

}
