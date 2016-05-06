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
	private Integer largeFlowRate; // rate of large flows
	private Integer smallFlowRate; // rate of small flows
	private File outputDir;
	
	private Double dutyCycle = 0.2;
	private Double period = 1.0;

	@Before
	public void initTest() {
		linkCapacity = 25000000;
		timeInterval = 2;
		packetSize = 100;
		numOfSmallFlows = 0;
		numOfLargeFlows = 10;
		largeFlowRate = 150000;
		smallFlowRate = 1500;
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
				largeFlowRate,
				smallFlowRate);

		flowGenerator.setOutputFile(new File(outputDir.toString()
				+ "/UniformFlowGeneratorFlows.txt"));
		flowGenerator.generateFlows();
	}
	
	@Test
    public void testUniformFlowGeneratorWithBurst() throws Exception {
        UniformFlowGenerator flowGenerator = new UniformFlowGenerator(linkCapacity,
                timeInterval,
                packetSize,
                numOfSmallFlows,
                numOfLargeFlows,
                largeFlowRate,
                smallFlowRate);

        flowGenerator.setOutputFile(new File(outputDir.toString()
                + "/UniformFlowGeneratorFlows.txt"));
        flowGenerator.setDutyCycleAndPeriod(dutyCycle, period);
        flowGenerator.generateFlows();
    }

	@Test
	public void testUniformFlowGeneratorWithSmallFlows() throws Exception {
		UniformFlowGenerator flowGenerator = new UniformFlowGenerator(linkCapacity,
				timeInterval,
				packetSize,
				1000,
				numOfLargeFlows,
				largeFlowRate,
				smallFlowRate);

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
				largeFlowRate,
				smallFlowRate);

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
				largeFlowRate,
				smallFlowRate);

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
				largeFlowRate,
				smallFlowRate);

		flowGenerator.setOutputFile(new File(outputDir.toString()
				+ "/RandomFlowGeneratorFlows_WithSmallFlows.txt"));
		flowGenerator.generateFlows();
	}

}
