package largeflow.multistagefilter;

import largeflow.datatype.Packet;

class Bucket {

	protected Integer value;
	protected Integer threshold;

	/**
	 * Set the value as 0.
	 */
	Bucket(Integer threshold) {
		value = 0;
		this.threshold = threshold;
	}

	/**
	 * Reset the bucket. Set the value to be 0.
	 */
	void reset() {
		value = 0;
	}
	
	/**
	 * return the bucket value
	 * 
	 * @return
	 */
	Integer getValue() {
		return value;
	}

	/**
	 * Process a packet
	 * 
	 * @param packet
	 * @return TODO
	 */
	void processPacket(Packet packet) {
		value += packet.size;
	}

	/**
	 * Return true if the flow traffic of the bucket violates the flow spec;
	 * otherwise, return false.
	 * 
	 * @return
	 */
	boolean check() {
		return value > threshold;
	}
	
	/**
	 * check the bucket before actually add the value into the bucket
	 * @param packetSize
	 * @return
	 */
	boolean checkWithShielding(int packetSize) {
	    return value + packetSize > threshold;
	}

    void copyFrom(Bucket bucket) {
        value = bucket.value;
    }
}
