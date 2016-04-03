package largeflow.emulator.test;

import java.io.File;
import java.util.List;
import java.util.Map;

import largeflow.datatype.MaxDamageEvaluatorResult;
import largeflow.eardet.EARDet;
import largeflow.emulator.Logger;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.MaxOveruseDamageEvaluator;
import largeflow.flowgenerator.UniAttackRateFlowGenerator;
import largeflow.flowgenerator.UniformFlowGenerator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MaxOveruseDamageEvaluatorTest {

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
	static private Integer bias;
	static private Integer gamma_h;
	static private Integer gamma_l;
	static private Integer alpha;
	static private Integer beta_l;
	static private Double maxIncubationTime;
	static private Integer numOfRepeatRounds;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		linkCapacity = 25000000;
		numOfRepeatRounds = 2;
		
		// for flow generator
		timeInterval = 5;
		packetSize = 100;
		numOfSmallFlows = 0;
		numOfLargeFlows = 10;
		numOfBurstFlows = 0;
		largeFlowRate = linkCapacity / 1000;
		smallFlowRate = 1500;
		burstFlowSize = 450000;

		// for EARDet
		bias = (int) (0.0 * largeFlowRate);
	    gamma_l = largeFlowRate - bias;
		gamma_h = gamma_l * 10;
		alpha = 1518;
		beta_l = 4 * alpha;
		maxIncubationTime = 1.0;

		File outputDir = new File("./data/test");
		inputTestTrafficFile = new File(outputDir.toString()
				+ "/UniformFlowGeneratorFlows.txt");

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

		UniAttackRateFlowGenerator flowGenerator = new UniformFlowGenerator(
				linkCapacity, timeInterval, packetSize, numOfSmallFlows,
				numOfLargeFlows, numOfBurstFlows, largeFlowRate, smallFlowRate,
				burstFlowSize);
		flowGenerator.setOutputFile(inputTestTrafficFile);

		EARDet eardet = new EARDet("eardet", alpha, beta_l, gamma_h, gamma_l,
				maxIncubationTime, linkCapacity);
		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
				"leakybucket", eardet.getBetaL(), largeFlowRate,
				linkCapacity);

		int maxAtkRate = largeFlowRate * 15;
		int minAtkRate = largeFlowRate * 5;
//		int atkRateInterval = (maxAtkRate - minAtkRate) / 10;
		int atkRateInterval = (maxAtkRate - minAtkRate) / 2;
		int maxNumOfCounters = eardet.getNumOfCounters() * 2;
		int minNumOfCounters = eardet.getNumOfCounters();
		int numOfCounterInterval = (maxNumOfCounters - minNumOfCounters) / 2;
//		int numOfCounterInterval = minNumOfCounters;

		MaxOveruseDamageEvaluator maxDamageEvaluator = new MaxOveruseDamageEvaluator(
				maxAtkRate, minAtkRate, atkRateInterval, maxNumOfCounters,
				minNumOfCounters, numOfCounterInterval, packetSize, linkCapacity);

		maxDamageEvaluator.setLogger(new Logger(
				"test_MaxDamageEvaluator"));
		maxDamageEvaluator.setFlowGenerator(flowGenerator);
		maxDamageEvaluator.setBaseDetector(leakyBucketDetector);
		maxDamageEvaluator.addDetector(eardet);
		maxDamageEvaluator.setNumOfRepeatRounds(numOfRepeatRounds);
		Map<String, List<MaxDamageEvaluatorResult>> resultMap = maxDamageEvaluator
				.run();

		List<MaxDamageEvaluatorResult> eardetResult = resultMap.get("eardet");
		for (int i = 0; i < maxDamageEvaluator.getNumOfRepeatRounds(); i++){
			System.out.println("Round: " + i +"\tRate: " + eardetResult.get(i).atkRate + "\t Counter: "
					+ eardetResult.get(i).numOfCounter + ", Max Damage: "
					+ eardetResult.get(i).maxDamage);
		}
	}
}
