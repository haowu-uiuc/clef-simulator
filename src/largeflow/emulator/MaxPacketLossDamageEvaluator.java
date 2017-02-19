package largeflow.emulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import largeflow.datatype.Damage;
import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.flowgenerator.RealAttackFlowGenerator;

/**
 * Calculate the maximum damage could be caused by all attack flows, among
 * different number of counters and different attack rate
 * 
 * 1. Count both Best Effort damage and Priority damage in.
 * 2. Inbound link could be larger than the outbound link.
 * 3. Only for single router network.
 * 
 * @author HaoWu
 *
 */
public class MaxPacketLossDamageEvaluator {

    private List<AdvancedRouter> routersToEvalList;
    private Detector baseDetector; // ideal detector to indicate flow spec, e.g.
                                   // leaky bucket filter
    private RealAttackFlowGenerator flowGenerator;
    private int maxAtkRate; // bytes / second
    private int minAtkRate; // bytes / second
//    private int atkRateInterval; // bytes / second
    private int maxNumOfCounters;
    private int minNumOfCounters;
//    private int numOfCounterInterval;
    private int startRound;
    private int numOfRepeatRounds;
    private int maxPacketSize;
    private List<Integer> atkRateList;
    private List<Integer> numOfCounterList;
    
    // max number of admitted flows: 
    // always admit attack flows (for experiment purpose)
    // admit legitimate flows as much as possible
    // -1 means no limit
    private int maxNumOfAdmittedFlows = -1;
    
    private HashMap<String, Long> preQdRealTrafficMap;
    private HashMap<String, Long> postQdAttackTrafficMap;
    private HashMap<String, Long> postQdRealTrafficMap;
    private HashMap<String, Long> blockedRealTrafficMap;
    private HashMap<String, Long> preQdAttackResvMap;   
        // {reservation bandwidth} * (t_block - t_start)
    
    private Logger logger;

    public MaxPacketLossDamageEvaluator() {
        routersToEvalList = new ArrayList<>();
        maxAtkRate = -1;
        minAtkRate = -1;
        maxNumOfCounters = -1;
        minNumOfCounters = -1;
        startRound = 0;
        numOfRepeatRounds = 1;
        this.maxPacketSize = NetworkConfig.maxPacketSize;
        
        postQdAttackTrafficMap = new HashMap<>();
        postQdRealTrafficMap = new HashMap<>();
        blockedRealTrafficMap = new HashMap<>();
        preQdAttackResvMap = new HashMap<>();
        preQdRealTrafficMap = new HashMap<>();
    }

    @Deprecated
    public MaxPacketLossDamageEvaluator(int maxAtkRate,
            int minAtkRate,
            int atkRateInterval,
            int maxNumOfCounters,
            int minNumOfCounters,
            int numOfCounterInterval) throws Exception {
        this.maxPacketSize = NetworkConfig.maxPacketSize;
        routersToEvalList = new ArrayList<>();
        configAtkRate(maxAtkRate, minAtkRate, atkRateInterval);
        configNumOfCounters(maxNumOfCounters,
                minNumOfCounters,
                numOfCounterInterval);
        
        postQdAttackTrafficMap = new HashMap<>();
        postQdRealTrafficMap = new HashMap<>();
        blockedRealTrafficMap = new HashMap<>();
        preQdAttackResvMap = new HashMap<>();
        preQdRealTrafficMap = new HashMap<>();
    }
    
    public MaxPacketLossDamageEvaluator(List<Integer> atkRateList,
            List<Integer> numOfCounterList) throws Exception {
        this.maxPacketSize = NetworkConfig.maxPacketSize;
        routersToEvalList = new ArrayList<>();
        this.atkRateList = atkRateList;
        this.numOfCounterList = numOfCounterList;
        maxAtkRate = atkRateList.get(atkRateList.size() - 1);
        minAtkRate = atkRateList.get(0);
        maxNumOfCounters = numOfCounterList.get(numOfCounterList.size() - 1);
        minNumOfCounters = numOfCounterList.get(0);
        
        postQdAttackTrafficMap = new HashMap<>();
        postQdRealTrafficMap = new HashMap<>();
        blockedRealTrafficMap = new HashMap<>();
        preQdAttackResvMap = new HashMap<>();
        preQdRealTrafficMap = new HashMap<>();
    }

    public void configAtkRate(int max,
            int min,
            int interval) {
        atkRateList = new ArrayList<>();
        for (int rate = min; rate <= max; rate += interval) {
            atkRateList.add(rate);
        }
        this.maxAtkRate = max;
        this.minAtkRate = min;
    }

