package largeflow.eardet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.Logger;

public class EARDet extends Detector {

	// parameter to set
	private Integer alpha; // max packet size, Byte
	private Integer numOfCounters;
	private Integer beta_th; // Byte
	private Integer linkCapacity; // Byte / sec
	private Integer maxVirtualPacketSize; // Byte e.g. = beta_th - 1
	private Integer curVirtualFlowIdValue;
	private Double maxIncubationTime; // sec

	// EARDet configs to read
	private Integer beta_h; // Byte
	private Integer beta_l; // Byte
	private Integer gamma_h; // Byte / sec
	private Integer gamma_l; // Byte / sec

	private Double currentTime; // sec
	private List<Integer> counterList;
	private Map<FlowId, Integer> flowToCounterMap;
	private Map<Integer, FlowId> counterToFlowMap;

	/**
	 * directly set the parameters for EARDet
	 * 
	 * @param detectorName
	 * @param alpha
	 * @param beta_th
	 * @param numOfCounters
	 * @param linkCapacity
	 */
	public EARDet(String detectorName, Integer alpha, Integer beta_th,
			Integer numOfCounters, Integer linkCapacity) {
		super(detectorName);

		// parameters
		this.linkCapacity = linkCapacity;
		this.alpha = alpha;
		this.beta_th = beta_th;
		this.numOfCounters = numOfCounters;
		maxVirtualPacketSize = beta_th - 1;

		// properties unknown?
		gamma_h = linkCapacity / (numOfCounters + 1);
		gamma_l = -1;
		beta_h = alpha + 2 * beta_th;
		beta_l = -1;
		maxIncubationTime = -1.0;

		// init states
		initStates(numOfCounters);
	}

	/**
	 * set the properties like high bandwidth threshold, low bandwidth
	 * threshold, and max incubation time. Then use such requirements to
	 * calculate the parameters of EARDet.
	 * 
	 * @param detectorName
	 * @param alpha
	 * @param beta_l
	 * @param gamma_h
	 * @param gamma_l
	 * @param maxIncubationTime
	 * @param linkCapacity
	 * @throws Exception
	 */
	public EARDet(String detectorName, Integer alpha, Integer beta_l,
			Integer gamma_h, Integer gamma_l, Double maxIncubationTime,
			Integer linkCapacity) throws Exception {
		super(detectorName);

		// properties
		this.beta_l = beta_l;
		this.gamma_h = gamma_h;
		this.gamma_l = gamma_l;
		this.alpha = alpha;
		this.maxIncubationTime = maxIncubationTime;
		this.linkCapacity = linkCapacity;

		double M = (double) gamma_h + (double) gamma_l - 2.
				* (double) (alpha + beta_l) / this.maxIncubationTime;
		if (M < 0) {
			throw new Exception(
					"M = gamma_h + gamma_l - 2 * (alpha + beta_l) / maxIncubationTime,"
							+ " M cannot below zero!");
		}

		double delta = M * M - (double) 4 * (double) gamma_h * (double) gamma_l;
		if (delta < 0) {
			throw new Exception(
					"delta = M*M - 4*gamma_h*gamma_l, delta cannot be below zero!");
		}

		double n = (double) linkCapacity / (M + Math.sqrt(delta)) * 2. - 1.;

		// parameters
		numOfCounters = (int) n + 1;
		beta_th = (int) ((double) beta_l + ((double) gamma_l * (double) (alpha + beta_l))
				/ ((double) linkCapacity / (double) (numOfCounters + 1) - (double) gamma_l)) + 1;
		beta_h = 2 * beta_th + alpha;
		setMaxVirtualPacketSize(beta_th);

		// init states
		initStates(numOfCounters);
	}

	@Override
	public void reset() {
		super.reset();
		initStates(numOfCounters);
	}

	private void initStates(int numOfCounters) {
		counterList = new ArrayList<>();
		for (int i = 0; i < numOfCounters; i++) {
			counterList.add(0);
		}
		currentTime = 0.;
		curVirtualFlowIdValue = 0;
		flowToCounterMap = new TreeMap<>();
		counterToFlowMap = new HashMap<>();
	}

	public void setAlpha(Integer alpha) {
		this.alpha = alpha;
	}

	/**
	 * set the number of counter and reset EARDet
	 */
	@Override
	public void setNumOfCounters(Integer numOfCounters) {
		this.numOfCounters = numOfCounters;
		reset();
	}

	public void setBetaTh(Integer beta_th) {
		this.beta_th = beta_th;
		setMaxVirtualPacketSize(beta_th);
	}

	private void setMaxVirtualPacketSize(Integer beta_th) {
		maxVirtualPacketSize = beta_th - 1;
	}

	public void setLinkCapacity(Integer linkCapacity) {
		this.linkCapacity = linkCapacity;
	}

	public Integer getAlpha() {
		return alpha;
	}

