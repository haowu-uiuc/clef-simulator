package largeflow.flowgenerator.test;

import static org.junit.Assert.assertTrue;

import java.io.File;

import largeflow.datatype.Packet;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;
import largeflow.flowgenerator.RealTrafficFlowGenerator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RealTrafficFlowGeneratorTest {

    static private Integer linkCapacity; // Byte
    static private Integer bestEffortLinkCapacity; // Byte
    static private Integer timeInterval; // seconds, length of packet stream

    // flows
    static private File outputDir;
    static private File outputFile;
    static private File realTrafficFile;
    static private Double compactTimes;

    @BeforeClass
    static public void initTest() {
        linkCapacity = 300000000;
        bestEffortLinkCapacity = 50000000;
        timeInterval = 10;
        compactTimes = 10.0;
        outputDir = new File("./data/test/flow_generator_test");
        outputFile = new File(
                outputDir.toString() + "/RealTrafficFlowGeneratorFlows.txt");
        realTrafficFile = new File("./data/test/realtrace_long.txt");
        // realTrafficFile = new
        // File("./data/Federico_II/federico_trace_10min.txt");

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    @AfterClass
    static public void tearDownTest() {
        if (outputFile.exists()) {
            outputFile.delete();
        }

        if (outputDir.exists()) {
            outputDir.delete();
        }
    }

    @Test
    public void testBasicFunctions() throws Exception {
        linkCapacity = 300000000;
        bestEffortLinkCapacity = 50000000;
        timeInterval = 10;
        compactTimes = 10.0;

        RealTrafficFlowGenerator flowGenerator = new RealTrafficFlowGenerator(
                linkCapacity,
                timeInterval,
                realTrafficFile,
                bestEffortLinkCapacity);
        flowGenerator.setCompactTimes(compactTimes);

        flowGenerator.setOutputFile(outputFile);
        flowGenerator.generateFlows();

        System.out
                .println("Num of Real Flows: " + flowGenerator.getNumOfFlows());
        System.out.println("Average Rate of Real Traffic: "
                + flowGenerator.getAveRealTrafficRate());
    }

    @Test
	public void analyzeRealTraffic() throws Exception {
	    linkCapacity = 25000000;
        timeInterval = 10;
        compactTimes = 1.0;
        
        RealTrafficFlowGenerator flowGenerator = new RealTrafficFlowGenerator(linkCapacity,
                timeInterval,
                realTrafficFile);
        flowGenerator.setCompactTimes(compactTimes);

        flowGenerator.setOutputFile(outputFile);
        flowGenerator.generateFlows();
        
        PacketReader pr = PacketReaderFactory.getPacketReader(flowGenerator.getOutputFile());
        
        
        System.out.println("Legal Traffic:");
        
        for (int largeFlowRate = 25000; largeFlowRate <= 2500000; largeFlowRate += 25000) {
            LeakyBucketDetector baseDetector = new LeakyBucketDetector(
                    "test_leakybucket",
                    1518 * 4,
                    largeFlowRate,
                    linkCapacity);
            
            Packet packet;
            int legalTraffic = 0;
            while ((packet = pr.getNextPacket()) != null) {
                baseDetector.processPacket(packet);
            }
            pr.close();
            pr.rewind();
            while ((packet = pr.getNextPacket()) != null) {
                if (baseDetector.getBlackList().containsKey(packet.flowId)) {
                    continue;
                }
                
                legalTraffic += packet.size;
            }
            pr.close();
            pr.rewind();
            
            int legalFlowNum = flowGenerator.getNumOfFlows() - baseDetector.getBlackList().size();
            
            System.out.println("LargeFlowRate=" + largeFlowRate + " : volume=" + legalTraffic
                    + ", ratio=" + (double)legalTraffic/flowGenerator.getRealTrafficVolume()
                    + ", flowNum=" + legalFlowNum
                    + ", flowRatio=" + (double)legalFlowNum / flowGenerator.getNumOfFlows());
        }
        
        System.out.println("All Real Traffic:");
        System.out.println("Num of Real Flows = " + flowGenerator.getNumOfFlows());
        System.out.println("Average Rate of Real Traffic = " + flowGenerator.getAveRealTrafficRate());
        System.out.println("Volume of Real Traffic = " + flowGenerator.getRealTrafficVolume());
        System.out.println("");
	}
    
    @Test
    public void testLargeRealFlowFilter() throws Exception {
        linkCapacity = 25000000;
        timeInterval = 10;
        compactTimes = 1.0;
        
        RealTrafficFlowGenerator flowGenerator_original = new RealTrafficFlowGenerator(linkCapacity,
                timeInterval,
                realTrafficFile);
        flowGenerator_original.setCompactTimes(compactTimes);
        flowGenerator_original.setOutputFile(outputFile);
        flowGenerator_original.generateFlows();
        
        System.out.println("Legal Traffic:");
        
        for (int largeFlowRate = 50000; largeFlowRate <= 500000; largeFlowRate += 50000) {
            LeakyBucketDetector baseDetector = new LeakyBucketDetector(
                    "test_leakybucket",
                    1518 * 4,
                    largeFlowRate,
                    linkCapacity);
            
            
            RealTrafficFlowGenerator flowGenerator = new RealTrafficFlowGenerator(linkCapacity,
                    timeInterval,
                    realTrafficFile);
            flowGenerator.setCompactTimes(compactTimes);

            flowGenerator.setOutputFile(outputFile);
            flowGenerator.enableLargeRealFlowFilter(baseDetector);
            flowGenerator.generateFlows();
            
            PacketReader pr = PacketReaderFactory.getPacketReader(flowGenerator.getOutputFile());
            
            baseDetector.reset();
            Packet packet;
            int legalTraffic = 0;
            while ((packet = pr.getNextPacket()) != null) {
                baseDetector.processPacket(packet);
            }
            pr.close();
            pr.rewind();
            while ((packet = pr.getNextPacket()) != null) {
                if (baseDetector.getBlackList().containsKey(packet.flowId)) {
                    continue;
                }
                
                legalTraffic += packet.size;
            }
            pr.close();
            pr.rewind();
            
            int legalFlowNum = flowGenerator.getNumOfFlows() - baseDetector.getBlackList().size();
            
            assertTrue(baseDetector.getBlackList().size() == 0);
            assertTrue(flowGenerator_original.getNumOfFlows() > flowGenerator.getNumOfFlows());

            System.out.println("LargeFlowRate=" + largeFlowRate + " : volume=" + legalTraffic
                    + ", ratio=" + (double)legalTraffic/flowGenerator.getRealTrafficVolume()
                    + ", flowNum=" + legalFlowNum
                    + ", flowRatio=" + (double)legalFlowNum / flowGenerator.getNumOfFlows());
        }
    }
    
    @Test
    public void testLargeRealFlowShaper() throws Exception {
        linkCapacity = 25000000;
        timeInterval = 10;
        compactTimes = 1.0;
        
        RealTrafficFlowGenerator flowGenerator_original = new RealTrafficFlowGenerator(linkCapacity,
                timeInterval,
                realTrafficFile);
        flowGenerator_original.setCompactTimes(compactTimes);
        flowGenerator_original.setOutputFile(outputFile);
        flowGenerator_original.generateFlows();
        System.out.println("Original Traffic:");
        System.out.println("Traffic Volume = " + flowGenerator_original.getRealTrafficVolume());
        
        System.out.println("Legal Traffic:");
        
        for (int largeFlowRate = 50000; largeFlowRate <= 200000; largeFlowRate += 50000) {
            LeakyBucketDetector baseDetector = new LeakyBucketDetector(
                    "test_leakybucket",
                    1518 * 4,
                    largeFlowRate,
                    linkCapacity);
            
            
            RealTrafficFlowGenerator flowGenerator = new RealTrafficFlowGenerator(linkCapacity,
                    timeInterval,
                    realTrafficFile);
            flowGenerator.setCompactTimes(compactTimes);

            flowGenerator.setOutputFile(outputFile);
            flowGenerator.enableLargeFlowShaper(baseDetector);
            flowGenerator.generateFlows();
            
            PacketReader pr = PacketReaderFactory.getPacketReader(flowGenerator.getOutputFile());
            
            baseDetector.reset();
            Packet packet;
            int legalTraffic = 0;
            while ((packet = pr.getNextPacket()) != null) {
                baseDetector.processPacket(packet);
            }
            pr.close();
            pr.rewind();
            while ((packet = pr.getNextPacket()) != null) {
                if (baseDetector.getBlackList().containsKey(packet.flowId)) {
                    continue;
                }
                
                legalTraffic += packet.size;
            }
            pr.close();
            pr.rewind();
            
            int legalFlowNum = flowGenerator.getNumOfFlows() - baseDetector.getBlackList().size();
            
            assertTrue(baseDetector.getBlackList().size() == 0);
            assertTrue(flowGenerator_original.getNumOfFlows().equals(flowGenerator.getNumOfFlows()));
            
            System.out.println("LargeFlowRate=" + largeFlowRate + " : volume=" + legalTraffic
                    + ", ratio=" + (double)legalTraffic/flowGenerator.getRealTrafficVolume()
                    + ", flowNum=" + legalFlowNum
                    + ", flowRatio=" + (double)legalFlowNum / flowGenerator.getNumOfFlows());
        }
    }

}
