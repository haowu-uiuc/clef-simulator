package largeflow.eardet;

import java.io.IOException;
import java.util.Random;

import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.Logger;

/**
 * virtual link EARDet
 * @author HaoWu
 *
 */
public class VirlinkEARDet extends Detector {

	// parameter to set
	private Integer alpha; // max packet size, Byte
	private Integer numOfCounters;
	private Integer beta_th; // Byte
	private Integer linkCapacity; // Byte / sec
	private Integer virtualLinkCapacity; // Byte / sec
	private Integer queueSizeThreshold; // Byte
	private Double probToDrop = 0.5;

	// system states and modules
	private Double currentTime; // sec
	private Double virtualCurrentTime; // sec
	private Double waitTime; // sec
	private EARDet eardet;
	private Random randGenerator;

	public VirlinkEARDet(String detectorName,
			Integer alpha,
			Integer beta_th,
			Integer numOfCounters,
			Integer linkCapacity,
			Integer virtualLinkCapacity,
			Integer queueSizeThreshold,
			Double probToDrop) {
		super(detectorName);

		eardet = new EARDet(detectorName,
				alpha,
				beta_th,
				numOfCounters,
				virtualLinkCapacity);

		this.linkCapacity = linkCapacity;
		this.virtualLinkCapacity = virtualLinkCapacity;
		this.queueSizeThreshold = queueSizeThreshold;
		this.probToDrop = probToDrop;

		setParameters();
		initStates();
	}

	public VirlinkEARDet(String detectorName,
			Integer alpha,
			Integer beta_l,
			Integer gamma_h,
			Integer gamma_l,
			Double maxIncubationTime,
			Integer linkCapacity,
			Integer virtualLinkCapacity,
			Integer queueSizeThreshold,
			Double probToDrop) throws Exception {
		super(detectorName);

		eardet = new EARDet(detectorName,
				alpha,
				beta_l,
				gamma_h,
				gamma_l,
				maxIncubationTime,
				virtualLinkCapacity);

		this.linkCapacity = linkCapacity;
		this.virtualLinkCapacity = virtualLinkCapacity;
		this.queueSizeThreshold = queueSizeThreshold;
		this.probToDrop = probToDrop;

		setParameters();
		initStates();
	}

	@Override
	public void reset() {
		super.reset();
		initStates();
	}

	private void setParameters() {
		this.numOfCounters = eardet.getNumOfCounters();
		this.alpha = eardet.getAlpha();
		this.beta_th = eardet.getBetaTh();
	}

	private void initStates() {
		eardet.reset();
		waitTime = 0.0;
		currentTime = 0.0;
		virtualCurrentTime = 0.0;
		randGenerator = new Random(System.currentTimeMillis());
	}

	public void setAlpha(Integer alpha) {
		eardet.setAlpha(alpha);
		this.alpha = eardet.getAlpha();
	}

	/**
	 * set the number of counter and reset EARDet
	 */
	@Override
	public void setNumOfCounters(Integer numOfCounters) {
		eardet.setNumOfCounters(numOfCounters);
		this.numOfCounters = eardet.getNumOfCounters();
		reset();
	}

	public void setBetaTh(Integer beta_th) {
		eardet.setBetaTh(beta_th);
		this.beta_th = eardet.getBetaTh();
	}

	public void setLinkCapacity(Integer linkCapacity) {
		this.linkCapacity = linkCapacity;
	}

	public void setVirtualLinkCapacity(Integer virtualLinkCapacity) {
		this.virtualLinkCapacity = virtualLinkCapacity;
		eardet.setLinkCapacity(virtualLinkCapacity);
	}

	public Integer getAlpha() {
		return alpha;
	}

	public Integer getBetaH() {
		return eardet.getBetaH();
	}

	public Integer getBetaL() {
		return eardet.getBetaL();
	}

	public Integer getGammaH() {

		return eardet.getGammaH();
	}

	public Integer getGammaL() {
		return eardet.getGammaL();
	}

	public Integer getBetaTh() {
		return eardet.getBetaTh();
	}

	public Integer getNumOfCounters() {
		return numOfCounters;
	}

	public Integer getLinkCapacity() {
		return linkCapacity;
	}

	public Integer getVirtualLinkCapacity() {
		return virtualLinkCapacity;
	}

	public Integer getQueueSizeThreshold() {
		return queueSizeThreshold;
	}
	
	public Double getProbToDrop() {
		return probToDrop;
	}
	
	// public Double getMaxIncubationTime() {
	// return eardet.getMaxIncubationTime();
	// }

	@Override
	public boolean processPacket(Packet packet) throws Exception {
		// if the flow is in blackList, then ignore
		if (blackList.containsKey(packet.flowId)) {
			return false;
		}

		// calculate waiting time and the packet timing in the virtual link
		double lastPacketVirtualEndTime = virtualCurrentTime;
		double transmitTime = (double) packet.size / (double) linkCapacity;
		double virtualTransmitTime = (double) packet.size
				/ (double) virtualLinkCapacity;

		double arriveTime = packet.time;
		waitTime = Math.max(lastPacketVirtualEndTime - arriveTime, 0.0);

		// determine the arrival time for the packet in the virtual link
		double virtualArriveTime = arriveTime;
		if (waitTime > 0) {
			virtualArriveTime = lastPacketVirtualEndTime;
		}

		// TODO: use RED to drop the packet
		// check waitTime with a threshold {queueSize / virtualLinkCapacity}
		if (waitTime > 0) {
			double dropProb = probToDrop * waitTime * virtualLinkCapacity / queueSizeThreshold;
			// dropProb > 1 ==> must drop
			// virtually drop the packet, i.e. do not consider it in the EARDet
			if (randGenerator.nextDouble() < dropProb) {
				System.out.println("packet drop by " + detectorName);
				return true;
			}
		}

		currentTime = packet.time + transmitTime;
		virtualCurrentTime = virtualArriveTime + virtualTransmitTime;

		// For an incoming packet (s, t) --> packet (s, t') in virtual link.
		// Put the manipulated packet (s, t') into the EARDet
		Packet virtualPacket = new Packet(packet.flowId,
				packet.size,
				virtualArriveTime);
		if (!eardet.processPacket(virtualPacket)) {
			blackList.put(packet.flowId, currentTime);
			return false;
		}

		return true;
	}

	@Override
	public void logConfig(Logger logger) throws IOException {
		super.logConfig(logger);
		logger.logConfigMsg("Link Capacity: " + getLinkCapacity()
				+ " Byte / sec\n");
		logger.logConfigMsg("Virtual Link Capacity: "
				+ getVirtualLinkCapacity() + " Byte / sec\n");
		logger.logConfigMsg("High-Bandwidth Rate Threshold: " + getGammaH()
				+ " Byte / sec\n");
		logger.logConfigMsg("High-Bandwidth Burst Threshold: " + getBetaH()
				+ " Byte\n");
		logger.logConfigMsg("Low-Bandwidth Rate Threshold: " + getGammaL()
				+ " Byte / sec\n");
		logger.logConfigMsg("Low-Bandwidth Burst Threshold: " + getBetaL()
				+ " Byte\n");
		logger.logConfigMsg("Num of Counter: " + getNumOfCounters() + "\n");
		logger.logConfigMsg("Max Packet Size: " + getAlpha() + " Byte\n");
		logger.logConfigMsg("Counter Threshold: " + getBetaTh() + " Byte\n");
	}

}
