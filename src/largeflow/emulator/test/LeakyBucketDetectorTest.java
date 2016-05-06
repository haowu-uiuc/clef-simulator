package largeflow.emulator.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import largeflow.datatype.Packet;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;
import largeflow.flowgenerator.AttackFlowGenerator;
import largeflow.flowgenerator.UniformFlowGenerator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * this set of tests relys on UniformFlowGenerator
 * @author HaoWu
 *
 */
public class LeakyBucketDetectorTest {

	static private Integer linkCapacity; // Byte
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
		linkCapacity = 25000000;
		timeInterval = 10;
		packetSize = 100;
		numOfSmallFlows = 0;
		numOfLargeFlows = 20;
		largeFlowRate = 150000;
		smallFlowRate = 1500;
		resolution = (int) (0.02 * largeFlowRate);

		File outputDir = new File("./data/test");
		inputTestTrafficFile = new File(outputDir.toString()
				+ "/UniformFlowGeneratorFlows.txt");

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		AttackFlowGenerator flowGenerator = new UniformFlowGenerator(
				linkCapacity, timeInterval, packetSize, numOfSmallFlows,
				numOfLargeFlows, largeFlowRate, smallFlowRate);

		flowGenerator.setOutputFile(inputTestTrafficFile);
		flowGenerator.generateFlows();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		inputTestTrafficFile.delete();
	}

	@Test
	public void testCatchingLargeFlows() throws IOException {
		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
				"test_leaky_bucket_detector", 6000,
				(largeFlowRate - resolution), linkCapacity);

		PacketReader pr = PacketReaderFactory.getPacketReader(inputTestTrafficFile);
		Packet packet;

		while ((packet = pr.getNextPacket()) != null) {
			leakyBucketDetector.processPacket(packet);
		}

		int numOfLargeFlowCaught = leakyBucketDetector.getBlackList().size();
		System.out.println("In testCatchingLargeFlows, num of large flow caught: " + numOfLargeFlowCaught);
		assertTrue(numOfLargeFlowCaught == numOfLargeFlows);
		pr.close();
	}

	@Test
	public void testNotCatchingSmallFlows() throws IOException {
		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
				"test_leaky_bucket_detector", 6000,
				(largeFlowRate + resolution), linkCapacity);

		PacketReader pr = PacketReaderFactory.getPacketReader(inputTestTrafficFile);
		Packet packet;

		while ((packet = pr.getNextPacket()) != null) {
			leakyBucketDetector.processPacket(packet);
		}

		int numOfLargeFlowCaught = leakyBucketDetector.getBlackList().size();
		System.out.println("In testNotCatchingSmallFlows, num of large flow caught: " + numOfLargeFlowCaught);
		assertTrue(numOfLargeFlowCaught == 0);
		pr.close();
	}
}
