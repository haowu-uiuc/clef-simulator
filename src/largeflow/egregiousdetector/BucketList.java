package largeflow.egregiousdetector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.utils.RandomHashFunction;

class BucketList {
	private Integer size;
	private Integer depth;
	private List<Bucket> bucketList;
	private RandomHashFunction<FlowId> hashFunc;

    private boolean splitByRelativeValue = false;
	
	public BucketList(Integer size, Integer depth) {
		this.size = size;
		this.depth = depth;
		bucketList = new ArrayList<>(size);
		hashFunc = new RandomHashFunction<>(size);

		for (int i = 0; i < size; i++) {
			Bucket bucket = new Bucket(depth);
			bucketList.add(bucket);
		}
	}

	public void splitByRelativeValue() {
	    splitByRelativeValue = true;
	}
	
	public Integer size() {
		return size;
	}

	public Integer getDepth() {
		return depth;
	}

	public Bucket get(Integer index) {
		return bucketList.get(index);
	}

	public void set(Integer index, Integer value) {
		bucketList.get(index).setValue(value);
	}

	public void addValueTo(Integer index, Integer extraValue) {
		bucketList.get(index).addValue(extraValue);
	}

	public List<BucketList> splitTopKBuckets(Integer k) throws Exception {
		List<BucketList> childBucketLists = new ArrayList<>();

		// find top-numOfBranches buckets
		List<Bucket> topBuckets = getTopKBuckets(k);

		// split these buckets
		for (Bucket bucket : topBuckets) {
			bucket.createChildBucketList(size);
			BucketList childBucketList = bucket.getChildBucketList();
			if (splitByRelativeValue) {
			    childBucketList.splitByRelativeValue();
			}
			childBucketLists.add(childBucketList);
		}

		return childBucketLists;
	}

	public void processPacket(Packet packet, ReservationDatabase resDb) {
		Boolean isNewFlow = hashFunc.isFirstTimeSee(packet.flowId);
		Integer hashCode = hashFunc.getHashCode(packet.flowId);
		Bucket bucket = get(hashCode);
		bucket.addValue(packet.size);
		if (isNewFlow) {
			bucket.addReservation(resDb.getReservation(packet.flowId));
		}

		BucketList childBucketList = bucket.getChildBucketList();
		if (childBucketList != null) {
			childBucketList.processPacket(packet, resDb);
		}
	}

	/**
	 * To find the top-k buckets, which have the most counter values.
	 * Q1: most counter value or the most {counter value} / {reservation bandwidth} ?
	 * ***Right now, we are using the most {counter value}
	 * Q2: Is this cheap to implement in hardware?
	 * @param k
	 * @return
	 * @throws Exception
	 */
	public List<Bucket> getTopKBuckets(Integer k) throws Exception {
		// for k == 1, we just need to traverse the list
		List<Bucket> topBuckets = new ArrayList<>();

		if (k == 1) {
			int maxValue = get(0).getValue();
			int maxIndex = 0;
			for (int i = 1; i < size; i++) {
				if (get(i).getValue() > maxValue) {
					maxIndex = i;
					maxValue = get(i).getValue();
				}
			}

			topBuckets.add(get(maxIndex));
			return topBuckets;
		}

		Comparator<Bucket> comparator = new BucketValueComparator();
		if (splitByRelativeValue) {
		     comparator = new BucketRelativeValueComparator();
		}
		PriorityQueue<Bucket> queue = new PriorityQueue<>(size, comparator);
		for (int i = 0; i < size; i++) {
			Bucket bucket = get(i);
			queue.add(bucket);
		}

		for (int i = 0; i < k; i++) {
			Bucket bucket = queue.poll();

			if (bucket == null) {
				// System.out.println("k = " + k
				// + " is too large. k is larger than"
				// + " the size of bucket list (fanout)");
				break;
			}

			topBuckets.add(bucket);
		}

		return topBuckets;
	}

	public List<FlowId> getFlowsInBucket(Integer bucketIndex) {
		return hashFunc.getKeys(bucketIndex);
	}

}