    public void configNumOfCounters(int max,
            int min,
            int interval) {
        numOfCounterList = new ArrayList<>();
        for (int num = min; num <= max; num += interval) {
            numOfCounterList.add(num);
        }
        this.maxNumOfCounters = max;
        this.minNumOfCounters = min;
    }

    public void setMaxNumOfAdmittedFlows(int max) {
        maxNumOfAdmittedFlows = max;
    }
    
    public void setStartRound(int startRound) throws Exception {
        if (startRound < 0) {
            throw new Exception("start round cannot be negative!");
        }
        this.startRound = startRound;
    }
    
    public void setNumOfRepeatRounds(int numOfRepeatRounds) throws Exception {
        if (numOfRepeatRounds <= 0) {
            throw new Exception(
                    "numOfRepeatRounds cannot be lower or equal to zero!");
        }
        this.numOfRepeatRounds = numOfRepeatRounds;
    }

    public int getNumOfRepeatRounds() {
        return numOfRepeatRounds;
    }

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public int getLinkCapacity() {
        return flowGenerator.getLinkCapacity();
    }

    public int getPriorityLinkCapacity() {
        return flowGenerator.getPriorityLinkCapacity();
    }

    public void setBaseDetector(Detector baseDetector) {
        this.baseDetector = baseDetector;
    }

    public void addRouter(AdvancedRouter router) {
        routersToEvalList.add(router);
        postQdAttackTrafficMap.put(router.name(), (long)0);
        postQdRealTrafficMap.put(router.name(), (long)0);
        blockedRealTrafficMap.put(router.name(), (long)0);
    }

