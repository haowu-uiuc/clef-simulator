package largeflow.emulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import largeflow.datatype.Damage;
import largeflow.datatype.Packet;

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
    private int atkRateInterval; // bytes / second
    private int maxNumOfCounters;
    private int minNumOfCounters;
    private int numOfCounterInterval;
    private int numOfRepeatRounds;
    private int maxPacketSize;
    // private int linkCapacity; // = priorityLinkCapacity +
    // bestEffortLinkCapacity
    // private int priorityLinkCapacity;
    // private int bestEffortLinkCapacity;
    
    private Long preQdRealTrafficVolume;
    private HashMap<String, Long> postQdAttackTrafficMap;
    private HashMap<String, Long> postQdRealTrafficMap;
    
    private Logger logger;

    public MaxPacketLossDamageEvaluator() {
        routersToEvalList = new ArrayList<>();
        maxAtkRate = -1;
        minAtkRate = -1;
        atkRateInterval = -1;
        maxNumOfCounters = -1;
        minNumOfCounters = -1;
        numOfCounterInterval = -1;
        numOfRepeatRounds = 1;
        this.maxPacketSize = NetworkConfig.maxPacketSize;
        
        postQdAttackTrafficMap = new HashMap<>();
        postQdRealTrafficMap = new HashMap<>();
        preQdRealTrafficVolume = (long)0;
    }

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
        preQdRealTrafficVolume = (long)0;
    }

    public void configAtkRate(int max,
            int min,
            int interval) {
        maxAtkRate = max;
        minAtkRate = min;
        atkRateInterval = interval;
    }

    public void configNumOfCounters(int max,
            int min,
            int interval) {
        maxNumOfCounters = max;
        minNumOfCounters = min;
        numOfCounterInterval = interval;
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

    public int getBestEffortLinkCapacity() {
        return flowGenerator.getBestEffortLinkCapacity();
    }

    public void setBaseDetector(Detector baseDetector) {
        this.baseDetector = baseDetector;
    }

    public void addRouter(AdvancedRouter router) {
        routersToEvalList.add(router);
        postQdAttackTrafficMap.put(router.name(), (long)0);
        postQdRealTrafficMap.put(router.name(), (long)0);
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

        // log configs
        logger.logTestConfig(maxAtkRate,
                minAtkRate,
                atkRateInterval,
                maxNumOfCounters,
                minNumOfCounters,
                numOfCounterInterval,
                numOfRepeatRounds,
                maxPacketSize,
                flowGenerator.getLinkCapacity());
        logger.logFlowGeneratorConfig(flowGenerator);
        logger.logDetectorConfig(baseDetector, true);
        for (Router router : routersToEvalList) {
            logger.logRouterConfig(router);
        }
        logger.flush();

        for (int round = 0; round < numOfRepeatRounds; round++) {
            for (Router router : routersToEvalList) {
                logger.logRouterDamage(router,
                        "Round # " + round + "\n");                
            }

            int i = 0;
            for (int atkRate = minAtkRate; 
                    atkRate <= maxAtkRate; 
                    atkRate += atkRateInterval) {
                flowGenerator.setAttackRate(atkRate);
                flowGenerator.generateFlows();
                preQdRealTrafficVolume = flowGenerator.getRealTrafficVolume();
                
                PacketReader packetReader = PacketReaderFactory
                        .getPacketReader(flowGenerator.getOutputFile());

                boolean baseDetectorLogged = false;
                
                for (int numOfCounters = minNumOfCounters; 
                        numOfCounters <= maxNumOfCounters; 
                        numOfCounters += numOfCounterInterval) {
                    i++;
                    System.out.println(round + "." + i + "\tRate: " + atkRate
                            + "\tCounter: " + numOfCounters);

//                    try {
                        for (Router router : routersToEvalList) {
                            router.setNumOfDetectorCounters(numOfCounters);
                        }
                        
                        // reset routers and base detector
                        packetReader.rewind();
                        baseDetector.reset();
                        for(Router router : routersToEvalList){
                            router.reset();
                        }
                        resetOutputTraffic();
                        
                        // run routers and base detector
                        Packet packet;
                        while((packet = packetReader.getNextPacket()) != null){
                            baseDetector.processPacket(packet);
                            for(AdvancedRouter router : routersToEvalList){
                                router.processPacket(packet);
                                accumulateOutputTraffic(router);
                            }
                        }
                        
                        for(AdvancedRouter router : routersToEvalList){
                            router.processEnd();
                            accumulateOutputTraffic(router);
                        }
                        
                        packetReader.close();
                        
                        // calculate the damage for each detector and log it
                        for (AdvancedRouter router : routersToEvalList) {
                            // Calculate the damage for each router
                            PacketLossDamageCalculator damageCalculator = new PacketLossDamageCalculator();
                            Damage damage = damageCalculator.getDamage(baseDetector, router, flowGenerator);
                            damage = new Damage();

                            logger.logRouterDamage(router, atkRate, numOfCounters, damage);
                            logger.logRouterBlackList(router, atkRate, numOfCounters, round);
                        }
                        
                        if (!baseDetectorLogged) {
                            // we only log the blacklist from base detector once to avoid duplication
                            logger.logBaseDetectorBlackList(baseDetector, atkRate, round);
                            baseDetectorLogged = true;
                        }
                        
                        logger.flush();
//                    } catch (Exception e) {
//                        System.out.println(e.toString());
//                    }
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
                if (flowGenerator.isLargeFlow(nextPacket.flowId) ||
                        flowGenerator.isBurstFlow(nextPacket.flowId)) {
                    long newValue = postQdAttackTrafficMap.get(router.name()) + nextPacket.size;
                    postQdAttackTrafficMap.put(router.name(), newValue);
                } else {
                    long newValue = postQdRealTrafficMap.get(router.name()) + nextPacket.size;
                    postQdRealTrafficMap.put(router.name(), newValue);
                }
            }
        }
    }
    
    private void resetOutputTraffic() {
        for (AdvancedRouter router : routersToEvalList) {
            postQdAttackTrafficMap.put(router.name(), (long) 0);
            postQdRealTrafficMap.put(router.name(), (long) 0);
        }
    }

}
