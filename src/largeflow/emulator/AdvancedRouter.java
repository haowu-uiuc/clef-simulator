package largeflow.emulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;



/**
 * Rounter framework: 
 * in_a --                                      ---- QD_out_1 ----- out_1 
 *        |                                     | 
 * in_b ——                                      ---- QD_out_2 ----- out_2 
 *        |(inbound QD)----- pre-QD detector -- |  
 *        …                                     … 
 *        |                                     | 
 * in_n ——                                      ---- QD_out_m ----- out_m
 * 
 * @author HaoWu
 *
 */
public class AdvancedRouter implements Router {

    private String name;
    
    private Integer totalInboundCapacity;
    private Integer totalOutboundCapacity;
    private ArrayList<Integer> inboundCapacities;
    private ArrayList<Integer> outboundCapacities;

    private Detector preQdDetector;
    private ArrayList<Detector> postQdDetectors;
    private QueueDiscipline inboundQD;
    private List<QueueDiscipline> outboundQDs;
    private List<Queue<Packet>> outputQueues;
    private RoutingTable routingTable;
    
    private Map<FlowId, Double> blackList = null;
    private Map<FlowId, Integer> blackList_DroppedTraffic;

    // the ratio of # of detector counters to total counters
    private Map<Detector, Double> detectorToCounterRatio;
    
    /**
     * one inbound link add one outbound link
     * 
     * @param inboundCapacity
     * @param outboundCapacity
     */
    public AdvancedRouter(String name, Integer inboundCapacity,
            Integer outboundCapacity) {
        this.name = name;
        
        blackList_DroppedTraffic = new TreeMap<>();
        
        inboundCapacities = new ArrayList<>();
        outboundCapacities = new ArrayList<>();
        postQdDetectors = new ArrayList<>(1);
        detectorToCounterRatio = new HashMap<>(2);

        inboundCapacities.add(inboundCapacity);
        outboundCapacities.add(outboundCapacity);
        totalInboundCapacity = inboundCapacity;
        totalOutboundCapacity = outboundCapacity;

        postQdDetectors.add(null);

        // routing table is not needed in this case
        routingTable = null;

        // single inbound link does not need QD
        inboundQD = null;

        // Set outbound QD
        outboundQDs = new ArrayList<>();
        outboundQDs.add(new QueueDiscipline(outboundCapacity));
        
        outputQueues = new ArrayList<>();
        outputQueues.add(new LinkedList<>());
    }

    /**
     * multiple inbound links and multiple outbound links
     * 
     * @param inboundCapacities
     * @param outboundCapacities
     */
    public AdvancedRouter(String name, List<Integer> inboundCapacities,
            List<Integer> outboundCapacities,
            RoutingTable routingTable) {
        this.name = name;
        
        blackList_DroppedTraffic = new TreeMap<>();
        
        this.inboundCapacities = new ArrayList<>(inboundCapacities);
        this.outboundCapacities = new ArrayList<>(outboundCapacities);
        postQdDetectors = new ArrayList<>(outboundCapacities.size());
        detectorToCounterRatio = new HashMap<>();

        this.routingTable = routingTable;

        totalInboundCapacity = 0;
        for (Integer inboundCapacity : inboundCapacities) {
            totalInboundCapacity += inboundCapacity;
        }
        
        totalOutboundCapacity = 0;
        for (Integer outboundCapacity : outboundCapacities) {
            totalOutboundCapacity += outboundCapacity;
        }

        // set post-QD detectors as all null
        for (int i = 0; i < outboundCapacities.size(); i++) {
            this.postQdDetectors.add(null);
        }

        // set inbound QD
        if (inboundCapacities.size() > 1) {
            inboundQD = new QueueDiscipline(totalInboundCapacity);
        } else {
            inboundQD = null;
        }

        // set outbound QDs and output queues
        outboundQDs = new ArrayList<>();
        outputQueues = new ArrayList<>();
        for (int i = 0; i < outboundCapacities.size(); i++) {
            outboundQDs.add(new QueueDiscipline(outboundCapacities.get(i)));
            outputQueues.add(new LinkedList<>());
        }
    }

