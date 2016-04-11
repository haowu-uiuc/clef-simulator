package largeflow.emulator;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

public abstract class Detector {
	// flow ID in blacklist and the time it got caught
	protected Map<FlowId, Double> blackList;
	protected String detectorName;

	public Detector(String detectorName) {
		blackList = new TreeMap<>();
		this.detectorName = detectorName;
	}

	// reset the detector for a new run
	// you should reset all status of the detector to the initial, e.g. counter
	// values
	public void reset() {
		blackList = new TreeMap<>();
	}

	public String name() {
		return detectorName;
	}

	public Map<FlowId, Double> getBlackList() {
		return blackList;
	}

	public Double getBlackListTime(FlowId flowId) {
		// if flow id is not in the map, then return null
		return blackList.get(flowId);
	}

	public void logConfig(Logger logger) throws IOException {
		logger.logConfigMsg("Detector Name: " + name() + "\n");
	}

	/**
	 * process each packet pass through the detector,
	 * if the packet is blocked by detector, return false, otherwise return true
	 * also record caught large flow into blacklist map : {flow Id} -> {catch time}
	 * @param packet
	 * @return true => pass the detector, flase => blocked by detector
	 * @throws Exception
	 */
	abstract public boolean processPacket(Packet packet) throws Exception;

	// set the number of counters used by detector. Namely how much memory used.
	abstract public void setNumOfCounters(Integer numOfCounters) throws Exception;

}
