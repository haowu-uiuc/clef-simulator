package largeflow.emulator;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

public class RealTrafficFlowGenerator extends FlowGenerator {

    private Double compactTimes = 1.0; // compact the real traffic from [0,
                                       // intervalTime * compactTimes] into [0,
                                       // intervalTime]
    private File realTrafficFile;

    private Set<FlowId> flowIdSet = new TreeSet<>();
    private Integer numOfRealFlows = 0;
    private Integer aveRealTrafficRate = 0;
    private Long totalRealTrafficVolume = (long) 0;

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
            Integer bestEffortLinkCapacity) {
        super(linkCapacity,
                timeInterval,
                bestEffortLinkCapacity);

        this.realTrafficFile = realTrafficFile;
    }

    public void setCompactTimes(Double compactTimes) throws Exception {
        if (compactTimes <= 0.0) {
            throw new Exception("compactTimes have to be larger than zero");
        }
        this.compactTimes = compactTimes;
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

        aveRealTrafficRate = (int) (totalRealTrafficVolume / timeInterval);

        realPacketReader.close();
        packetWriter.close();
    }

    @Override
    public void logConfig(Logger logger) throws IOException {
        super.logConfig(logger);
        logger.logConfigMsg("Real Traffic File: " + realTrafficFile + "\n");
        logger.logConfigMsg("Compact Time: " + compactTimes + "\n");
        logger.logConfigMsg("Num of Real Flows: " + numOfRealFlows + "\n");
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
    }

    public Set<FlowId> getFlowIdSet() {
        return flowIdSet;
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
