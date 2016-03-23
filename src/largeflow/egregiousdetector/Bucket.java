package largeflow.egregiousdetector;

/**
 * Bucket data type. This class is only used in this package, so it is not
 * public.
 * 
 * @author HaoWu
 *
 */
class Bucket {
	private Integer value;
	private Integer depth;
	private BucketList childBucketList;
	private Integer reservation; // reserved bandwidth of all flows in the
									// bucket. Byte / sec

	public Bucket(Integer depth) {
		this.depth = depth;
		reservation = 0;
		value = 0;
		childBucketList = null;
	}

	public Integer getDepth() {
		return depth;
	}

	public Integer getValue() {
		return value;
	}

	public void addReservation(Integer resOfNewFlow) {
		reservation += resOfNewFlow;
	}

	public Integer getReservation() {
		return reservation;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	public void addValue(Integer extraValue) {
		value += extraValue;
	}

	public Boolean isChildBucketListNull() {
		if (childBucketList == null) {
			return true;
		} else {
			return false;
		}
	}

	public BucketList getChildBucketList() {
		return childBucketList;
	}

	public void createChildBucketList(Integer size) {
		childBucketList = new BucketList(size, depth + 1);
	}

	public void tearDownChildBucketList() {
		childBucketList = null;
	}

	public Boolean isNotDeeperThan(Integer depthToCompare) {
		return (depth < depthToCompare);
	}

	public String toString() {
		return value.toString();
	}
}
