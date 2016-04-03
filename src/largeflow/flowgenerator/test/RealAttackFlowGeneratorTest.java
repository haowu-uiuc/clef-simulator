package largeflow.flowgenerator.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;
import largeflow.flowgenerator.RealAttackFlowGenerator;
import largeflow.flowgenerator.RealTrafficFlowGenerator;
import largeflow.utils.GenericUtils;

public class RealAttackFlowGeneratorTest {

	private int linkCapacity; // Byte
	private int outboundCapacity; // Byte
	private int timeInterval; // seconds, length of packet stream
	private int largeFlowPacketSize; // Byte, packet size for
												// generated
	private int perFlowReservation; // Byte
	private int fullRealFlowPacketSize; // Byte
	private int numOfFullRealFlows;
	private int numOfUnderUseRealFlows;

	// flows
	private int numOfLargeFlows; // number of large flows to generate
	private int largeFlowRate; // rate of large flows
	static private File outputDir;
	static private File outputFile;
	static private File realTrafficFile;
	private double compactTimes;

	@BeforeClass
	static public void initTest() {
		outputDir = new File("./data/test/flow_generator_test");
		outputFile = new File(outputDir.toString() + "/RealAttackFlowGeneratorFlows.txt");
		realTrafficFile = new File("./data/test/realtrace_long.txt");

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
	}

	@AfterClass
	static public void tearDownTest() {
	    GenericUtils.deleteDir(outputDir);
	}