    @Override
    public String name(){
        return name;
    }
    
    public Integer getNumOfInboundLinks() {
        if (inboundCapacities == null) {
            return null;
        }
        return inboundCapacities.size();
    }

    public Integer getNumOfOutboundLinks() {
        if (outboundCapacities == null) {
            return null;
        }
        return outboundCapacities.size();
    }

    public Integer getTotalInboundCapacity() {
        return totalInboundCapacity;
    }
    
    public Integer getTotalOutboundCapacity() {
        return totalOutboundCapacity;
    }

    public Integer getOutboundCapacity(Integer outboundIndex) {
        return outboundCapacities.get(outboundIndex);
    }

    public void setPreQdDetector(Detector detector) {
        preQdDetector = detector;
        detectorToCounterRatio.put(detector, 1.0);
    }
    
    public void setPreQdDetector(Detector detector, double counterRatio) {
        preQdDetector = detector;
        detectorToCounterRatio.put(detector, counterRatio);
    }

    public Detector getPreQdDtector() {
        return preQdDetector;
    }
    
    /**
     * by default, we set detector to oubound link out_1.
     * 
     * @param detector
     */
    public void setPostQdDetector(Detector detector) {
        postQdDetectors.set(0, detector);
        detectorToCounterRatio.put(detector, 1.0);
    }
    
    public void setPostQdDetector(Detector detector, double counterRatio) {
        postQdDetectors.set(0, detector);
        detectorToCounterRatio.put(detector, counterRatio);
    }

    public void setPostQdDetector(int outboundLinkIndex,
            Detector detector) {
        postQdDetectors.set(outboundLinkIndex, detector);
        detectorToCounterRatio.put(detector, 1.0);
    }
    
    public void setPostQdDetector(int outboundLinkIndex,
            Detector detector, double counterRatio) {
        postQdDetectors.set(outboundLinkIndex, detector);
        detectorToCounterRatio.put(detector, counterRatio);
    }

    public Detector getPostQdDetector(Integer outboundIndex) {
        return postQdDetectors.get(outboundIndex);
    }

    public List<Detector> getAllPostQdDetectors() {
        return postQdDetectors;
    }

    @Override
    public void reset() {
        blackList = null;
        blackList_DroppedTraffic = new TreeMap<>();
        
        // reset inbound side detector
        if (preQdDetector != null) {
            preQdDetector.reset();
        }
        
        // reset inbound QD
        if (inboundQD != null) {
            inboundQD.reset();
        }
        
        // reset outbound side detectors and QDs
        for (int i = 0; i < outboundCapacities.size(); i++) {
            Detector detector = postQdDetectors.get(i);
            QueueDiscipline qd = outboundQDs.get(i);
            if (detector != null) {
                detector.reset();
            }
            
            qd.reset();
            
            outputQueues.get(i).clear();
        }
    }

    @Override
    public boolean processPacket(Packet packet) throws Exception {

        if (flowIsInBlackList(packet.flowId)) {
            // accumulate the dropped traffic volume
            if (blackList_DroppedTraffic.containsKey(packet.flowId)) {
                int droppedTraffic = blackList_DroppedTraffic.get(packet.flowId);
                droppedTraffic += packet.size;
                blackList_DroppedTraffic.put(packet.flowId, droppedTraffic);
            } else {
                blackList_DroppedTraffic.put(packet.flowId, packet.size);
            }
            
            return false;
        }

        // inbound QD
        Queue<Packet> inboundQueue = runInboundQd(packet);

        // pre-QD detector
        runPreQdDetector(inboundQueue);

        // post-QD detector
        runPostQdDetectors();

        return true;
    }
    
