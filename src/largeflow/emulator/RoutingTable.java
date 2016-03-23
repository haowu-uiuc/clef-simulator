package largeflow.emulator;

import java.io.IOException;
import java.util.Map;

import largeflow.datatype.FlowId;

public class RoutingTable {
    
    private Map<FlowId, Integer> mapFlowIdToPort;
    
    public RoutingTable() {
        //TODO : implement constructor please.
    }
    
    public Integer getOutboundPortIndex(FlowId flowId) {
        //TODO : finish this method
        return mapFlowIdToPort.get(flowId);
    }
    
    public void logConfig(Logger logger) throws IOException {
        // TODO :
        logger.logConfigMsg("To be completed...");
    }
}
