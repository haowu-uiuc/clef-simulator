package largeflow.multistagefilter;

import largeflow.datatype.Packet;

class LeakyBucket extends Bucket {

	private Integer drainRate;
	private Integer linkCapacity;
	private Double currentTime;

	LeakyBucket(Integer threshold,
			Integer drainRate,
			Integer linkCapacity) {
		super(threshold);
		currentTime = 0.;

		this.drainRate = drainRate;
		this.linkCapacity = linkCapacity;
	}

	@Override
	void reset() {
		super.reset();
		currentTime = 0.;
	}

	@Override
	void processPacket(Packet packet) {
		Double lastTime = currentTime;
		currentTime = packet.time;

		// decrease the bucket before adding the packet
		value -= (int) Math.round((currentTime - lastTime) * drainRate);
		if (value < 0) {
			value = 0;
		}

		// add packet into the bucket
		value += packet.size;

		// decrease the bucket
		Double processTime = (double) packet.size / (double) linkCapacity;
		value -= (int) Math.round(processTime * drainRate);
		currentTime += processTime;
	}

}