	public Integer getBetaH() {
		return beta_h;
	}

	public Integer getBetaL() {
		return beta_l;
	}

	public Integer getGammaH() {

		return gamma_h;
	}

	public Integer getGammaL() {
		return gamma_l;
	}

	public Integer getBetaTh() {
		return beta_th;
	}

	public Integer getNumOfCounters() {
		return numOfCounters;
	}

	public Integer getLinkCapacity() {
		return linkCapacity;
	}
	
	public Double getMaxIncubationTime() {
		return maxIncubationTime;
	}

	@Override
	public boolean processPacket(Packet packet) throws Exception {
		double lastPacketEndTime = currentTime;
		currentTime = packet.time + (double) packet.size
				/ (double) linkCapacity;

		// if the flow is in blackList, then ignore
		if (blackList.containsKey(packet.flowId)) {
			return false;
		}

		// process virtual flow
		int virtualTrafficSize = (int) ((packet.time - lastPacketEndTime) *
				linkCapacity) + 1;
		while (virtualTrafficSize >= maxVirtualPacketSize) {
			virtualTrafficSize -= maxVirtualPacketSize;
			Packet virPacket = new Packet(getNextVirtualFlowId(),
					maxVirtualPacketSize, lastPacketEndTime); // time doesnt
																// matter here
			addPacketToCounter(virPacket);
		}

		if (virtualTrafficSize > 0) {
			Packet virPacket = new Packet(getNextVirtualFlowId(),
					virtualTrafficSize, lastPacketEndTime); // time doesnt
															// matter here
			addPacketToCounter(virPacket);
		}

		// process this real packet
		int counterIndex = addPacketToCounter(packet);
		if (counterIndex >= 0 && isCounterOverflow(counterIndex)) {
			blackList.put(packet.flowId, currentTime);
			return false;
		}

		return true;
	}

	private Integer addPacketToCounter(Packet packet) throws Exception {
		// return the index of the counter associated to the packet, -1 if no
		// counter associated to.

		// if the flow is associated with counter, just increase this counter
		if (flowToCounterMap.containsKey(packet.flowId)) {
			int counterIndex = flowToCounterMap.get(packet.flowId);
			counterList.set(counterIndex, counterList.get(counterIndex)
					+ packet.size);
			return counterIndex;
		} else { // if the flow is new to counter
			// find the counter with min value
			int minIndex = 0;
			int minValue = counterList.get(minIndex);

			for (int i = 1; i < numOfCounters; i++) {
				int curValue = counterList.get(i);
				if (curValue < minValue) {
					minIndex = i;
					minValue = curValue;
				}
			}

			if (minValue < 0) {
				throw new Exception(
						"minValue < 0. Counter value should not below zero!");
			}

			int valueLeft = packet.size - minValue;
			if (valueLeft > 0) {
				decreaseAllCountersBy(minValue);

				// update the counter
				counterList.set(minIndex, valueLeft);

				// update the counter flow maps
				FlowId preFlowId = counterToFlowMap.get(minIndex);
				if (preFlowId != null) {
					flowToCounterMap.remove(preFlowId);
				}
				flowToCounterMap.put(packet.flowId, minIndex);
				counterToFlowMap.put(minIndex, packet.flowId);

				return minIndex;
			} else if (valueLeft < 0) {
				decreaseAllCountersBy(packet.size);
				return -1;
			} else {
				decreaseAllCountersBy(packet.size);
				FlowId preFlowId = counterToFlowMap.get(minIndex);
				flowToCounterMap.remove(preFlowId);
				counterToFlowMap.remove(minIndex);
				return -1;
			}
		}
	}

	@Override
	public void logConfig(Logger logger) throws IOException {
		super.logConfig(logger);
		logger.logConfigMsg("High-Bandwidth Rate Threshold: " + getGammaH() + " Byte / sec\n");
		logger.logConfigMsg("High-Bandwidth Burst Threshold: " + getBetaH() + " Byte\n");
		logger.logConfigMsg("Low-Bandwidth Rate Threshold: " + getGammaL() + " Byte / sec\n");
		logger.logConfigMsg("Low-Bandwidth Burst Threshold: " + getBetaL() + " Byte\n");
		logger.logConfigMsg("Num of Counter: " + getNumOfCounters() + "\n");
		logger.logConfigMsg("Max Packet Size: " + getAlpha() + " Byte\n");
		logger.logConfigMsg("Counter Threshold: " + getBetaTh() + " Byte\n");
	}
	
	private boolean isCounterOverflow(Integer counterIndex) {
		if (counterList.get(counterIndex) > beta_th) {
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

	private void decreaseAllCountersBy(Integer valueToDecrease) {
		if (valueToDecrease == 0) {
			return;
		}

		for (int i = 0; i < numOfCounters; i++) {
			counterList.set(i, counterList.get(i) - valueToDecrease);
		}
	}

}
