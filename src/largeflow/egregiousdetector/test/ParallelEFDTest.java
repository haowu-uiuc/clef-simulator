package largeflow.egregiousdetector.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.egregiousdetector.EgregiousFlowDetector;
import largeflow.egregiousdetector.ParallelEFD;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;
import largeflow.flowgenerator.AttackFlowGenerator;
import largeflow.flowgenerator.UniformFlowGenerator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ParallelEFDTest {

    static private int linkCapacity = 25000000;
    static private Integer timeInterval = 10; // seconds, length of packet
                                              // stream
    static private Integer packetSize = 100; // Byte, packet size for generated
                                             // flows
    // number of small flows to generate
    static private Integer numOfSmallFlows = 80;
    // static private Integer numOfSmallFlows = 0;
    // number of large flows to generate
    static private Integer numOfLargeFlows = 20;
    // static private Integer numOfLargeFlows = 1;
    static private Integer largeFlowRate = 350000; // rate of large flows
    static private Integer smallFlowRate = 100000; // rate of small flows
    static private File inputTestTrafficFile;
    private ParallelEFD detector;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        File outputDir = new File("./data/test");
        inputTestTrafficFile = new File(
                outputDir.toString() + "/UniformFlowGeneratorFlows.txt");

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        AttackFlowGenerator flowGenerator = new UniformFlowGenerator(
                linkCapacity,
                timeInterval,
                packetSize,
                numOfSmallFlows,
                numOfLargeFlows,
                largeFlowRate,
                smallFlowRate);

        flowGenerator.setOutputFile(inputTestTrafficFile);
        flowGenerator.generateFlows();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
         inputTestTrafficFile.delete();
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws Exception {
        String detectorName = "test_egregious_detector";
        int gamma = 250000;
        int burst = 6072;
        double period = 0.1; // minPeriod = 6072 / 250000 = 0.025
        int numOfCounters = 200;

        detector = new ParallelEFD("test_parallel_efd", linkCapacity);
        detector.setTwinEFD();
        
        EgregiousFlowDetector subEFD1 = new EgregiousFlowDetector(detectorName,
                gamma,
                burst,
                period,
                linkCapacity,
                numOfCounters/2);
        subEFD1.setEstimatedNumOfFlows(linkCapacity / gamma);
        subEFD1.splitBucketByRelativeValue();
        subEFD1.enableDebug();
        
        EgregiousFlowDetector subEFD2 = new EgregiousFlowDetector(detectorName,
                gamma,
                burst,
                period,
                linkCapacity,
                numOfCounters/2);
        subEFD2.setEstimatedNumOfFlows(linkCapacity / gamma);
        subEFD2.splitBucketByRelativeValue();
        subEFD2.enableDebug();
        
        detector.addEFD(subEFD1);
        detector.addEFD(subEFD2);
        detector.setNumOfCounters(numOfCounters);
        
        System.out.println("Tc1 = " + subEFD1.getPeriod());
        System.out.println("Tc2 = " + subEFD2.getPeriod());
        
        LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
                "test_leaky_bucket_detector", burst, gamma, linkCapacity);

        PacketReader pr = PacketReaderFactory
                .getPacketReader(inputTestTrafficFile);
        Packet packet;

        while ((packet = pr.getNextPacket()) != null) {
            detector.processPacket(packet);
            leakyBucketDetector.processPacket(packet);
        }

        // result output
        int numOfLargeFlowCaught = detector.getBlackList().size();
        int numOfLargeFlowCaughtByLeakyBucket = leakyBucketDetector
                .getBlackList().size();

        Set<FlowId> FN = new HashSet<>(
                leakyBucketDetector.getBlackList().keySet());
        Set<FlowId> FP = new HashSet<>(detector.getBlackList().keySet());
        FN.removeAll(detector.getBlackList().keySet());
        FP.removeAll(leakyBucketDetector.getBlackList().keySet());

        Map<FlowId, Double> delayMap = new HashMap<>();
        for (Map.Entry<FlowId, Double> entry : leakyBucketDetector
                .getBlackList().entrySet()) {
            if (detector.getBlackList().containsKey(entry.getKey())) {
                delayMap.put(entry.getKey(),
                        detector.getBlackList().get(entry.getKey())
                                - leakyBucketDetector.getBlackList()
                                        .get(entry.getKey()));
            } else {
                delayMap.put(entry.getKey(), Double.POSITIVE_INFINITY);
            }
        }

        System.out.println("\n=====System Setting=====");
        System.out.println("Large Flow Rate Threshold: " + subEFD1.getGamma()
                + " Byte / sec");
        System.out.println("Burst Allowed: " + subEFD1.getBurst() + " Byte");
        System.out.println(
                "Link Capacity: " + subEFD1.getLinkCapacity() + " Byte / sec");
        System.out.println("Period: " + subEFD1.getPeriod() + " sec");
        System.out.println("Num of Counter: " + subEFD1.getNumOfCounters());
        System.out.println(
                "Fanout(Size) of each Bucketlist: " + subEFD1.getFanout());
        System.out.println(
                "Max Depth of Bucketlist Tree: " + subEFD1.getMaxDepth());
        System.out.println("Number of Branches in the BucketList Tree: "
                + subEFD1.getNumOfBranches());

        System.out.println("\n=====Results=====");
        System.out.println(
                "# of flows caught by detector: " + numOfLargeFlowCaught);
        System.out.println("TP = " + numOfLargeFlowCaughtByLeakyBucket);
        System.out.println("FN = " + FN.size());
        System.out.println("FP = " + FP.size());

        System.out.println("Delay Map: " + delayMap);
        System.out.println("Detector BlackList: " + detector.getBlackList());
        System.out.println(
                "LeakBucket BlackList: " + leakyBucketDetector.getBlackList());

    }
    
    @Test
    public void testTwinEFDHighPerid() throws Exception {
        int outboundLinkCapacity = 125000000;
        EgregiousFlowDetector efd = new EgregiousFlowDetector("efd",
                12500,
                1514,
                0.2,
                125000000,
                50);
        efd.setEstimatedNumOfFlows(10000);
        efd.splitBucketByRelativeValue();
        efd.enableDebug();
        
        System.out.println("EFD Depth = " + efd.getMaxDepth());
        System.out.println("EFD Period = " + efd.getPeriod());
        System.out.println("Twin EFD High Period = " + efd.getTwinEFDHighPeriod(outboundLinkCapacity));
        
        assertTrue(Math.abs(efd.getTwinEFDHighPeriod(outboundLinkCapacity) / efd.getPeriod() - 9.78) < 0.05);
        
        EgregiousFlowDetector efd2 = new EgregiousFlowDetector("efd",
                12500,
                1514,
                0.2,
                125000000,
                50);
        efd.setEstimatedNumOfFlows(10000);
        efd.splitBucketByRelativeValue();
        efd.enableDebug();
        
        ParallelEFD twinEFD = new ParallelEFD("test_twin_efd", 125000000);
        twinEFD.addEFD(efd);
        twinEFD.addEFD(efd2);
        twinEFD.setTwinEFD();
        twinEFD.setNumOfCounters(100);
        assertTrue(efd.getNumOfCounters() == 50);
        assertTrue(efd.getNumOfCounters() == 50);
        assertTrue(Math.abs(efd2.getPeriod() / efd.getPeriod() - 9.78) < 0.05);
        
        twinEFD.setTwinEFDTc2Adjust(0.5);
        twinEFD.setNumOfCounters(100);
        assertTrue(Math.abs(efd2.getPeriod() / efd.getPeriod() - 9.78 * 0.5) < 0.05);
    }
}
