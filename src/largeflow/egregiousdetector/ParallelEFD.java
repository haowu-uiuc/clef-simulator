package largeflow.egregiousdetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.Logger;

public class ParallelEFD extends Detector {

    private List<EgregiousFlowDetector> efds;
    private Integer outboundLinkCapacity; // Byte / sec
    private double twin_EFD_Tc2_adjust = 1.0;
    private boolean isTwinEFD = false;
    
    public ParallelEFD(String detectorName,
            Integer outboundLinkCapacity) {
        super(detectorName);
        efds = new ArrayList<>();
        this.outboundLinkCapacity = outboundLinkCapacity;
    }
    
    public void addEFD(EgregiousFlowDetector efd) {
        efds.add(efd);
    }
    
    public void setTwinEFD() {
        isTwinEFD = true;
    }
    
    public void setTwinEFDTc2Adjust(Double twin_EFD_Tc2_adjust) {
        this.twin_EFD_Tc2_adjust = twin_EFD_Tc2_adjust;
    }
    
    @Override
    public void reset() {
        super.reset();
        for (EgregiousFlowDetector efd : efds) {
            efd.reset();
        }
    }

    @Override
    public boolean processPacket(Packet packet) throws Exception {
        if (efds.isEmpty()) {
            System.out.println("WARN: no efd in ParallelEFD " + detectorName);
            return true;
        }
           
        // check black list in each efd
        if (blackList.containsKey(packet.flowId)) {
            return false;
        }
        
        // process packet in each efd
        for (EgregiousFlowDetector efd : efds) {            
            if (!efd.processPacket(packet)) {
                blackList.put(packet.flowId, packet.time);
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void setNumOfCounters(Integer numOfCounters) throws Exception {
        // equally assign the counter to each EFD
        int m = numOfCounters / efds.size();
        for (EgregiousFlowDetector efd : efds) {
            efd.setNumOfCounters(m);
        }
        if (isTwinEFD && efds.size() == 2) {
            // twin efd
            double Tc1 = efds.get(0).getPeriod();
            double Tc2 = efds.get(0).getTwinEFDHighPeriod(outboundLinkCapacity)
                    * twin_EFD_Tc2_adjust;
            efds.get(1).setPeriod(Tc2);
            System.out.println(">>>Set Twin EFD periods: Tc1 = " + Tc1 + ", Tc2 = " + Tc2);
        }
    }
    
    @Override
    public void logConfig(Logger logger) throws IOException {
        super.logConfig(logger);
        for (EgregiousFlowDetector efd : efds) {
            logger.logConfigMsg("---Sub-EFD Name: " + efd.name() + "---\n");
            efd.logConfig(logger);;
        }
    }

}
