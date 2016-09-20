package largeflow.multistagefilter;

import java.io.IOException;

import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.Logger;

public class FlowMemoryDetector extends Detector{

    private Integer numOfCounters;
    private FlowMemoryFactory flowMemoryFactory;
    private FlowMemory flowMemory;
    private Integer linkCapacity;
    
    public FlowMemoryDetector(String detectorName,
            Integer numOfCounters,
            Integer linkCapacity,
            FlowMemoryFactory flowMemoryFactory) {
        super(detectorName);
        this.numOfCounters = numOfCounters;
        this.linkCapacity = linkCapacity;
        flowMemory = flowMemoryFactory.createFlowMemory(numOfCounters);
        this.flowMemoryFactory = flowMemoryFactory;
    }

    public void setFlowMemoryFactory(FlowMemoryFactory flowMemoryFactory) throws Exception {
        this.flowMemoryFactory = flowMemoryFactory;
        setNumOfCounters(numOfCounters);
    }
    
    @Override
    public boolean processPacket(Packet packet) throws Exception {
        if (blackList.containsKey(packet.flowId)) {
            return false;
        }
        
        boolean flowInFM = flowMemory.flowIsInFlowMemory(packet.flowId);
        if (flowInFM) {
            // if the flow is already in the flow memory,
            // pass the packet to the FM and see whether the bucket value
            // reaches the threshold
            boolean shouldBlackList = flowMemory.processPacket(packet);
            if (shouldBlackList) {
                Double blackListTime = packet.time
                        + (double) packet.size / (double) linkCapacity;
                blackList.put(packet.flowId, blackListTime);
                return false;
            }
        } else {
            flowMemory.addFlow(packet.flowId);
        }

        return true;
    }

    @Override
    public void setNumOfCounters(Integer numOfCounters) throws Exception {
        this.numOfCounters = numOfCounters;
        flowMemory = flowMemoryFactory.createFlowMemory(numOfCounters);
        reset();
    }
    
    @Override
    public void reset() {
        super.reset();
        flowMemory.reset();
    }
    
    @Override
    public void logConfig(Logger logger) throws IOException {
        super.logConfig(logger);
        logger.logConfigMsg("Num of Counter: " + numOfCounters + "\n");
        flowMemory.logConfig(logger);
    }

}
