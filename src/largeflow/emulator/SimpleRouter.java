package largeflow.emulator;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

/**
 * inbound link capacity == outbound link capacity
 * only one detector applied in the router
 * @author HaoWu
 *
 */
public class SimpleRouter implements Router {
    
    private String name;
    private Detector detector;
    
    private Map<FlowId, Integer> blackList_DroppedTraffic;

    public SimpleRouter(Detector detector) {
        blackList_DroppedTraffic = new TreeMap<>();
        this.detector = detector;
    }

    @Override
    public String name(){
        return name;
    }
    
    @Override
    public void reset() {
        detector.reset();
        blackList_DroppedTraffic = new TreeMap<>();
    }

    @Override
    public boolean processPacket(Packet packet) throws Exception {
        
        boolean packetIsBlocked = !detector.processPacket(packet);
        
        if (packetIsBlocked) {
            // accumulate the dropped traffic volume
            if (blackList_DroppedTraffic.containsKey(packet.flowId)) {
                int droppedTraffic = blackList_DroppedTraffic.get(packet.flowId);
                droppedTraffic += packet.size;
                blackList_DroppedTraffic.put(packet.flowId, droppedTraffic);
            } else {
                blackList_DroppedTraffic.put(packet.flowId, packet.size);
            }
        }
        
        return packetIsBlocked;
    }

    @Override
    public Boolean flowIsInBlackList(FlowId flowId) {
        if (detector.getBlackList().containsKey(flowId)) {
            return true;
        }

        return false;
    }

    @Override
    public Double flowBlackListTime(FlowId flowId) {
        return detector.getBlackList().get(flowId);
    }

    @Override
    public void processEnd() throws Exception {
    }

    @Override
    public void logConfig(Logger logger) throws IOException {
        logger.logConfigMsg("Router Class: " + this.getClass() + "\n");
        logger.logConfigMsg("-----Detector-----\n");
        detector.logConfig(logger);
    }

    @Override
    public void setNumOfDetectorCounters(Integer numOfCounters) throws Exception {
        if (detector != null) {
            detector.setNumOfCounters(numOfCounters);
        }
    }

    @Override
    public Map<FlowId, Double> getBlackList() {
        return detector.blackList;
    }

    @Override
    public Map<FlowId, Integer> getDroppdTrafficMap() {
        return blackList_DroppedTraffic;
    }
    
}