    @Override
    /**
     * process all packets left in the QDs
     */
    public void processEnd() throws Exception {

        // inbound QD
        if (inboundQD != null) {
            inboundQD.processEnd();
        }
        
        Queue<Packet> inboundQueue = runInboundQd(null);

        // pre-QD detector
        runPreQdDetector(inboundQueue);

        // outbound QDs
        for (QueueDiscipline qd : outboundQDs) {
            qd.processEnd();
        }
        
        // post-QD detector
        runPostQdDetectors();
        
    }
    
    private Queue<Packet> runInboundQd(Packet packet) {
        Queue<Packet> inboundQueue = new LinkedList<>();
        
        if (inboundQD != null) {
            if (packet != null) {
                inboundQD.processPacket(packet);
            }
            
            // check output queue
            Packet tmpPacket;
            while ((tmpPacket = inboundQD.getNextPacket()) != null) {
                inboundQueue.add(tmpPacket);
            }
        } else {
            if (packet != null) {
                inboundQueue.add(packet);
            }
        }
        
        return inboundQueue;
    }
    
    private void runPreQdDetector(Queue<Packet> inboundQueue) throws Exception {
        Packet tmpPacket;
        while (!inboundQueue.isEmpty()) {
            
            tmpPacket = inboundQueue.poll();
            if (preQdDetector != null){
                preQdDetector.processPacket(tmpPacket);
            }
            
            // choose outbound link
            // and input packet into outbound QD
            if (routingTable == null) {
                outboundQDs.get(0).processPacket(tmpPacket);
            } else {
                outboundQDs
                    .get(routingTable.getOutboundPortIndex(tmpPacket.flowId))
                    .processPacket(tmpPacket);
            }
            
        }
    }
    
    private void runPostQdDetectors() throws Exception {
        for (int i = 0; i < getNumOfOutboundLinks(); i++) {
            QueueDiscipline qd = outboundQDs.get(i);
            
            Packet outputPacket;
            while ((outputPacket = qd.getNextPacket()) != null) {
                if (postQdDetectors.get(i) != null) {
                    postQdDetectors.get(i).processPacket(outputPacket);
                }
                
                // For evaluation of multiple nodes, we need the output 
                // packets from each outbound link, and use them as the input
                // packets of the downstream node.
                // We can have a Packet Queue in the router and output the
                // packets into Packet Queue here.
                outputQueues.get(i).add(outputPacket);
            }
        }
    }

