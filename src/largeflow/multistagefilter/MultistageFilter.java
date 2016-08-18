package largeflow.multistagefilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.Logger;
import largeflow.utils.GenericUtils;
import largeflow.utils.RandomHashFunction;

abstract class MultistageFilter extends Detector {

    private boolean DEBUG = false;
    
    // parameters
    protected Integer numOfStages;
    protected Integer sizeOfStage;
    protected Integer numOfCounters;
    protected Double ratioOfFlowMemory = 0.5; // default value = 0.5
    protected Integer linkCapacity;
    protected List<RandomHashFunction<FlowId>> hashFuncs;
    protected FlowMemoryFactory flowMemoryFactory;

    // states
    protected List<List<Bucket>> stages;
    protected FlowMemory flowMemory;

    public MultistageFilter(String detectorName,
            Integer numOfStages,
            Integer sizeOfStage,
            Integer linkCapacity) {
        super(detectorName);

        this.numOfStages = numOfStages;
        this.sizeOfStage = sizeOfStage;
        this.numOfCounters = numOfStages * sizeOfStage;
        this.linkCapacity = linkCapacity;

        initHashFuncs();
    }

    private void initHashFuncs() {
        hashFuncs = new ArrayList<>(numOfStages);
        for (int i = 0; i < numOfStages; i++) {
            hashFuncs.add(new RandomHashFunction<>(sizeOfStage));
        }
    }

    @Override
    public void reset() {
        super.reset();
        initHashFuncs();
        if (flowMemory != null) {
            flowMemory.reset();
        }
    }

    @Override
    public void setNumOfCounters(Integer numOfCounters) throws Exception {
        int numOfCountUsed = 0;
        if (flowMemoryFactory != null) {
            numOfCountUsed = (int) (numOfCounters * ratioOfFlowMemory);
            flowMemory = flowMemoryFactory.createFlowMemory(numOfCountUsed);
        }
        this.numOfCounters = numOfCounters;
        sizeOfStage = (numOfCounters - numOfCountUsed) / numOfStages;
        if (DEBUG) {
            System.out.println("AMF Config: Flow Memory Counters: " + numOfCountUsed 
                    + ", Size of Stage: " + sizeOfStage + ", Num of Stages: " + numOfStages);
        }
        reset();
    }

    public void setFlowMemoryFactory(FlowMemoryFactory flowMemoryFactory) throws Exception {
        this.flowMemoryFactory = flowMemoryFactory;
        flowMemory = flowMemoryFactory
                .createFlowMemory((int) (numOfCounters * ratioOfFlowMemory));
        setNumOfCounters(numOfCounters);
    }

    public void setRatioOfFlowMemory(double ratio) throws Exception {
        this.ratioOfFlowMemory = ratio;
        setNumOfCounters(numOfCounters);
    }

    public FlowMemory getFlowMemory() {
        return flowMemory;
    }

    public Integer getNumOfStages() {
        return numOfStages;
    }

    public Integer getSizeOfStage() {
        return sizeOfStage;
    }

    public Integer getNumOfCounters() {
        return numOfCounters;
    }

    public Integer getLinkCapacity() {
        return linkCapacity;
    }

    @Override
    public void logConfig(Logger logger) throws IOException {
        super.logConfig(logger);
        logger.logConfigMsg("Number of stages: " + getNumOfStages() + "\n");
        logger.logConfigMsg("Size of each stage: " + getSizeOfStage() + " \n");
        logger.logConfigMsg("Num of Counter: " + getNumOfCounters() + "\n");
        logger.logConfigMsg(
                "Link capacity: " + getLinkCapacity() + " Byte/s \n");
    }

    @Override
    public boolean processPacket(Packet packet) throws Exception {
        if (blackList.containsKey(packet.flowId)) {
            return false;
        }

        // if no flow memory
        if (flowMemory == null) {
            // pass the packet into the multistage filter anyway
            Map<FlowId, Double> flowsToBlock = processPacketInMultistage(
                    packet);
            GenericUtils.addAllNewEntriesIntoMap(blackList, flowsToBlock);
            if (flowsToBlock.containsKey(packet.flowId)) {
                return false;
            } else {
                return true;
            }
        }

        // if the flow is already in the flow memory,
        // OR
        // if the flow is not in flow memory then pass it to multistage filter,
        // and the flow has passed through the multistage filter
        // THEN
        // pass the packet through flow memory
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
            // If the flow is not in flow memory then pass it to multistage
            // filter, add all flows (except flows already in FM and blacklist)
            // return from multistage filter into FM.
            // Multistage filte like FMF may return more than one flows, because
            // it is based on the periodical check.
            Set<FlowId> flowsToFM = new HashSet<>();
            flowsToFM.addAll(processPacketInMultistage(packet).keySet());

            // pass flows to flow memory
            Set<FlowId> tmpFlowIds = new HashSet<>(flowsToFM);
            for (FlowId flowId : tmpFlowIds) {
                // filter out flows which are are ready in FM and Blacklist
                if (blackList.containsKey(flowId)
                        || flowMemory.flowIsInFlowMemory(flowId)) {
                    flowsToFM.remove(flowId);
                }
            }

            for (FlowId flowId : flowsToFM) {
                flowMemory.addFlow(flowId);
            }
        }

        return true;
    }

    public void enableDebug() {
        DEBUG = true;
    }
    
    public void disableDebug() {
        DEBUG = false;
    }
    
    /**
     * process packet in the multi-stages.
     * 
     * @param packet
     * @return Map<FlowId, Double> : map of flow_id - filter_time to show the
     *         flows passing the filter this and the it's time.
     * @throws Exception
     */
    abstract public Map<FlowId, Double> processPacketInMultistage(Packet packet)
            throws Exception;

}
