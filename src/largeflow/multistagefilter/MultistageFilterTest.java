package largeflow.multistagefilter;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.Logger;
import largeflow.emulator.AttackFlowGenerator;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;
import largeflow.emulator.UniformFlowGenerator;

public class MultistageFilterTest {

	static private Integer linkCapacity; // Byte
	static private Integer timeInterval; // seconds, length of packet stream
	static private Integer packetSize; // Byte, packet size for generated flows
	static private Integer numOfSmallFlows; // number of small flows to generate
	static private Integer numOfLargeFlows; // number of large flows to generate
	static private Integer numOfBurstFlows; // number of burst flows to generate
	static private Integer largeFlowRate; // rate of large flows
	static private Integer smallFlowRate; // rate of small flows
	static private Integer burstFlowSize; // size of each burst
	static private File inputTestTrafficFile;
	static private Integer resolution;
	static private Logger logger;

	
	@BeforeClass
	static public void setUpBeforeClass() throws Exception {

		linkCapacity = 25000000;
		timeInterval = 10;
		packetSize = 100;
		numOfSmallFlows = 0;
		numOfLargeFlows = 20;
		numOfBurstFlows = 0;
		largeFlowRate = 50000;
		smallFlowRate = 1500;
		burstFlowSize = 450000;
		resolution = (int) (0.02 * largeFlowRate);

		File outputDir = new File("./data/test");
		inputTestTrafficFile = new File(outputDir.toString() + "/UniformFlowGeneratorFlows.txt");

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		AttackFlowGenerator flowGenerator = new UniformFlowGenerator(linkCapacity,
				timeInterval,
				packetSize,
				numOfSmallFlows,
				numOfLargeFlows,
				numOfBurstFlows,
				largeFlowRate,
				smallFlowRate,
				burstFlowSize);

		flowGenerator.setOutputFile(inputTestTrafficFile);
		flowGenerator.generateFlows();

		logger = new Logger("test_exp");
		
	}

	@AfterClass
	static public void tearDownAfterClass() throws Exception {
		if (inputTestTrafficFile.exists()) {
			inputTestTrafficFile.delete();
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

		testMultistageFilter(fmfDetector);
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
		packetList.add(new Packet(new FlowId("1"), 1000, 0.2951));	// packet that crosses periods
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

		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector("test_LeakyBucketDetector",
				threshold,
				drainRate,
				linkCapacity);

		testMultistageFilter(amfDetector);
		testDetectorWithLeakyBucket(amfDetector, leakyBucketDetector);
		
		logger.logDetectorConfig(amfDetector, false);
	}

	private void testMultistageFilter(MultistageFilter detector) throws Exception {

		PacketReader pr = PacketReaderFactory.getPacketReader(inputTestTrafficFile);
		Packet packet;

		while ((packet = pr.getNextPacket()) != null) {
			detector.processPacket(packet);
		}

		int numOfLargeFlowCaught = detector.getBlackList().size();
		System.out.println("=======testCatchingLargeFlows=======");
		System.out.println("Detector: " + detector.name());
		System.out.println("link capacity = " + detector.getLinkCapacity());
		System.out.println("# of stages = " + detector.getNumOfStages());
		System.out.println("size of stage = " + detector.getSizeOfStage());
		System.out.println("num counters = " + detector.getNumOfCounters());
		System.out.println("Num of large flow caught: " + numOfLargeFlowCaught);
		assertTrue(numOfLargeFlowCaught == numOfLargeFlows);
		pr.close();
	}

	private void testDetectorWithLeakyBucket(Detector detector,
			LeakyBucketDetector leakyBucketDetector) throws Exception {
		PacketReader pr = PacketReaderFactory.getPacketReader(inputTestTrafficFile);
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
			assertTrue(leakyBucketDetector.getBlackList().containsKey(flowId));
		}

		for (FlowId flowId : leakyBucketDetector.getBlackList().keySet()) {
			assertTrue(detector.getBlackList().containsKey(flowId));
		}
	}

}