    @Override
    public Boolean flowIsInBlackList(FlowId flowId) {
        if (preQdDetector != null
                && preQdDetector.getBlackList().containsKey(flowId)) {
            return true;
        }

        if (routingTable == null) {
            if (postQdDetectors.get(0) != null && postQdDetectors.get(0)
                    .getBlackList().containsKey(flowId)) {
                return true;
            }
        } else {
            Detector postQdDetector = postQdDetectors
                    .get(routingTable.getOutboundPortIndex(flowId));
            if (postQdDetector != null
                    && postQdDetector.getBlackList().containsKey(flowId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Double flowBlackListTime(FlowId flowId) {

        Double time1 = null;
        Double time2 = null;
        
        if (preQdDetector != null) {
            time1 = preQdDetector.getBlackListTime(flowId);
        }

        if (routingTable == null) {
            if (postQdDetectors.get(0) != null) {
                time2 = postQdDetectors.get(0).getBlackListTime(flowId);
            }
        } else {
            Detector postQdDetector = postQdDetectors
                    .get(routingTable.getOutboundPortIndex(flowId));
            if (postQdDetector != null
                    && postQdDetector.getBlackList().containsKey(flowId)) {
                time2 = postQdDetector.getBlackListTime(flowId);
            }
        }

        if (time1 == null && time2 == null) {
            return null;
        } else if (time1 != null && time2 != null) {
            return Math.min(time1, time2);
        } else if (time1 != null){
            return time1;
        } else {
            return time2;
        }
    }
    
    @Override
    public void logConfig(Logger logger) throws IOException {
        logger.logConfigMsg("Router Class: " + this.getClass() + "\n");
        logger.logConfigMsg("-----Inbound Links-----\n");
        logger.logConfigMsg("Total Inbound Capacity: " 
                + totalInboundCapacity + " Byte/sec \n");
        for (int i = 0; i < inboundCapacities.size(); i ++) {
            logger.logConfigMsg("Capacity of Inbound Link " + i + ": " 
                    + inboundCapacities.get(i) + " Byte/sec \n");
        }
        
        logger.logConfigMsg("-----Outbound Links-----\n");
        for (int i = 0; i < outboundCapacities.size(); i ++) {
            logger.logConfigMsg("Capacity of Outbound Link " + i + ": " 
                    + outboundCapacities.get(i) + " Byte/sec \n");
        }
        
        logger.logConfigMsg("-----Pre-QD Detector-----\n");
        if (preQdDetector != null) {
           preQdDetector.logConfig(logger);
        } else {
            logger.logConfigMsg("null detector\n");
        }
        
        logger.logConfigMsg("-----Post-QD Detectors-----\n");
        for (int i = 0; i < postQdDetectors.size(); i ++) {
            logger.logConfigMsg(">>---Post-QD Detector " + i + "---\n");
            Detector detector = postQdDetectors.get(i);
            if (detector != null) {
                detector.logConfig(logger);
            } else {
                logger.logConfigMsg("null detector\n");
            }
        }
        
        logger.logConfigMsg("-----Routing Table-----\n");
        if (routingTable != null) {
            routingTable.logConfig(logger);
        } else {
            logger.logConfigMsg("0 --> 0 \n");
        }

    }

    @Override
    public void setNumOfDetectorCounters(Integer numOfCounters) throws Exception {
        for (Entry<Detector, Double> entry : detectorToCounterRatio.entrySet()) {
            Detector detector = entry.getKey();
            double ratio = entry.getValue();
            detector.setNumOfCounters((int) Math.round(numOfCounters * ratio));
        }
    }

    @Override
    public Map<FlowId, Double> getBlackList() {
        if (blackList != null) {
            return blackList;
        }
        
        blackList = new TreeMap<>();
        if (preQdDetector != null) {
            blackList.putAll(preQdDetector.blackList);
        }
        
        for (int i = 0; i < outboundCapacities.size(); i ++) {
            Detector detector = postQdDetectors.get(i);
            if (detector != null) {
                for ( Map.Entry<FlowId, Double> entry : detector.blackList.entrySet()) {
                    if (!blackList.containsKey(entry.getKey())) {
                        blackList.put(entry.getKey(), entry.getValue());
                    } else if (blackList.get(entry.getKey()) > entry.getValue()) {
                        blackList.put(entry.getKey(), entry.getValue());
                    }                    
                }
            }
        }
        
        return blackList;
    }

    @Override
    public Map<FlowId, Integer> getDroppdTrafficMap() {
        return blackList_DroppedTraffic;
    }
    
    public Integer getOutboundLinkIndex(FlowId flowId) {
        if (routingTable == null) {
            return 0;
        } else {
            return routingTable.getOutboundPortIndex(flowId);
        }
    }
    
    /**
     * get the next packets output into outbound link
     * @param outboundIndex
     * @return
     */
    public Packet getNextOutboundPacket(int outboundIndex) {
        if (outputQueues.get(outboundIndex).isEmpty()) {
            return null;
        }
        
        return outputQueues.get(outboundIndex).poll();
    }
    
    /**
     * get the all next packets output into outbound link
     * @param outboundIndex
     * @return
     */
    public List<Packet> getNextAllOutboundPacket(int outboundIndex) {
        Queue<Packet> queue = outputQueues.get(outboundIndex);
        List<Packet> packets = new ArrayList<>();
        while (!outputQueues.get(outboundIndex).isEmpty()) {
            packets.add(queue.poll());
        }
        
        return packets;
    }

}