	@Test
	public void test() throws Exception {
	    
	    linkCapacity = 250000000;
        outboundCapacity = 25000000;
        timeInterval = 10;
        largeFlowPacketSize = 1000;
        numOfLargeFlows = 10;
        perFlowReservation = outboundCapacity / 1000; // Byte
        largeFlowRate = perFlowReservation * 2;
        fullRealFlowPacketSize = 500; // Byte
        numOfFullRealFlows = 450;
        numOfUnderUseRealFlows = 50;
        compactTimes = 1.0;
	    
	    
        Detector baseDetector = new LeakyBucketDetector("test_leakybucket",
                4 * 1518,
                perFlowReservation,
                linkCapacity);
	    
		RealTrafficFlowGenerator realTrafficFlowGenerator = 
				new RealTrafficFlowGenerator(linkCapacity,
						timeInterval,
						realTrafficFile);
		realTrafficFlowGenerator.setCompactTimes(compactTimes);
		realTrafficFlowGenerator.setOutputFile(new File(outputDir + "/RealTrafficFlowGeneratorFlows.txt"));
		realTrafficFlowGenerator.enableLargeRealFlowFilter(baseDetector);
		realTrafficFlowGenerator.generateFlows();
		
        RealAttackFlowGenerator flowGenerator = new RealAttackFlowGenerator(
                linkCapacity,
                timeInterval,
                largeFlowPacketSize,
                numOfLargeFlows,
                largeFlowRate,
                perFlowReservation,
                fullRealFlowPacketSize,
                numOfFullRealFlows,
                numOfUnderUseRealFlows,
                realTrafficFlowGenerator);

		flowGenerator.setOutputFile(outputFile);
		flowGenerator.generateFlows();
		
		
		
	    PacketReader pr = PacketReaderFactory.getPacketReader(flowGenerator.getOutputFile());
	    Packet packet;
	    
	    Set<FlowId> fullRealSet = new HashSet<>(numOfFullRealFlows);
        Set<FlowId> underUseSet = new HashSet<>(numOfFullRealFlows);
        Set<FlowId> attackSet = new HashSet<>(numOfFullRealFlows);
	    
	    
	    while ((packet = pr.getNextPacket()) != null) {
	        if (packet.size == largeFlowPacketSize) {
	            attackSet.add(packet.flowId);
	        } else if (packet.size == fullRealFlowPacketSize) {
	            fullRealSet.add(packet.flowId);
	        } else {
	            underUseSet.add(packet.flowId);
	        }
	        
	        baseDetector.processPacket(packet);
	    }
	    
	    System.out.println("Full Real Number = " + flowGenerator.getNumOfFullRealFlows());
        System.out.println("Attack Flow Number = " + flowGenerator.getNumOfAttFlows());
        System.out.println("Under Use Real Number = " + flowGenerator.getNumOfUnderUseRealFlows());
        System.out.println("All Flow Number = " + flowGenerator.getNumOfFlows());
	    
	    System.out.println("Actaul Full Real Number = " + fullRealSet.size());
        System.out.println("Actaul Attack Flow Number = " + attackSet.size());
        System.out.println("Actaul Under Use Real Number = " + underUseSet.size());
        
        System.out.println("TP size = " + baseDetector.getBlackList().size());
        System.out.println("Blacklist = " + baseDetector.getBlackList());
        
        System.out.println("Attack FlowId Set size = " + flowGenerator.getAttackFlowIdSet().size());
        System.out.println("Full Real FlowId Set size = " + flowGenerator.getFullRealFlowIdSet().size());
        System.out.println("Under Use FlowId Set size = " + flowGenerator.getUnderUseRealFlowIdSet().size());
        
        System.out.println("Full Real Traffic Volume = " + flowGenerator.getFullRealTrafficVolume());
        System.out.println("Theo Full Real Traffic Volume = " + (numOfFullRealFlows * perFlowReservation));
        
        assertTrue(flowGenerator.getNumOfAttFlows() == numOfLargeFlows);
        assertTrue(flowGenerator.getNumOfFullRealFlows() == numOfFullRealFlows);
        assertTrue(flowGenerator.getNumOfUnderUseRealFlows() == numOfUnderUseRealFlows);
        assertTrue(flowGenerator.getNumOfFlows() == (numOfFullRealFlows + numOfLargeFlows + numOfUnderUseRealFlows));
        
        assertTrue(attackSet.size() == numOfLargeFlows);
        assertTrue(fullRealSet.size() == numOfFullRealFlows);
        assertTrue(underUseSet.size() == numOfUnderUseRealFlows);
       
	    assertTrue(baseDetector.getBlackList().size() == numOfLargeFlows);
	    
	    assertTrue(flowGenerator.getAttackFlowIdSet().size() == numOfLargeFlows);
	    assertTrue(flowGenerator.getFullRealFlowIdSet().size() == numOfFullRealFlows);
	    assertTrue(flowGenerator.getUnderUseRealFlowIdSet().size() == numOfUnderUseRealFlows);

	    // re-generate the flows without reset
	    long preFullRealVolume = flowGenerator.getFullRealTrafficVolume();
	    long preUnderUseVolume = flowGenerator.getUnderUseRealTrafficVolume();
	    Set<FlowId> preUnderUseSet = flowGenerator.getUnderUseRealFlowIdSet();
	    flowGenerator.generateFlows();
	    assertTrue(preFullRealVolume == flowGenerator.getFullRealTrafficVolume());
	    assertTrue(preUnderUseVolume == flowGenerator.getUnderUseRealTrafficVolume());
	    assertTrue(preUnderUseSet.equals(flowGenerator.getUnderUseRealFlowIdSet()));
	    
	    preFullRealVolume = flowGenerator.getFullRealTrafficVolume();
        preUnderUseVolume = flowGenerator.getUnderUseRealTrafficVolume();
        preUnderUseSet = flowGenerator.getUnderUseRealFlowIdSet();
        flowGenerator.reset();
        flowGenerator.generateFlows();
        assertFalse(preFullRealVolume == flowGenerator.getFullRealTrafficVolume());
        assertFalse(preUnderUseVolume == flowGenerator.getUnderUseRealTrafficVolume());
        assertFalse(preUnderUseSet.equals(flowGenerator.getUnderUseRealFlowIdSet()));

	    
	      
	}

}
