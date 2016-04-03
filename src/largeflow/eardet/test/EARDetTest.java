package largeflow.eardet.test;

import static org.junit.Assert.*;

import java.io.File;

import largeflow.datatype.Packet;
import largeflow.eardet.EARDet;
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

/**
 * this set of test case rely on UniformFlowGenerator and LeakyBucketDetector
 * 
 * @author HaoWu
 *
 */
public class EARDetTest {

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

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		linkCapacity = 25000000;
		timeInterval = 5;
		packetSize = 100;
		numOfSmallFlows = 0;
		numOfLargeFlows = 20;
		numOfBurstFlows = 0;
		largeFlowRate = 50000;
		smallFlowRate = 1500;
		burstFlowSize = 450000;
		resolution = (int) (0.0 * largeFlowRate);

		File outputDir = new File("./data/test");
		inputTestTrafficFile = new File(outputDir.toString()
				+ "/UniformFlowGeneratorFlows.txt");

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		AttackFlowGenerator flowGenerator = new UniformFlowGenerator(linkCapacity,
				timeInterval, packetSize, numOfSmallFlows, numOfLargeFlows,
				numOfBurstFlows, largeFlowRate, smallFlowRate, burstFlowSize);

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
	public void testCatchingLargeFlow() throws Exception {
		int gamma_h = largeFlowRate - resolution;
		int gamma_l = (int) (gamma_h / 10.0);
		int alpha = 1518;
		int beta_l = 4 * alpha;
		
		EARDet eardet = new EARDet("test_eardet", alpha, beta_l, gamma_h, gamma_l,
				1.0, linkCapacity);
		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
				"test_leaky_bucket_detector", eardet.getBetaH(), gamma_h, linkCapacity);

		PacketReader pr = PacketReaderFactory.getPacketReader(inputTestTrafficFile);
		Packet packet;

		while ((packet = pr.getNextPacket()) != null) {
			eardet.processPacket(packet);
			leakyBucketDetector.processPacket(packet);
		}

		int numOfLargeFlowCaught = eardet.getBlackList().size();
		int numOfLargeFlowCaughtByLeakyBucket = leakyBucketDetector
				.getBlackList().size();
		System.out.println("=======testCatchingLargeFlows=======");
		System.out.println("link capacity = " + eardet.getLinkCapacity());
		System.out.println("beta_h = " + eardet.getBetaH());
		System.out.println("gamma_h = " + eardet.getGammaH());
		System.out.println("beta_l = " + eardet.getBetaL());
		System.out.println("gamma_l = " + eardet.getGammaL());
		System.out.println("beta_th = " + eardet.getBetaTh());
		System.out.println("num counters = " + eardet.getNumOfCounters());
		System.out.println("Num of large flow caught: " + numOfLargeFlowCaught);
		System.out.println("Num of large flow in Leaky Bucket: "
				+ numOfLargeFlowCaughtByLeakyBucket);
		assertTrue(numOfLargeFlowCaught == numOfLargeFlows);
		pr.close();
	}

	@Test
	public void testNotCatchingSmallFlow() throws Exception {
		int gamma_l = largeFlowRate + resolution;
		int gamma_h = (int) (gamma_l * 10.0);
		int alpha = 1518;
		int beta_l = 4 * alpha;
		
		EARDet eardet = new EARDet("test_eardet", alpha, beta_l, gamma_h, gamma_l,
				1.0, linkCapacity);
		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
				"test_leaky_bucket_detector", beta_l, gamma_l, linkCapacity);

		PacketReader pr = PacketReaderFactory.getPacketReader(inputTestTrafficFile);
		Packet packet;

		while ((packet = pr.getNextPacket()) != null) {
			eardet.processPacket(packet);
			leakyBucketDetector.processPacket(packet);
		}

		int numOfLargeFlowCaught = eardet.getBlackList().size();
		int numOfLargeFlowCaughtByLeakyBucket = leakyBucketDetector
				.getBlackList().size();
		System.out.println("=======testNotCatchingSmallFlow=======");
		System.out.println("link capacity = " + eardet.getLinkCapacity());
		System.out.println("beta_h = " + eardet.getBetaH());
		System.out.println("gamma_h = " + eardet.getGammaH());
		System.out.println("beta_l = " + eardet.getBetaL());
		System.out.println("gamma_l = " + eardet.getGammaL());
		System.out.println("beta_th = " + eardet.getBetaTh());
		System.out.println("num counters = " + eardet.getNumOfCounters());
		System.out.println("Num of large flow caught: " + numOfLargeFlowCaught);
		System.out.println("Num of large flow in Leaky Bucket: "
				+ numOfLargeFlowCaughtByLeakyBucket);
		System.out.println("Caught Flow ID = " + eardet.getBlackList());
		assertTrue(numOfLargeFlowCaught == 0);
		pr.close();
	}

}
