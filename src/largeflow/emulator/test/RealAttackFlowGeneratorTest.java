package largeflow.emulator.test;

import java.io.File;

import largeflow.emulator.RealAttackFlowGenerator;
import largeflow.emulator.RealTrafficFlowGenerator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RealAttackFlowGeneratorTest {

	static private Integer linkCapacity; // Byte
	static private Integer timeInterval; // seconds, length of packet stream
	static private Integer largeFlowPacketSize; // Byte, packet size for
												// generated
	// flows
	static private Integer numOfLargeFlows; // number of large flows to generate
	static private Integer largeFlowRate; // rate of large flows
	static private File outputDir;
	static private File outputFile;
	static private File realTrafficFile;
	static private Double compactTimes;

	@BeforeClass
	static public void initTest() {
		linkCapacity = 250000000;
		timeInterval = 10;
		largeFlowPacketSize = 1000;
		numOfLargeFlows = 1;
		// largeFlowRate = 5000000;
		largeFlowRate = 0;
		compactTimes = 10.0;
		outputDir = new File("./data/test/flow_generator_test");
		outputFile = new File(outputDir.toString() + "/RealAttackFlowGeneratorFlows.txt");
		realTrafficFile = new File("./data/test/realtrace_long.txt");

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
	}

	@AfterClass
	static public void tearDownTest() {
		if (outputDir.exists()) {
			for (File file : outputDir.listFiles()) {
				file.delete();
			}
			outputDir.delete();
		}
	}

	@Test
	public void testUniformFlowGenerator() throws Exception {
		RealTrafficFlowGenerator realTrafficFlowGenerator = 
				new RealTrafficFlowGenerator(linkCapacity,
						timeInterval,
						realTrafficFile);
		realTrafficFlowGenerator.setCompactTimes(compactTimes);
		realTrafficFlowGenerator.setOutputFile(new File(outputDir + "/RealTrafficFlowGeneratorFlows.txt"));
		realTrafficFlowGenerator.generateFlows();
		
		RealAttackFlowGenerator flowGenerator = new RealAttackFlowGenerator(linkCapacity,
				timeInterval,
				largeFlowPacketSize,
				numOfLargeFlows,
				largeFlowRate,
				realTrafficFlowGenerator);

		flowGenerator.setOutputFile(outputFile);
		flowGenerator.generateFlows();
	}

}
