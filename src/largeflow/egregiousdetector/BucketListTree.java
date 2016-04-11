package largeflow.egregiousdetector;

import java.util.ArrayList;
import java.util.List;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.NetworkConfig;

class BucketListTree {

	private Integer maxDepth;
	private Integer currentLevel;
	private Integer fanout;
	private Integer numOfBranches;

	private ReservationDatabase resDb;

	private BucketList rootBucketList;
	private List<BucketList> bottomBucketLists;

	public BucketListTree(Integer maxDepth, Integer fanout,
			Integer numOfBranches, ReservationDatabase resDb) throws Exception {
		if (resDb == null) {
			throw new Exception("Reservation Database cannot be null!");
		}
		this.resDb = resDb;
		this.maxDepth = maxDepth;
		this.fanout = fanout;
		this.numOfBranches = numOfBranches;

		currentLevel = 1;
		rootBucketList = new BucketList(fanout, currentLevel);
		bottomBucketLists = new ArrayList<>();
		bottomBucketLists.add(rootBucketList);
	}

	private void setMaxDepth(Integer maxDepth) {
		this.maxDepth = maxDepth;
	}

	public void reset() {
		currentLevel = 1;
		rootBucketList = new BucketList(fanout, currentLevel);
		bottomBucketLists = new ArrayList<>();
		bottomBucketLists.add(rootBucketList);
	}

	public void reset(Integer newMaxDepth) {
		setMaxDepth(newMaxDepth);
		reset();
	}

	public void processPacket(Packet packet) {
		rootBucketList.processPacket(packet, resDb);
	}

	public Integer getCurrentLevel() {
		return currentLevel;
	}

	public Integer getMaxDepth() {
		return maxDepth;
	}

	public List<BucketList> getBottomBucketLists() {
		return bottomBucketLists;
	}

	public Boolean splitBuckets() throws Exception {
		if (currentLevel >= maxDepth) {
			return false;
		}

		int k = 1; // for level below root
		if (currentLevel == 1) {
			// for root level
			k = numOfBranches;
		}

		List<BucketList> newBottomBucketLists = new ArrayList<>();
		for (BucketList bucketList : bottomBucketLists) {
			List<BucketList> childBucketLists = bucketList.splitTopKBuckets(k);
			newBottomBucketLists.addAll(childBucketLists);
		}

		bottomBucketLists = newBottomBucketLists;
		currentLevel++;

		return true;
	}

	/**
	 * check the buckets in the bottom and return the flow IDs if there are
	 * large flows in a time interval.
	 * 
	 * @return
	 */
	public List<FlowId> checkBottomBuckets(Double timeInterval) {
		List<FlowId> largeFlowList = new ArrayList<>();

		if (currentLevel < maxDepth) {
			return largeFlowList;
		}

		for (BucketList bucketList : bottomBucketLists) {
			// check whether the bucket value exceeds the reservation, if so,
			// return the flows assigned to the bucket.
			for (int bucketIndex = 0; bucketIndex < bucketList.size(); bucketIndex++) {
				Bucket bucket = bucketList.get(bucketIndex);
				List<FlowId> bucketFlows = bucketList.getFlowsInBucket(bucketIndex);
				        
				int value = bucket.getValue();
				int reservation = bucket.getReservation();
                if (bucketFlows.size() == 1
                        && value > (int) (reservation * timeInterval) + NetworkConfig.maxPacketSize) {
					// the +1 is to raise a little reservation so that avoid
					// some false positive at corner
					// put all flows in this bucket into the large flow list
                    // Only consider bucket with one flow to avoid FP
					largeFlowList.addAll(bucketFlows);
				}
			}

		}

		return largeFlowList;
	}

}
