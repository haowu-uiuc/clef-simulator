package largeflow.flowgenerator.test;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import largeflow.flowgenerator.RandomFlowGenerator;
import largeflow.flowgenerator.UniformFlowGenerator;

public class FlowGeneratorTest {

	private Integer linkCapacity; // Byte
	private Integer timeInterval; // seconds, length of packet stream
	private Integer packetSize; // Byte, packet size for generated flows
	private Integer numOfSmallFlows; // number of small flows to generate
	private Integer numOfLargeFlows; // number of large flows to generate
	private Integer numOfBurstFlows; // number of burst flows to generate
	private Integer largeFlowRate; // rate of large flows
	private Integer smallFlowRate; // rate of small flows
	private Integer burstFlowSize; // size of each burst
	private File outputDir;

	@Before
	public void initTest() {
		linkCapacity = 25000000;
		timeInterval = 2;
		packetSize = 100;
		numOfSmallFlows = 0;
		numOfLargeFlows = 100;
		numOfBurstFlows = 0;
		largeFlowRate = 150000;
		smallFlowRate = 1500;
		burstFlowSize = 450000;
		outputDir = new File("./data/test");

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
	}

	@Test
	public void testUniformFlowGenerator() throws Exception {
		UniformFlowGenerator flowGenerator = new UniformFlowGenerator(linkCapacity,
				timeInterval,
				packetSize,
				numOfSmallFlows,
				numOfLargeFlows,
				numOfBurstFlows,
				largeFlowRate,
				smallFlowRate,
				burstFlowSize);

		flowGenerator.setOutputFile(new File(outputDir.toString()
				+ "/UniformFlowGeneratorFlows.txt"));
		flowGenerator.generateFlows();
	}

	@Test
	public void testUniformFlowGeneratorWithSmallFlows() throws Exception {
		UniformFlowGenerator flowGenerator = new UniformFlowGenerator(linkCapacity,
				timeInterval,
				packetSize,
				1000,
				numOfLargeFlows,
				numOfBurstFlows,
				largeFlowRate,
				smallFlowRate,
				burstFlowSize);

		flowGenerator.setOutputFile(new File(outputDir.toString()
				+ "/UniformFlowGeneratorFlows_WithSmallFlows.txt"));
		flowGenerator.generateFlows();
	}
	
	@Test
	public void testUniformFlowGeneratorWithNoFlow() throws Exception {
		UniformFlowGenerator flowGenerator = new UniformFlowGenerator(linkCapacity,
				timeInterval,
				packetSize,
				0,
				0,
				0,
				largeFlowRate,
				smallFlowRate,
				burstFlowSize);

		flowGenerator.setOutputFile(new File(outputDir.toString()
				+ "/UniformFlowGeneratorFlows_WithNoFlows.txt"));
		flowGenerator.generateFlows();
	}

	@Test
	public void testRandomFlowGenerator() throws Exception {
		RandomFlowGenerator flowGenerator = new RandomFlowGenerator(linkCapacity,
				timeInterval,
				packetSize,
				numOfSmallFlows,
				numOfLargeFlows,
				numOfBurstFlows,
				largeFlowRate,
				smallFlowRate,
				burstFlowSize);

		flowGenerator.setOutputFile(new File(outputDir.toString()
				+ "/RandomFlowGeneratorFlows.txt"));
		flowGenerator.generateFlows();
	}

	@Test
	public void testRandomFlowGeneratorWithSmallFlows() throws Exception {
		RandomFlowGenerator flowGenerator = new RandomFlowGenerator(linkCapacity,
				timeInterval,
				packetSize,
				1000,
				numOfLargeFlows,
				numOfBurstFlows,
				largeFlowRate,
				smallFlowRate,
				burstFlowSize);

		flowGenerator.setOutputFile(new File(outputDir.toString()
				+ "/RandomFlowGeneratorFlows_WithSmallFlows.txt"));
		flowGenerator.generateFlows();
	}

}
