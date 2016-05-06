package largeflow.eardet.test;

import static org.junit.Assert.assertTrue;

import java.io.File;

import largeflow.datatype.Packet;
import largeflow.eardet.VirlinkEARDet;
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

public class VirlinkEARDetTest {

	static private Integer linkCapacity; // Byte / sec
	static private Integer virtualLinkCapacity; // Byte / sec
	static private Integer queueSizeThreshold; // Byte
	static private Integer timeInterval; // seconds, length of packet stream
	static private Integer packetSize; // Byte, packet size for generated flows
	static private Integer numOfSmallFlows; // number of small flows to generate
	static private Integer numOfLargeFlows; // number of large flows to generate
	static private Integer largeFlowRate; // rate of large flows
	static private Integer smallFlowRate; // rate of small flows
	static private File inputTestTrafficFile;
	static private Integer resolution;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
//		inputTestTrafficFile.delete();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCatchingLargeFlow() throws Exception {
		linkCapacity = 25000000;
		virtualLinkCapacity = linkCapacity / 20;
		queueSizeThreshold = 1518 * 10;
		timeInterval = 15;
		packetSize = 500;
		numOfSmallFlows = 0;
		numOfLargeFlows = 30;
		largeFlowRate = 300000;
		smallFlowRate = 1500;
		resolution = (int) (0.0 * largeFlowRate);

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
				largeFlowRate,
				smallFlowRate);

		flowGenerator.setOutputFile(inputTestTrafficFile);
		flowGenerator.generateFlows();

		// int gamma_h = largeFlowRate - resolution;
		int gamma_h = linkCapacity / 100;
		int gamma_l = (int) (gamma_h / 5.0);
		int alpha = 1518;
		int beta_l = 4 * alpha;

		VirlinkEARDet virlinkEARDet = new largeflow.eardet.VirlinkEARDet("test_virlink_eardet",
				alpha,
				beta_l,
				gamma_h,
				gamma_l,
				1.0,
				linkCapacity,
				virtualLinkCapacity,
				queueSizeThreshold,
				1.0);

		VirlinkEARDet virlinkEARDet_nodrop = new largeflow.eardet.VirlinkEARDet("test_virlink_eardet_nodrop",
				alpha,
				beta_l,
				gamma_h,
				gamma_l,
				1.0,
				linkCapacity,
				virtualLinkCapacity,
				queueSizeThreshold,
				0.0);

		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector("test_leaky_bucket_detector",
				virlinkEARDet.getBetaH(),
				gamma_h,
				linkCapacity);

		PacketReader pr = PacketReaderFactory
				.getPacketReader(inputTestTrafficFile);
		Packet packet;

		while ((packet = pr.getNextPacket()) != null) {
			virlinkEARDet.processPacket(packet);
			virlinkEARDet_nodrop.processPacket(packet);
			leakyBucketDetector.processPacket(packet);
		}

		int numOfLargeFlowCaught = virlinkEARDet.getBlackList().size();
		int numOfLargeFlowCaught_nodrop = virlinkEARDet_nodrop.getBlackList()
				.size();
		int numOfLargeFlowCaughtByLeakyBucket = leakyBucketDetector
				.getBlackList().size();
		System.out.println("=======testCatchingLargeFlows=======");
		System.out
				.println("link capacity = " + virlinkEARDet.getLinkCapacity());
		System.out.println("beta_h = " + virlinkEARDet.getBetaH());
		System.out.println("gamma_h = " + virlinkEARDet.getGammaH());
		System.out.println("beta_l = " + virlinkEARDet.getBetaL());
		System.out.println("gamma_l = " + virlinkEARDet.getGammaL());
		System.out.println("beta_th = " + virlinkEARDet.getBetaTh());
		System.out.println("num counters = " + virlinkEARDet.getNumOfCounters());
		System.out.println("Num of large flow caught: " + numOfLargeFlowCaught);
		System.out.println("Num of large flow caught when no packet drop: "
				+ numOfLargeFlowCaught_nodrop);
		System.out.println("Num of large flow in Leaky Bucket: "
				+ numOfLargeFlowCaughtByLeakyBucket);
		assertTrue(numOfLargeFlowCaught == numOfLargeFlows);
//		assertTrue(numOfLargeFlowCaught < numOfLargeFlows);
		pr.close();
	}

	@Test
	public void testNotCatchingSmallFlow() throws Exception {
		linkCapacity = 25000000;
		virtualLinkCapacity = linkCapacity / 10;
		queueSizeThreshold = 1518 * 10;
		timeInterval = 5;
		packetSize = 500;
		numOfSmallFlows = 0;
		numOfLargeFlows = 10;
		largeFlowRate = 40000;
		smallFlowRate = 1500;
		resolution = (int) (0.0 * largeFlowRate);

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
				largeFlowRate,
				smallFlowRate);

		flowGenerator.setOutputFile(inputTestTrafficFile);
		flowGenerator.generateFlows();

		// int gamma_h = largeFlowRate - resolution;
		int gamma_h = linkCapacity / 100;
		int gamma_l = (int) (gamma_h / 5.0);
		int alpha = 1518;
		int beta_l = 4 * alpha;

		VirlinkEARDet virlinkEARDet = new largeflow.eardet.VirlinkEARDet("test_virlink_eardet",
				alpha,
				beta_l,
				gamma_h,
				gamma_l,
				1.0,
				linkCapacity,
				virtualLinkCapacity,
				queueSizeThreshold,
				1.0);

		VirlinkEARDet virlinkEARDet_nodrop = new largeflow.eardet.VirlinkEARDet("test_virlink_eardet_nodrop",
				alpha,
				beta_l,
				gamma_h,
				gamma_l,
				1.0,
				linkCapacity,
				virtualLinkCapacity,
				queueSizeThreshold,
				0.0);

		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector("test_leaky_bucket_detector",
				virlinkEARDet.getBetaH(),
				gamma_h,
				linkCapacity);

		PacketReader pr = PacketReaderFactory
				.getPacketReader(inputTestTrafficFile);
		Packet packet;

		while ((packet = pr.getNextPacket()) != null) {
			virlinkEARDet.processPacket(packet);
			virlinkEARDet_nodrop.processPacket(packet);
			leakyBucketDetector.processPacket(packet);
		}

		int numOfLargeFlowCaught = virlinkEARDet.getBlackList().size();
		int numOfLargeFlowCaught_nodrop = virlinkEARDet_nodrop.getBlackList()
				.size();
		int numOfLargeFlowCaughtByLeakyBucket = leakyBucketDetector
				.getBlackList().size();
		System.out.println("=======testCatchingLargeFlows=======");
		System.out
				.println("link capacity = " + virlinkEARDet.getLinkCapacity());
		System.out.println("beta_h = " + virlinkEARDet.getBetaH());
		System.out.println("gamma_h = " + virlinkEARDet.getGammaH());
		System.out.println("beta_l = " + virlinkEARDet.getBetaL());
		System.out.println("gamma_l = " + virlinkEARDet.getGammaL());
		System.out.println("beta_th = " + virlinkEARDet.getBetaTh());
		System.out
				.println("num counters = " + virlinkEARDet.getNumOfCounters());
		System.out.println("Num of large flow caught: " + numOfLargeFlowCaught);
		System.out.println("Num of large flow caught when no packet drop: "
				+ numOfLargeFlowCaught_nodrop);
		System.out.println("Num of large flow in Leaky Bucket: "
				+ numOfLargeFlowCaughtByLeakyBucket);
		assertTrue(numOfLargeFlowCaught == 0);
		assertTrue(numOfLargeFlowCaught_nodrop == 0);
		pr.close();
	}

}
