package largeflow.egregiousdetector.test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.egregiousdetector.EgregiousFlowDetector;
import largeflow.emulator.AttackFlowGenerator;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;
import largeflow.emulator.UniformFlowGenerator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EgregiousFlowDetectorTest {

	static private int linkCapacity = 25000000;
	static private Integer timeInterval = 10; // seconds, length of packet stream
	static private Integer packetSize = 100; // Byte, packet size for generated
												// flows
	static private Integer numOfSmallFlows = 80; // number of small flows to
												// generate
	static private Integer numOfLargeFlows = 20; // number of large flows to
													// generate
	static private Integer numOfBurstFlows = 0; // number of burst flows to
												// generate
	static private Integer largeFlowRate = 260000; // rate of large flows
	static private Integer smallFlowRate = 100000; // rate of small flows
	static private Integer burstFlowSize = 0; // size of each burst
	static private File inputTestTrafficFile;
	private EgregiousFlowDetector detector;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File outputDir = new File("./data/test");
		inputTestTrafficFile = new File(outputDir.toString()
				+ "/UniformFlowGeneratorFlows.txt");

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
		int numOfCounters = 100;

		detector = new EgregiousFlowDetector(detectorName,
				gamma,
				burst,
				period,
				linkCapacity,
				numOfCounters);
		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector("test_leaky_bucket_detector",
				burst,
				gamma,
				linkCapacity);

		PacketReader pr = PacketReaderFactory.getPacketReader(inputTestTrafficFile);
		Packet packet;

		while ((packet = pr.getNextPacket()) != null) {
			detector.processPacket(packet);
			leakyBucketDetector.processPacket(packet);
		}

		
		// result output
		int numOfLargeFlowCaught = detector.getBlackList().size();
		int numOfLargeFlowCaughtByLeakyBucket = leakyBucketDetector
				.getBlackList().size();

		Set<FlowId> FN = new HashSet<>(leakyBucketDetector.getBlackList()
				.keySet());
		Set<FlowId> FP = new HashSet<>(detector.getBlackList().keySet());
		FN.removeAll(detector.getBlackList().keySet());
		FP.removeAll(leakyBucketDetector.getBlackList().keySet());

		Map<FlowId, Double> delayMap = new HashMap<>(leakyBucketDetector.getBlackList());
		for (Map.Entry<FlowId, Double> entry : detector.getBlackList()
				.entrySet()) {
			if (delayMap.containsKey(entry.getKey())) {
				delayMap.put(entry.getKey(),
						entry.getValue() - delayMap.get(entry.getKey()));
			} else {
				delayMap.put(entry.getKey(), Double.POSITIVE_INFINITY);
			}
		}

		System.out.println("\n=====System Setting=====");
		System.out.println("Large Flow Rate Threshold: " + detector.getGamma()
				+ " Byte / sec");
		System.out.println("Burst Allowed: " + detector.getBurst() + " Byte");
		System.out.println("Link Capacity: " + detector.getLinkCapacity()
				+ " Byte / sec");
		System.out.println("Period: " + detector.getPeriod() + " sec");
		System.out.println("Num of Counter: " + detector.getNumOfCounters());
		System.out.println("Fanout(Size) of each Bucketlist: "
				+ detector.getFanout());
		System.out.println("Max Depth of Bucketlist Tree: "
				+ detector.getMaxDepth());
		System.out.println("Number of Branches in the BucketList Tree: "
				+ detector.getNumOfBranches());

		System.out.println("\n=====Results=====");
		System.out.println("# of flows caught by detector: "
				+ numOfLargeFlowCaught);
		System.out.println("TP = " + numOfLargeFlowCaughtByLeakyBucket);
		System.out.println("FN = " + FN.size());
		System.out.println("FP = " + FP.size());

		System.out.println("Delay Map: " + delayMap);
		System.out.println("Detector BlackList: " + detector.getBlackList());
		System.out.println("LeakBucket BlackList: "
				+ leakyBucketDetector.getBlackList());

	}

}
