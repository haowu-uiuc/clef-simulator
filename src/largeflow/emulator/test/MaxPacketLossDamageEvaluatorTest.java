package largeflow.emulator.test;

import java.io.File;

import largeflow.eardet.EARDet;
import largeflow.emulator.Logger;
import largeflow.emulator.AdvancedRouter;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.MaxPacketLossDamageEvaluator;
import largeflow.flowgenerator.RealAttackFlowGenerator;
import largeflow.flowgenerator.RealTrafficFlowGenerator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MaxPacketLossDamageEvaluatorTest {

	static private int inboundLinkCapacity; // Byte / sec
	static private int outboundLinkCapacity; // Byte / sec
	static private int timeInterval; // seconds, length of packet stream
	static private int largeFlowPacketSize; // Byte, packet size for generated flows
	static private int numOfLargeFlows; // number of large flows to generate
	static private int largeFlowRate; // rate of large flows
	static private int burstTolerance; // maximum burst size
	
	static private int perFlowReservation; // per-flow reservation bandwidth
	static private int fullRealFlowPacketSize; // packet size of synthetic full real flows
	static private int numOfFullRealFlows;     // number of real flows fully use the reservation
	static private int numOfUnderUseRealFlows; // number of real flows under use the reservation
	
	static private File inputTestTrafficFile;
	static private File realTrafficFile;
	static private File realTrafficOutputFile;
	static private int bias;
	static private int gamma_h;
	static private int gamma_l;
	static private int alpha;
	static private int beta_l;
	static private double maxIncubationTime;
	static private int numOfRepeatRounds;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	    inboundLinkCapacity = 250000000;
	    outboundLinkCapacity = 25000000;
		numOfRepeatRounds = 2;
		
		// for flow generator
	    perFlowReservation = outboundLinkCapacity / 250;
	    fullRealFlowPacketSize = 500;
	    numOfFullRealFlows = 135;
	    numOfUnderUseRealFlows = 15;
	        
		timeInterval = 10;
		largeFlowPacketSize = 500;
//		numOfSmallFlows = 0;
		numOfLargeFlows = 10;
//		numOfBurstFlows = 0;
		largeFlowRate = perFlowReservation;
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

		// setup base detector
		LeakyBucketDetector baseDetector = new LeakyBucketDetector(
				"leakybucket", burstTolerance, largeFlowRate,
				outboundLinkCapacity);

		// setup routers
		AdvancedRouter router1 = new AdvancedRouter("router_eardet", 
		        inboundLinkCapacity, outboundLinkCapacity);
		
		EARDet eardet = new EARDet("eardet", alpha, beta_l, gamma_h, gamma_l,
                maxIncubationTime, outboundLinkCapacity);
		router1.setPostQdDetector(eardet);
		
		// setup flow generator
		RealTrafficFlowGenerator realTrafficFlowGenerator 
        = new RealTrafficFlowGenerator(outboundLinkCapacity, 
                timeInterval, 
                realTrafficFile);
        // use outbound link capacity here to guarantee the real traffic 
        // always fits the outbound link
        realTrafficFlowGenerator.setOutputFile(realTrafficOutputFile);
        realTrafficFlowGenerator.enableLargeRealFlowFilter(baseDetector);
        realTrafficFlowGenerator.generateFlows();
        
        RealAttackFlowGenerator flowGenerator = new RealAttackFlowGenerator(
                inboundLinkCapacity,
                timeInterval,
                largeFlowPacketSize,
                numOfLargeFlows,
                largeFlowRate,
                perFlowReservation,
                fullRealFlowPacketSize,
                numOfFullRealFlows,
                numOfUnderUseRealFlows,
                realTrafficFlowGenerator);
        flowGenerator.setOutputFile(inputTestTrafficFile);
		
		
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
		evaluator.setBaseDetector(baseDetector);
		evaluator.addRouter(router1);
		evaluator.setStartRound(2);
		evaluator.setNumOfRepeatRounds(numOfRepeatRounds);
		evaluator.run();
        flowGenerator.deleteOutputFile();
		
	}
}
