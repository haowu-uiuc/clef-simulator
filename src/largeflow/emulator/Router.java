package largeflow.emulator;

import java.io.IOException;
import java.util.Map;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

public interface Router {

    public String name();
    
	public void reset();
	
	/**
	 * return false, when the packet is already in the blacklist
	 * @param packet
	 * @return
	 * @throws Exception
	 */
	public boolean processPacket(Packet packet) throws Exception;

	public void processEnd() throws Exception;
	
	public Boolean flowIsInBlackList(FlowId flowId);
	
	public Double flowBlackListTime(FlowId flowId);
	
	/**
	 * please call this at the end of the a run.
	 * @return
	 */
	public Map<FlowId, Double> getBlackList();
	
	/**
	 * get the map of dropped traffic of a flow in the blacklist
	 * flowId <-> dropped traffic volume
	 * @return
	 */
	public Map<FlowId, Integer> getDroppdTrafficMap();
	
	public void setNumOfDetectorCounters(Integer numOfCounters) throws Exception;
	
	public void logConfig(Logger logger) throws IOException;
	
}
