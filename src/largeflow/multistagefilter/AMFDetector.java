package largeflow.multistagefilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Logger;

public class AMFDetector extends MultistageFilter {

    private Integer drainRate;
    private Integer threshold;

    public AMFDetector(String detectorName,
            Integer numOfStages,
            Integer sizeOfStage,
            Integer linkCapacity,
            Integer drainRate,
            Integer threshold) {
        super(detectorName,
                numOfStages,
                sizeOfStage,
                linkCapacity);

        this.drainRate = drainRate;
        this.threshold = threshold;

        initState();
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

    @Override
    public Map<FlowId, Double> processPacketInMultistage(Packet packet)
            throws Exception {
        Map<FlowId, Double> flowsToFM = new HashMap<>();
        
        // check whether the flow is already in the blacklist
        if (blackList.containsKey(packet.flowId)) {
            return flowsToFM;
        }

        // Sheilding: don't add packet if the flow is sent to blacklist
        if (checkFlowWithShielding(packet)) {
            // blacklist the flow
            Double blackListTime = packet.time
                    + (double) packet.size / (double) linkCapacity;
            flowsToFM.put(packet.flowId, blackListTime);
        } else {
            processPacketForAllStages(packet);
        }
        
        return flowsToFM;
    }

    private void initState() {
        stages = new ArrayList<>();
        for (int i = 0; i < numOfStages; i++) {
            List<Bucket> tmpStage = new ArrayList<>();
            for (int j = 0; j < sizeOfStage; j++) {
                tmpStage.add(new LeakyBucket(threshold,
                        drainRate,
                        linkCapacity));
            }
            stages.add(tmpStage);
        }
    }

    private void processPacketForAllStages(Packet packet) {
        // get bucket indexes in each stage
        int[] bucketIndexes = new int[numOfStages];
        for (int i = 0; i < numOfStages; i++) {
            bucketIndexes[i] = hashFuncs.get(i).getHashCode(packet.flowId);
        }
        
        // update the bucket value to the packet start time
        for (int i = 0; i < numOfStages; i++) {
            Bucket bucket = stages.get(i).get(bucketIndexes[i]);
            bucket.processPacket(new Packet(packet.flowId, 0, packet.time));
        }
        
        // find the min-value bucket
        Bucket minBucket = stages.get(0).get(bucketIndexes[0]);
        int minStageIndex = 0;
        for (int i = 1; i < numOfStages; i++) {
            Bucket curBucket = stages.get(i).get(bucketIndexes[i]);
            if (curBucket.getValue() < minBucket.getValue()) {
                minBucket = curBucket;
                minStageIndex = i;
            }
        }
        
        // process the packet in the min-value bucket
        minBucket.processPacket(packet);
        
        // if the bucket value less than min-value bucket value,
        // just set the same end time and value to the bucket
        // if not just set the same end time, but keep the value the same
        for (int i = 0; i < numOfStages; i++) {
            if (i == minStageIndex) {
                continue;
            }

            // process packet into its buckets
            Bucket bucket = stages.get(i).get(bucketIndexes[i]);
            
             // process other bucket with empty packet to update time 
            // to the same as the min-value bucket
            bucket.processPacket(new Packet(
                    packet.flowId, 0, 
                    ((LeakyBucket) minBucket).getCurrenTime()));
            if (bucket.getValue() < minBucket.getValue()) {
                bucket.copyFrom(minBucket);
            }
        }
    }
    
    public Integer getDrainRate() {
        return drainRate;
    }

    public Integer getThreshold() {
        return threshold;
    }

    @Override
    public void logConfig(Logger logger) throws IOException {
        super.logConfig(logger);
        logger.logConfigMsg("Leaky bucket drain rate: " + getDrainRate()
                + " Byte / sec \n");
        logger.logConfigMsg("Bucket threshold: " + getThreshold() + " Byte \n");
    }    
}