    public void setFlowGenerator(RealAttackFlowGenerator flowGenerator) {
        this.flowGenerator = flowGenerator;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * run the evaluator and return a result map: {detector name} -> result
     * 
     * @return
     * @throws Exception
     */
    public void run() throws Exception {
        if (baseDetector == null) {
            throw new Exception("Please set base detector in the evaluator");
        }

        if (routersToEvalList.isEmpty()) {
            throw new Exception(
                    "No detector to evaluate, please add detectors");
        }

        if (flowGenerator == null) {
            throw new Exception("Please set flow generator in the evaluator");
        }
        
        if (logger == null) {
            throw new Exception("Please set logger, or the run is wasted");
        }
        
        Set<FlowId> nonAdmittedFlows = new HashSet<>();
        if (maxNumOfAdmittedFlows > 0) {
            // need to have a non-admitted flow set:
            int numOfNonAdmitted = flowGenerator.getNumOfFlows() 
                    - maxNumOfAdmittedFlows;
            int count = 0;
            Random rand = new Random();
            while (count < numOfNonAdmitted) {
                int r = rand.nextInt(flowGenerator.getNumOfFullRealFlows() 
                        + flowGenerator.getNumOfUnderUseRealFlows()) + 1
                        + flowGenerator.getNumOfAttFlows();
                FlowId fid = new FlowId(r);
                if (!nonAdmittedFlows.contains(fid)) {
                    nonAdmittedFlows.add(fid);
                    count++;
                }
            }
            System.out.println("Flows in waiting list: " + nonAdmittedFlows);
        }

        // log configs
        logger.logTestConfig(maxAtkRate,
                minAtkRate,
                maxNumOfCounters,
                minNumOfCounters,
                numOfRepeatRounds,
                maxPacketSize,
                flowGenerator.getLinkCapacity());
        logger.logFlowGeneratorConfig(flowGenerator);
        logger.logDetectorConfig(baseDetector, true);
        for (Router router : routersToEvalList) {
            logger.logRouterConfig(router);
        }
        logger.flush();

        for (int round = startRound; round < numOfRepeatRounds + startRound; round++) {
            flowGenerator.reset();  // re-generate real/legitimate traffic in each round.
            // without reset(), the flowGenerator will reuse the legitimate flows generated
            // in the last time.
            
            int numOfLargeFlows = flowGenerator.getNumOfAttFlows();
            Map<String, Damage> baselineDamageMap = new HashMap<>();
            
            int i = 0;
            for (int atkRateIndex = -1; atkRateIndex < atkRateList.size(); atkRateIndex++) {
                int atkRate = 0;
                if (atkRateIndex == -1) {
                    // calculate the baseline with zero attack flows
                    // see the traffic dropped because of congestion of legitimate traffic
                    flowGenerator.setNumOfAttFlows(0);
                    flowGenerator.setAttackRate(atkRate);
                    flowGenerator.generateFlows();
                } else {
                    // restore the number of large flows
                    flowGenerator.setNumOfAttFlows(numOfLargeFlows);
                    atkRate = atkRateList.get(atkRateIndex);
                    flowGenerator.setAttackRate(atkRate);
                    flowGenerator.generateFlows();
                }
                
                PacketReader packetReader = PacketReaderFactory
                        .getPacketReader(flowGenerator.getOutputFile());

                boolean baseDetectorLogged = false;
                
                // create subloggers
                Map<String, SubLogger> subLoggerMap = new HashMap<>();
                if (atkRateIndex != -1) {
                    for (Router router : routersToEvalList) {
                        subLoggerMap.put(router.name(), logger.getSubLogger(router, round, atkRate));
                    }
                }
                
                for (int counterIndex = 0; counterIndex < numOfCounterList.size(); counterIndex++) {
                    i++;
                    int numOfCounters = numOfCounterList.get(counterIndex);
                    System.out.println(round + "." + i + "\tRate: " + atkRate
                            + "\tCounter: " + numOfCounters);

                    try {
                        Map<String, Set<FlowId>> nonAdmittedFlowsMap =
                                new HashMap<>();
                        for (Router router : routersToEvalList) {
                            router.setNumOfDetectorCounters(numOfCounters);
                            
                            // init non admit flow set
                            nonAdmittedFlowsMap.put(router.name(),
                                    new HashSet<>(nonAdmittedFlows));
                            
                            Set<FlowId> set = nonAdmittedFlowsMap.get(router.name());
                            int delta = numOfLargeFlows - flowGenerator.getNumOfAttFlows();
                            for (int k = 0; k < delta; k++) {
                                // TODO: for baseline round, we remove flows from
                                // the nonAdmittedFlows
                                set.remove(set.iterator().next());
                            }
                            if (delta > 0) {
                                for (FlowId fid: set) {
                                    fid.set(fid.getIntegerValue() - delta);
                                }
                            }
                        }
                        
                        // reset routers and base detector
                        packetReader.rewind();
                        baseDetector.reset();
                        for(Router router : routersToEvalList){
                            router.reset();
                        }
                        resetTrafficMap();
                        
                        // run routers and base detector
                        Packet packet;
                        while((packet = packetReader.getNextPacket()) != null){
                            baseDetector.processPacket(packet);
                            for(AdvancedRouter router : routersToEvalList){
                                Set<FlowId> nonAdFlows = 
                                        nonAdmittedFlowsMap.get(router.name());
                                if (nonAdFlows.contains(packet.flowId)) {
                                    // if the flow is in the non admitted set, skip
                                    continue;
                                }
                                
                                int preBlSize = router.getBlackList().size();
                                
                                // packet process
                                boolean isBlocked = !router.processPacket(packet);
                                int postBlSize = router.getBlackList().size();
                                int moreToAdmit = postBlSize - preBlSize;
                                while (moreToAdmit > 0 && !nonAdFlows.isEmpty()) {
                                    FlowId fid = nonAdFlows.iterator().next();
                                    nonAdFlows.remove(fid);
                                    moreToAdmit--;
                                    // System.out.println(router.name() + " admit flow " 
                                    //         + fid + " at " + packet.time);
                                }
                                
                                if (! flowGenerator.isLargeFlow(packet.flowId)) {
                                    preQdRealTrafficMap.put(router.name(), 
                                            preQdRealTrafficMap.get(router.name()) + packet.size);
                                    if (isBlocked) {                                    
                                        // accumulate the blocked real traffic (legitimate traffic), 
                                        // i.e. FP traffic
                                        long value = blockedRealTrafficMap.get(router.name());
                                        blockedRealTrafficMap.put(router.name(), value + packet.size);
                                    }
                                }
                                accumulateOutputTraffic(router);
                            }
                        }
                        
                        for(AdvancedRouter router : routersToEvalList){
                            router.processEnd();
                            accumulateOutputTraffic(router);
                            calculateAtkReservationVolume(router);
                        }
                        
                        packetReader.close();
                        
                        // calculate the damage for each detector and log it
                        for (AdvancedRouter router : routersToEvalList) {
                            // Calculate the damage for each router
                            PacketLossDamageCalculator damageCalculator = 
                                    new PacketLossDamageCalculator(baseDetector, router, flowGenerator);
                            
                            Damage damage = damageCalculator.getMeasuredDamage(
                                    preQdAttackResvMap.get(router.name()),
                                    preQdRealTrafficMap.get(router.name()),
                                    postQdAttackTrafficMap.get(router.name()),
                                    postQdRealTrafficMap.get(router.name()),
                                    blockedRealTrafficMap.get(router.name()),
                                    router.getTotalOutboundCapacity());
                            
                            if (atkRateIndex == -1) {
                                // for baseline
                                baselineDamageMap.put(router.name(), damage);
                            } else {
                                // log blacklist into different files for differen {round, atkRate, # of counters}
                                logger.logRouterBlackList(router, atkRate, numOfCounters, round, baseDetector);
                                
                                // set baseline damage
                                damage.baseline_damage = 
                                        baselineDamageMap.get(router.name()).QD_drop_damage;
                                
                                // log the traffic and damage into different files for different {round, atkRate}
                                subLoggerMap.get(router.name()).logDamageAndTraffic(router,
                                        atkRate,
                                        numOfCounters,
                                        round,
                                        preQdAttackResvMap.get(router.name()),
                                        preQdRealTrafficMap.get(router.name()),
                                        postQdAttackTrafficMap.get(router.name()),
                                        postQdRealTrafficMap.get(router.name()),
                                        blockedRealTrafficMap.get(router.name()),
                                        router.getTotalOutboundCapacity(),
                                        damage);
                                subLoggerMap.get(router.name()).flush();
                            }
                            
                            /*
                            // log the traffic into one file
                            logger.logRouterTotalTrafficVolume(router,
                                    atkRate,
                                    numOfCounters,
                                    round,
                                    preQdAttackResvMap.get(router.name()),
                                    preQdRealTrafficVolume,
                                    postQdAttackTrafficMap.get(router.name()),
                                    postQdRealTrafficMap.get(router.name()),
                                    blockedRealTrafficMap.get(router.name()),
                                    router.getTotalOutboundCapacity());
                            
                            // log the damage into one file
                            logger.logRouterDamage(round, router, atkRate, numOfCounters, damage);
                            */
                        }
                        
                        if (!baseDetectorLogged) {
                            // we only log the blacklist from base detector once to avoid duplication
                            logger.logBaseDetectorBlackList(baseDetector, atkRate, round);
                            baseDetectorLogged = true;
                        }
                        
                        logger.flush();
                    } catch (Exception e) {
                        System.out.println(e.toString());
                    }
                    
                    // if this is for baseline, we only need to test one round:
                    if (atkRateIndex == -1) {
                        break;
                    }
                }
                
                // close and clear subloggers
                if (atkRateIndex != -1) {
                    for (Router router : routersToEvalList) {
                        subLoggerMap.get(router.name()).close();
                    }
                    subLoggerMap.clear();
                }
                
                System.gc();
            }
        }

        logger.close();
    }
    
    private void accumulateOutputTraffic(AdvancedRouter router) {
        Packet nextPacket;
        for (int j = 0; j < router.getNumOfOutboundLinks(); j++) {
            while ((nextPacket = router.getNextOutboundPacket(j)) != null) {
                if (flowGenerator.isLargeFlow(nextPacket.flowId)) {
                    long newValue = postQdAttackTrafficMap.get(router.name()) + nextPacket.size;
                    postQdAttackTrafficMap.put(router.name(), newValue);
                } else {
                    long newValue = postQdRealTrafficMap.get(router.name()) + nextPacket.size;
                    postQdRealTrafficMap.put(router.name(), newValue);
                }
            }
        }
    }
        
    private void resetTrafficMap() {
        for (AdvancedRouter router : routersToEvalList) {
            postQdAttackTrafficMap.put(router.name(), (long) 0);
            postQdRealTrafficMap.put(router.name(), (long) 0);
            preQdAttackResvMap.put(router.name(), (long) 0);
            blockedRealTrafficMap.put(router.name(), (long) 0);
            preQdRealTrafficMap.put(router.name(), (long) 0);
        }
    }
    
    private void calculateAtkReservationVolume(AdvancedRouter router) {
        long volume = 0;
        for (FlowId flowId : flowGenerator.getAttackFlowIdSet()) {
            // for every TP which is large flow
            if (!flowGenerator.isLargeFlow(flowId)) {
                continue;
            }
            
            Double startTime = flowGenerator.getStartTimeOfLargeFlow(flowId);
            Double blockTime = router.getBlackList().get(flowId);
            
            if (blockTime == null) {
                // set the end of simulation interval 
                // as the block time for now
                blockTime = flowGenerator.getTraceLength();
            }
            
            volume += (blockTime - startTime) * flowGenerator.getPerFlowReservation(); 
        }
        preQdAttackResvMap.put(router.name(), volume);
    }

}
