package largeflow.multistagefilter;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.Logger;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;
import largeflow.flowgenerator.AttackFlowGenerator;
import largeflow.flowgenerator.UniformFlowGenerator;

public class MultistageFilterTest {

    static private Integer linkCapacity; // Byte
    static private Integer timeInterval; // seconds, length of packet stream
    static private Integer packetSize; // Byte, packet size for generated flows
    static private Integer numOfSmallFlows; // number of small flows to generate
    static private Integer numOfLargeFlows; // number of large flows to generate
    static private Integer largeFlowRate; // rate of large flows
    static private Integer smallFlowRate; // rate of small flows
    static private File inputTestTrafficFile;
    static private File inputWithSmallFlowsTestTrafficFile;
    static private Integer resolution;
    static private Logger logger;

    @BeforeClass
    static public void setUpBeforeClass() throws Exception {

        linkCapacity = 25000000;
        timeInterval = 10;
        packetSize = 100;
        numOfSmallFlows = 1000;
        numOfLargeFlows = 20;
        largeFlowRate = 50000;
        smallFlowRate = 1500;
        resolution = (int) (0.1 * largeFlowRate);

        File outputDir = new File("./data/test");
        inputTestTrafficFile = new File(
                outputDir.toString() + "/UniformFlowGeneratorFlows.txt");
        inputWithSmallFlowsTestTrafficFile = new File(outputDir.toString()
                + "/UniformFlowGeneratorFlowsWithSmallFlows.txt");

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        AttackFlowGenerator flowGenerator = new UniformFlowGenerator(
                linkCapacity,
                timeInterval,
                packetSize,
                0,
                numOfLargeFlows,
                largeFlowRate,
                smallFlowRate);

        flowGenerator.setOutputFile(inputTestTrafficFile);
        flowGenerator.generateFlows();

        flowGenerator = new UniformFlowGenerator(linkCapacity,
                timeInterval,
                packetSize,
                numOfSmallFlows,
                numOfLargeFlows,
                largeFlowRate,
                smallFlowRate);
        flowGenerator.setOutputFile(inputWithSmallFlowsTestTrafficFile);
        flowGenerator.generateFlows();

        logger = new Logger("test_exp");

    }

    @AfterClass
    static public void tearDownAfterClass() throws Exception {
        if (inputTestTrafficFile.exists()) {
            inputTestTrafficFile.delete();
        }
        if (inputWithSmallFlowsTestTrafficFile.exists()) {
            inputWithSmallFlowsTestTrafficFile.delete();
        }
        logger.close();
    }

    @Test
    public void FMFDetectorTest() throws Exception {
        Double T = 0.1;
        Integer numOfStages = 4;
        Integer sizeOfStage = 25;
        Integer threshold = (int) ((double) (largeFlowRate - resolution) * T);

        FMFDetector fmfDetector = new FMFDetector("test_FMFDetector",
                numOfStages,
                sizeOfStage,
                linkCapacity,
                T,
                threshold);

        testMultistageFilter(fmfDetector, inputTestTrafficFile, 0, null);
        logger.logDetectorConfig(fmfDetector, false);
    }

    @Test
    public void FMFDetectorCrossPeriodsTest() throws Exception {

        List<Packet> packetList;
        packetList = new ArrayList<>();
        packetList.add(new Packet(new FlowId("1"), 1000, 0.0));
        packetList.add(new Packet(new FlowId("1"), 501, 0.05));
        packetList.add(new Packet(new FlowId("1"), 0, 0.101));

        packetList.add(new Packet(new FlowId("1"), 1000, 0.11));
        packetList.add(new Packet(new FlowId("1"), 500, 0.15));
        packetList.add(new Packet(new FlowId("1"), 0, 0.201));

        packetList.add(new Packet(new FlowId("1"), 1000, 0.21));
        // packet that crosses periods
        packetList.add(new Packet(new FlowId("1"), 1000, 0.2951));
        packetList.add(new Packet(new FlowId("1"), 0, 0.301));

        packetList.add(new Packet(new FlowId("1"), 1000, 0.35));
        packetList.add(new Packet(new FlowId("1"), 0, 0.401));

        Double T = 0.1;
        Integer threshold = 1500;
        Integer linkCapacity = 100000;
        Integer numOfStages = 4;
        Integer sizeOfStage = 25;

        FMFDetector fmfDetector = new FMFDetector("test_FMFDetector",
                numOfStages,
                sizeOfStage,
                linkCapacity,
                T,
                threshold);
        fmfDetector.considerPacketCrossPeriods();

        int i = 0;
        // period 0
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertTrue(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.getBlackList().remove(new FlowId("1"));
        assertTrue(fmfDetector.getBlackList().isEmpty());

        // period 1
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        assertTrue(fmfDetector.getBlackList().isEmpty());

        // period 2
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        assertTrue(fmfDetector.getBlackList().isEmpty());

        // period 3
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertTrue(fmfDetector.getBlackList().containsKey(new FlowId("1")));

        fmfDetector.reset();
        fmfDetector.notConsiderPacketCrossPerids();

        i = 0;
        // period 0
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertTrue(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.getBlackList().remove(new FlowId("1"));
        assertTrue(fmfDetector.getBlackList().isEmpty());

        // period 1
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        assertTrue(fmfDetector.getBlackList().isEmpty());

        // period 2
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertTrue(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.getBlackList().remove(new FlowId("1"));
        assertTrue(fmfDetector.getBlackList().isEmpty());

        // period 3
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));
        fmfDetector.processPacket(packetList.get(i++));
        assertFalse(fmfDetector.getBlackList().containsKey(new FlowId("1")));

    }

    @Test
    public void AMFDetectorTest() throws Exception {

        Integer numOfStages = 4;
        Integer sizeOfStage = 25;
        Integer threshold = 1518 * 4;
        Integer drainRate = largeFlowRate - resolution;

        AMFDetector amfDetector = new AMFDetector("test_AMFDetector",
                numOfStages,
                sizeOfStage,
                linkCapacity,
                drainRate,
                threshold);

        LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
                "test_LeakyBucketDetector", threshold, drainRate, linkCapacity);

        testMultistageFilter(amfDetector, inputTestTrafficFile, 0, null);
        testDetectorWithLeakyBucket(amfDetector, leakyBucketDetector);

        logger.logDetectorConfig(amfDetector, false);
    }

