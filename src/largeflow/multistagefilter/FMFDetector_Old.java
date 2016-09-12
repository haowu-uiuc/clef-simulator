package largeflow.multistagefilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Logger;
import largeflow.emulator.NetworkConfig;
import largeflow.utils.GenericUtils;

/**
 * The FMF without optimization.
 * Check the flows in the bucket at the end of the period
 * No shielding
 * No conservative updating
 * @author HaoWu
 *
 */
public class FMFDetector_Old extends MultistageFilter {

    private Double T; // period T for periodic check
    private Integer threshold; // threshold of bucket
    private Integer currentPeriod; // current period number (= 0, 1, 2, ...)
    private boolean considerPacketCrossPeriods;

    public FMFDetector_Old(String detectorName,
            Integer numOfStages,
            Integer sizeOfStage,
            Integer linkCapacity,
            Double T,
            Integer threshold) throws Exception {
        super(detectorName,
                numOfStages,
                sizeOfStage,
                linkCapacity);
        this.T = T;
        this.threshold = threshold;

        int maxPacketSize = NetworkConfig.maxPacketSize;
        if (T < maxPacketSize / linkCapacity) {
            throw new Exception(
                    "Period T has to be larger than maxPacketSize / linkCapacity .");
        }

        initState();

        // default to consider packet across periods.
        considerPacketCrossPeriods = true;
    }

    @Override
    public void reset() {
        super.reset();
        initState();
    }

    @Override
    public void setNumOfCounters(Integer numOfCounters) throws Exception {
        super.setNumOfCounters(numOfCounters);
        initState();
    }

    /**
     * return false: the flow of the packet is already in the blacklist; return
     * true: otherwise
     */
    @Override
    public Map<FlowId, Double> processPacketInMultistage(Packet packet)
            throws Exception {
        Map<FlowId, Double> flowsToFM = new HashMap<>();

        // check whether the flow is already in the blacklist
        if (blackList.containsKey(packet.flowId)) {
            return flowsToFM;
        }

        double packetStartTime = packet.time;
        double packetEndTime = packet.time
                + (double) packet.size / (double) linkCapacity;
        int startPeriod = (int) Math.floor(packetStartTime / T);
        int endPeriod = (int) Math.floor(packetEndTime / T);

        int lastPeriod = currentPeriod;
        currentPeriod = startPeriod;

        if (lastPeriod < currentPeriod) {
            // if currentPeriod is larger than lastPeriod,
            // check the buckets and reset buckets
            flowsToFM.putAll(checkAllFlows((lastPeriod + 1) * T));
            resetBucketList();
            if (flowsToFM.containsKey(packet.flowId)) {
                return flowsToFM;
            }
        }

        if (startPeriod == endPeriod || !considerPacketCrossPeriods) {
            // if the packet does not cross two periods
            // PS: if considerPacketCrossPeriods == true,
            // then we will ignore the "else" branch anyway.
            processPacketForAllStages(packet);

        } else {
            // if the packet cross two periods
            // only add the amount of the packet in the current period for now;
            // check the buckets + reset buckets;
            // and then add the other part of the packet into the next period;

            int nextPeriodPacketAmount = (int) Math
                    .round(packet.size * (packetEndTime - T * endPeriod)
                            / (packetEndTime - packetStartTime));

            // add the amount of the packet in the current period
            Packet firstHalfPacket = new Packet(packet.flowId,
                    packet.size - nextPeriodPacketAmount,
                    packet.time);
            processPacketForAllStages(firstHalfPacket);

            // check the bucket value,
            // and include flows return into flows sending to flow memory
            // reset the bucket, and add nextPeriodPacketAmount
            Map<FlowId, Double> flowsPassedMF = checkAllFlows(
                    (currentPeriod + 1) * T);
            GenericUtils.addAllNewEntriesIntoMap(flowsToFM, flowsPassedMF);
            resetBucketList();

            currentPeriod = endPeriod;

            if (flowsToFM.containsKey(packet.flowId)) {
                return flowsToFM;
            }

            // add the other part of the packet into the next period
            Packet secondHalfPacket = new Packet(packet.flowId,
                    nextPeriodPacketAmount,
                    packet.time);
            processPacketForAllStages(secondHalfPacket);
        }

        return flowsToFM;
    }

    private void initState() {
        stages = new ArrayList<>();
        currentPeriod = 0;

        for (int i = 0; i < numOfStages; i++) {
            List<Bucket> tmpStage = new ArrayList<>();
            for (int j = 0; j < sizeOfStage; j++) {
                tmpStage.add(new Bucket(threshold));
            }
            stages.add(tmpStage);
        }
    }

    private void resetBucketList() {
        for (int i = 0; i < stages.size(); i++) {
            List<Bucket> tmpStage = stages.get(i);
            for (int j = 0; j < tmpStage.size(); j++) {
                tmpStage.get(j).value = 0;
            }
        }
    }

    public Double getT() {
        return T;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void notConsiderPacketCrossPerids() {
        considerPacketCrossPeriods = false;
    }

    public void considerPacketCrossPeriods() {
        considerPacketCrossPeriods = true;
    }

    @Override
    public void logConfig(Logger logger) throws IOException {
        super.logConfig(logger);
        logger.logConfigMsg("Period: " + getT() + " sec \n");
        logger.logConfigMsg("Bucket threshold: " + getThreshold() + " Byte \n");
        logger.logConfigMsg("Consider packets crossing periods: "
                + considerPacketCrossPeriods + "\n");
    }

    private void processPacketForAllStages(Packet packet) {
        for (int i = 0; i < numOfStages; i++) {
            // get the buckets of the flow
            Integer index = hashFuncs.get(i).getHashCode(packet.flowId);

            // process packet into its buckets
            Bucket bucket = stages.get(i).get(index);
            bucket.processPacket(packet);
        }
    }

    private Map<FlowId, Double> checkAllFlows(double checkTime) {
        Map<FlowId, Double> flowsToFM = new HashMap<>();

        for (int i = 0; i < sizeOfStage; i++) {
            if (!stages.get(0).get(i).check()) {
                continue;
            }

            List<FlowId> flowIds = hashFuncs.get(0).getKeys(i);
            for (FlowId flowId : flowIds) {
                if (blackList.containsKey(flowId) || (flowMemory != null
                        && flowMemory.flowIsInFlowMemory(flowId))) {
                    // if the flow is already in blacklist,
                    // or flow memory, then skip
                    continue;
                }

                boolean passedMultistageFilter = true;
                for (int j = 0; j < numOfStages; j++) {
                    int bucketIndex = hashFuncs.get(j).getHashCode(flowId);
                    if (!stages.get(j).get(bucketIndex).check()) {
                        passedMultistageFilter = false;
                        break;
                    }
                }

                if (passedMultistageFilter) {
                    // put flows into flow memory the flowId
                    flowsToFM.put(flowId, checkTime);
                }

            }
        }

        return flowsToFM;
    }

}
