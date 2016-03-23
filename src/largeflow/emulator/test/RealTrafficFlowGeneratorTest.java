package largeflow.emulator.test;

import java.io.File;

import largeflow.emulator.RealTrafficFlowGenerator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RealTrafficFlowGeneratorTest {

	static private Integer linkCapacity; // Byte
	static private Integer bestEffortLinkCapacity; // Byte
	static private Integer timeInterval; // seconds, length of packet stream
	static private Integer largeFlowPacketSize; // Byte, packet size for generated
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
		outputFile = new File(outputDir.toString()
				+ "/RealTrafficFlowGeneratorFlows.txt");
		realTrafficFile = new File("./data/test/realtrace_long.txt");
		//realTrafficFile = new File("./data/Federico_II/federico_trace_10min.txt");

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
	public void testUniformFlowGenerator() throws Exception {
		RealTrafficFlowGenerator flowGenerator = new RealTrafficFlowGenerator(linkCapacity,
				timeInterval,
				realTrafficFile,
				bestEffortLinkCapacity);
		flowGenerator.setCompactTimes(compactTimes);

		flowGenerator.setOutputFile(outputFile);
		flowGenerator.generateFlows();
		
		System.out.println("Best Effort Link Capacity: " + flowGenerator.getBestEffortLinkCapacity());
		System.out.println("Num of Real Flows: " + flowGenerator.getNumOfFlows());
		System.out.println("Average Rate of Real Traffic: " + flowGenerator.getAveRealTrafficRate());
	}

}
