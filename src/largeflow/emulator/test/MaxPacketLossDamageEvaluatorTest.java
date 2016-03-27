package largeflow.emulator.test;

import java.io.File;
import java.util.List;
import java.util.Map;

import largeflow.datatype.MaxDamageEvaluatorResult;
import largeflow.eardet.EARDet;
import largeflow.emulator.Logger;
import largeflow.emulator.AdvancedRouter;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.MaxOveruseDamageEvaluator;
import largeflow.emulator.MaxPacketLossDamageEvaluator;
import largeflow.emulator.RealAttackFlowGenerator;
import largeflow.emulator.RealTrafficFlowGenerator;
import largeflow.emulator.UniAttackRateFlowGenerator;
import largeflow.emulator.UniformFlowGenerator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MaxPacketLossDamageEvaluatorTest {

	static private Integer inboundLinkCapacity; // Byte / sec
	static private Integer outboundLinkCapacity; // Byte / sec
	static private Integer timeInterval; // seconds, length of packet stream
	static private Integer largeFlowPacketSize; // Byte, packet size for generated flows
	static private Integer numOfSmallFlows; // number of small flows to generate
	static private Integer numOfLargeFlows; // number of large flows to generate
	static private Integer numOfBurstFlows; // number of burst flows to generate
	static private Integer largeFlowRate; // rate of large flows
	static private Integer burstTolerance; // maximum burst size
	static private Integer smallFlowRate; // rate of small flows
	static private Integer burstFlowSize; // size of each burst
	static private File inputTestTrafficFile;
	static private File realTrafficFile;
	static private File realTrafficOutputFile;
	static private Integer bias;
	static private Integer gamma_h;
	static private Integer gamma_l;
	static private Integer alpha;
	static private Integer beta_l;
	static private Double maxIncubationTime;
	static private Integer numOfRepeatRounds;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	    inboundLinkCapacity = 250000000;
	    outboundLinkCapacity = 25000000;
		numOfRepeatRounds = 2;
		
		// for flow generator
		timeInterval = 10;
		largeFlowPacketSize = 500;
//		numOfSmallFlows = 0;
		numOfLargeFlows = 10;
//		numOfBurstFlows = 0;
		largeFlowRate = outboundLinkCapacity / 200;
		burstTolerance = 4 * 1518;
//		smallFlowRate = 1500;
//		burstFlowSize = 450000;

		// for EARDet
		bias = (int) (0.0 * largeFlowRate);
	    gamma_l = largeFlowRate - bias;
		gamma_h = gamma_l * 2;
		alpha = 1518;
		beta_l = burstTolerance;
		maxIncubationTime = 1.0;

		File outputDir = new File("./data/test");
		inputTestTrafficFile = new File(outputDir.toString()
				+ "/RealAttackFlowGeneratorFlows.txt");
		realTrafficFile = new File(outputDir.toString()
		        + "/realtrace_long.txt");
	    realTrafficOutputFile = new File(outputDir + "/RealTrafficFlowGeneratorFlows.txt");


		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {

	    RealTrafficFlowGenerator realTrafficFlowGenerator 
	        = new RealTrafficFlowGenerator(outboundLinkCapacity, 
	                timeInterval, 
	                realTrafficFile);
	    // use outbound link capacity here to guarantee the real traffic 
	    // always fits the outbound link
	    realTrafficFlowGenerator.setOutputFile(realTrafficOutputFile);
	    realTrafficFlowGenerator.generateFlows();
	    
		RealAttackFlowGenerator flowGenerator = new RealAttackFlowGenerator(
		        inboundLinkCapacity, timeInterval, largeFlowPacketSize, 
		        numOfLargeFlows, largeFlowRate, realTrafficFlowGenerator);
		flowGenerator.setOutputFile(inputTestTrafficFile);

		// setup base detector
		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
				"leakybucket", burstTolerance, largeFlowRate,
				outboundLinkCapacity);

		// setup routers
		AdvancedRouter router1 = new AdvancedRouter("router_eardet", 
		        inboundLinkCapacity, outboundLinkCapacity);
		
		EARDet eardet = new EARDet("eardet", alpha, beta_l, gamma_h, gamma_l,
                maxIncubationTime, outboundLinkCapacity);
		router1.setPostQdDetector(eardet);
		
		
		// setup evaluator
		
		int maxAtkRate = largeFlowRate * 5;
		int minAtkRate = largeFlowRate;
		int atkRateInterval = (maxAtkRate - minAtkRate) / 4;
		int maxNumOfCounters = eardet.getNumOfCounters() * 2;
		int minNumOfCounters = eardet.getNumOfCounters();
		int numOfCounterInterval = (maxNumOfCounters - minNumOfCounters) / 2;
//		int numOfCounterInterval = minNumOfCounters;

		MaxPacketLossDamageEvaluator evaluator = 
		        new MaxPacketLossDamageEvaluator(maxAtkRate, minAtkRate, 
		                atkRateInterval, maxNumOfCounters, 
		                minNumOfCounters, numOfCounterInterval);

		evaluator.setLogger(new Logger(
				"test_MaxPacketLossDamageEvaluator"));
		evaluator.setFlowGenerator(flowGenerator);
		evaluator.setBaseDetector(leakyBucketDetector);
		evaluator.addRouter(router1);
		evaluator.setNumOfRepeatRounds(numOfRepeatRounds);
		evaluator.run();

		
	}
}
