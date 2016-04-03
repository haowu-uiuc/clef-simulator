package largeflow.flowgenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.Logger;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;
import largeflow.emulator.PacketWriter;
import largeflow.emulator.QueueDiscipline;

public class RealTrafficFlowGenerator extends FlowGenerator {

    private Double compactTimes = 1.0; // compact the real traffic from [0,
                                       // intervalTime * compactTimes] into [0,
                                       // intervalTime]
    private File realTrafficFile;

    private Set<FlowId> flowIdSet = new TreeSet<>();
    private Integer numOfRealFlows = 0;
    private Integer aveRealTrafficRate = 0;
    private Long totalRealTrafficVolume = (long) 0;
    
    private Detector baseDetectorForFilter = null;
    private LeakyBucketDetector baseDetectorFlowShaper = null;
    private int startFlowId_int = Integer.MAX_VALUE;
    
    public RealTrafficFlowGenerator(Integer linkCapacity,
            Integer timeInterval,
            File realTrafficFile) {
        super(linkCapacity,
                timeInterval);

        this.realTrafficFile = realTrafficFile;
    }

    public RealTrafficFlowGenerator(Integer linkCapacity,
            Integer timeInterval,
            File realTrafficFile,
            Integer priorityLinkCapacity) {
        super(linkCapacity,
                timeInterval,
                priorityLinkCapacity);

        this.realTrafficFile = realTrafficFile;
    }

    public void setCompactTimes(Double compactTimes) throws Exception {
        if (compactTimes <= 0.0) {
            throw new Exception("compactTimes have to be larger than zero");
        }
        this.compactTimes = compactTimes;
    }
    
    /**
     * use base detector to filter out all flows which violate the base detector.
     * @param baseDetector
     */
    public void enableLargeRealFlowFilter(Detector baseDetector) {
        this.baseDetectorForFilter = baseDetector;
    }
    
    /**
     * use QD to enforce every flow to comply with flow spec 
     * of the specified leaky bucket detector.
     * It will drop packets which will cause flow spec violation.
     * Usually we should not use this with enableLargeRealFlowFilter
     */
    public void enableLargeFlowShaper(LeakyBucketDetector detector) {
        baseDetectorFlowShaper = detector;
    }

    @Override
    public void generateFlowsHelper() throws Exception {

        parameterCheck();

        if (outputPackets != null) {
            outputPackets.clear();
        }
        outputPackets = null;

        PacketReader realPacketReader = PacketReaderFactory
                .getPacketReader(realTrafficFile);

        QueueDiscipline gatewayRouter = new QueueDiscipline(
                priorityLinkCapacity);
        PacketWriter packetWriter = new PacketWriter(outputFile);
        
        Packet realPacket = realPacketReader.getNextPacket();
        
        while (realPacket != null) {

            countFlows(realPacket.flowId);

            realPacket.time = realPacket.time / compactTimes;
            gatewayRouter.processPacket(realPacket);
            realPacket = realPacketReader.getNextPacket();

            // output adjusted packets in the queue
            Packet adjustedPacket;
            while ((adjustedPacket = gatewayRouter.getNextPacket()) != null) {
                
                if (baseDetectorFlowShaper != null) {
                    boolean isLegal = baseDetectorFlowShaper.tryPacket(adjustedPacket);
                    if (isLegal) {
                        baseDetectorFlowShaper.processPacket(adjustedPacket);
                    } else {
                        continue;
                    }
                }
                
                if (baseDetectorForFilter != null) {
                    baseDetectorForFilter.processPacket(adjustedPacket);
                }
                
                if (adjustedPacket.time > timeInterval) {
                    break;
                }

                packetWriter.writePacket(adjustedPacket);
                totalRealTrafficVolume += adjustedPacket.size;
            }

            if (adjustedPacket != null && adjustedPacket.time > timeInterval) {
                break;
            }

        }
        
        packetWriter.close();
        realPacketReader.close();
        
        // remove illegal flows
        if (baseDetectorForFilter != null) {
            countFlowsReset();
            File tmpFile = new File(outputFile.getParentFile() + "/tmp_traffic.txt");
            Files.copy(outputFile.toPath(), tmpFile.toPath());
            PacketReader pr = PacketReaderFactory.getPacketReader(tmpFile);
            PacketWriter pw = new PacketWriter(outputFile);
            
            totalRealTrafficVolume = (long) 0;
            Packet packet;
            while((packet = pr.getNextPacket()) != null) {
                if (baseDetectorForFilter.getBlackList().containsKey(packet.flowId)) {
                    continue;
                }
                
                pw.writePacket(packet);
                totalRealTrafficVolume += packet.size;
                countFlows(packet.flowId);
            }
            
            pr.close();
            pw.close();
            tmpFile.delete();
            baseDetectorForFilter.reset();
        }

        aveRealTrafficRate = (int) (totalRealTrafficVolume / timeInterval);
    }

    @Override
    public void logConfig(Logger logger) throws IOException {
        super.logConfig(logger);
        logger.logConfigMsg("Real Traffic File: " + realTrafficFile + "\n");
        logger.logConfigMsg("Compact Time: " + compactTimes + "\n");
        logger.logConfigMsg("Num of Real Flows: " + numOfRealFlows + "\n");
        
        if (baseDetectorForFilter != null) {
            logger.logConfigMsg("Filter out flows violating the base detector: " 
                    + baseDetectorForFilter.name() + "\n");
        }
        
        if (baseDetectorFlowShaper != null) {
            logger.logConfigMsg("Shape flows violating the base detector: " 
                    + baseDetectorFlowShaper.name() + "\n");
        }
    }

    @Override
    protected void parameterCheck() throws Exception {
        super.parameterCheck();
        if (realTrafficFile == null) {
            throw new Exception("Please set the real traffic file");
        }
    }

    public Integer getAveRealTrafficRate() throws Exception {
        if (!flowsAreGenerated) {
            throw new Exception("Flows are not generated yet!");
        }
        return aveRealTrafficRate;
    }

    public String getRealTrafficSourceFile() {
        return realTrafficFile.toString();
    }

    private void countFlows(FlowId flowId) {
        if (!flowIdSet.contains(flowId)) {
            numOfRealFlows++;
            flowIdSet.add(flowId);
        }
        
        if (flowId.getIntegerValue() < startFlowId_int) {
            startFlowId_int = flowId.getIntegerValue();
        }
    }
    
    private void countFlowsReset() {
        flowIdSet.clear();
        numOfRealFlows = 0;
        startFlowId_int = Integer.MAX_VALUE;
    }

    public Set<FlowId> getFlowIdSet() {
        return flowIdSet;
    }
    
    public Integer getStartFlowId() {
        return startFlowId_int;
    }
    
    @Override
    public Integer getNumOfFlows() throws Exception {
        if (!flowsAreGenerated) {
            throw new Exception("Flows are not generated yet!");
        }
        return numOfRealFlows;
    }
    
    public Long getRealTrafficVolume() {
        return totalRealTrafficVolume;
    }

}