    @Test
    public void FMFDetectorWithFlowMemoryTest() throws Exception {
        Double T = 0.1;
        Integer numOfStages = 4;
        Integer sizeOfStage = 25;
        Integer threshold = (int) ((double) (largeFlowRate - resolution) * T);
        Integer leakyBucketThreshold = 1518 * 4;
        Integer drainRate = largeFlowRate - resolution;
        FlowMemoryFactory fm_factory = new FlowMemoryFactory(
                leakyBucketThreshold, drainRate, linkCapacity);

        FMFDetector fmfDetector = new FMFDetector("test_FMFDetector",
                numOfStages,
                sizeOfStage,
                linkCapacity,
                T,
                threshold);
        fmfDetector.setFlowMemoryFactory(fm_factory);

        testMultistageFilter(fmfDetector, inputTestTrafficFile, 0, 0);

        fmfDetector.reset();
        testMultistageFilter(fmfDetector,
                inputWithSmallFlowsTestTrafficFile,
                null,
                0);

        logger.logDetectorConfig(fmfDetector, false);
    }

    @Test
    public void AMFDetectorWithFlowMemoryTest() throws Exception {

        Integer numOfStages = 4;
        Integer sizeOfStage = 25;
        Integer threshold = 1518 * 4;
        Integer drainRate = largeFlowRate - resolution;
        FlowMemoryFactory fm_factory = new FlowMemoryFactory(threshold,
                drainRate,
                linkCapacity);

        AMFDetector amfDetector = new AMFDetector("test_AMFDetector",
                numOfStages,
                sizeOfStage,
                linkCapacity,
                drainRate,
                threshold);
//        fm_factory.enableDebug();
        amfDetector.setFlowMemoryFactory(fm_factory);
        amfDetector.setRatioOfFlowMemory(0.5);
//        amfDetector.enableDebug();

        LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
                "test_LeakyBucketDetector", threshold, drainRate, linkCapacity);

        testMultistageFilter(amfDetector, inputTestTrafficFile, 0, 0);
        testDetectorWithLeakyBucket(amfDetector, leakyBucketDetector);

        amfDetector.reset();
        testMultistageFilter(amfDetector,
                inputWithSmallFlowsTestTrafficFile,
                null,
                0);

        logger.logDetectorConfig(amfDetector, false);
    }

    private void testMultistageFilter(MultistageFilter detector,
            File inputTrafficFile,
            Integer goldenFN,
            Integer goldenFP) throws Exception {

        PacketReader pr = PacketReaderFactory.getPacketReader(inputTrafficFile);
        Packet packet;

        while ((packet = pr.getNextPacket()) != null) {
            detector.processPacket(packet);
        }

        int numOfLargeFlowCaught = 0;
        int numOfFP = 0;
        for (FlowId flowId : detector.getBlackList().keySet()) {
            if (flowId.getIntegerValue() <= numOfLargeFlows) {
                numOfLargeFlowCaught++;
            } else {
                numOfFP++;
            }
        }

        System.out.println("=======testCatchingLargeFlows=======");
        System.out.println("Detector: " + detector.name());
        System.out.println("Traffic File: " + inputTrafficFile.getName());
        System.out.println("link capacity = " + detector.getLinkCapacity());
        System.out.println("# of stages = " + detector.getNumOfStages());
        System.out.println("size of stage = " + detector.getSizeOfStage());
        System.out.println("num counters = " + detector.getNumOfCounters());
        System.out.println("Num of large flow caught: " + numOfLargeFlowCaught);
        System.out.println("Num of FP: " + numOfFP);
        System.out.println("Num of FN: " + (numOfLargeFlows - numOfLargeFlowCaught));
        if (goldenFN != null) {
            assertTrue(numOfLargeFlowCaught == numOfLargeFlows - goldenFN);
        }
        if (goldenFP != null) {
            assertTrue(numOfFP == goldenFP);
        }
        pr.close();
    }

    private void testDetectorWithLeakyBucket(Detector detector,
            LeakyBucketDetector leakyBucketDetector) throws Exception {
        PacketReader pr = PacketReaderFactory
                .getPacketReader(inputTestTrafficFile);
        Packet packet;

        while ((packet = pr.getNextPacket()) != null) {
            detector.processPacket(packet);
            leakyBucketDetector.processPacket(packet);
        }

        // System.out.println("Leaky Bucket Detector: " +
        // leakyBucketDetector.getBlackList().size());
        // System.out.println(leakyBucketDetector.getBlackList());
        // System.out.println("Detector: " + detector.getBlackList().size());
        // System.out.println(detector.getBlackList());

        // blacklists of leakybucket and detector are the same.
        for (FlowId flowId : detector.getBlackList().keySet()) {
            if (flowId.getIntegerValue() <= numOfLargeFlows) {
                assertTrue(
                        leakyBucketDetector.getBlackList().containsKey(flowId));
            }
        }

        for (FlowId flowId : leakyBucketDetector.getBlackList().keySet()) {
            assertTrue(detector.getBlackList().containsKey(flowId));
        }
    }

}
