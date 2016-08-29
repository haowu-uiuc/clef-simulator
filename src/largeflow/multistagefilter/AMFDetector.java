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

        Boolean shouldBlackList = true;
        
        for (int i = 0; i < numOfStages; i++) {
            // get the buckets of the flow
            Integer index = hashFuncs.get(i).getHashCode(packet.flowId);

            // process packet into its buckets
            Bucket bucket = stages.get(i).get(index);
            bucket.processPacket(packet);
            if (!bucket.check()) {
                shouldBlackList = false;
            }
        }
        
        if (shouldBlackList) {
            Double blackListTime = packet.time
                    + (double) packet.size / (double) linkCapacity;
            flowsToFM.put(packet.flowId, blackListTime);
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
