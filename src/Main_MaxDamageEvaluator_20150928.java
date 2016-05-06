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

public class Main_MaxDamageEvaluator_20150928 {

	static private Integer linkCapacity; // Byte
	static private Integer timeInterval; // seconds, length of packet stream
	static private Integer packetSize; // Byte, packet size for generated flows
	static private Integer numOfSmallFlows; // number of small flows to generate
	static private Integer numOfLargeFlows; // number of large flows to generate
	static private Integer largeFlowRate; // rate of large flows
	static private Integer smallFlowRate; // rate of small flows
	static private File inputTestTrafficFile;
	static private Integer bias;
	static private Integer gamma_h;
	static private Integer gamma_l;
	static private Integer alpha;
	static private Integer beta_l;
	static private Double maxIncubationTime;
	static private Integer numOfRepeatRounds;
	static private String expName;

	public static void main(String[] args) throws Exception {

		expName = "test_MaxDamageEvaluator";
		linkCapacity = 25000000;
		numOfRepeatRounds = 100;
		
		// for flow generator
		timeInterval = 5;
		packetSize = 100;
		numOfSmallFlows = 0;
		numOfLargeFlows = 10;
		largeFlowRate = linkCapacity / 100;
		smallFlowRate = 1500;

		// for EARDet
		bias = (int) (0.00 * largeFlowRate);
		gamma_h = largeFlowRate - bias;
		gamma_l = gamma_h / 10;
		alpha = 1518;
		beta_l = 4 * alpha;
		maxIncubationTime = 1.0;

		File outputDir = new File("./data");
		inputTestTrafficFile = new File(outputDir.toString()
				+ "/UniformFlowGeneratorFlows.txt");

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		UniAttackRateFlowGenerator flowGenerator = new UniformFlowGenerator(
				linkCapacity, timeInterval, packetSize, numOfSmallFlows,
				numOfLargeFlows, largeFlowRate, smallFlowRate);
		flowGenerator.setOutputFile(inputTestTrafficFile);

		EARDet eardet = new EARDet("eardet", alpha, beta_l, gamma_h, gamma_l,
				maxIncubationTime, linkCapacity);
		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
				"leakybucket", eardet.getBetaL(), eardet.getGammaL(),
				linkCapacity);

		int maxAtkRate = (int) (largeFlowRate * 1.5);
		int minAtkRate = largeFlowRate / 15;
		int atkRateInterval = (maxAtkRate - minAtkRate) / 100;
		int maxNumOfCounters = eardet.getNumOfCounters();
		int minNumOfCounters = eardet.getNumOfCounters() / 10;
		int numOfCounterInterval = (maxNumOfCounters - minNumOfCounters) / 20;
//		int numOfCounterInterval = minNumOfCounters;

		MaxOveruseDamageEvaluator maxDamageEvaluator = new MaxOveruseDamageEvaluator(
				maxAtkRate, minAtkRate, atkRateInterval, maxNumOfCounters,
				minNumOfCounters, numOfCounterInterval, packetSize, linkCapacity);

		maxDamageEvaluator.setLogger(new Logger(expName));
		maxDamageEvaluator.setFlowGenerator(flowGenerator);
		maxDamageEvaluator.setBaseDetector(leakyBucketDetector);
		maxDamageEvaluator.addDetector(eardet);
		maxDamageEvaluator.setNumOfRepeatRounds(numOfRepeatRounds);
		Map<String, List<MaxDamageEvaluatorResult>> resultMap = maxDamageEvaluator
				.run();

		for (String detectorName : resultMap.keySet()) {
			List<MaxDamageEvaluatorResult> eardetResult = resultMap
					.get(detectorName);
			for (int i = 0; i < maxDamageEvaluator.getNumOfRepeatRounds(); i++) {
				System.out.println("Detector: " + detectorName + "\tRound: " + i + "\tRate: "
						+ eardetResult.get(i).atkRate + "\t Counter: "
						+ eardetResult.get(i).numOfCounter + ", Max Damage: "
						+ eardetResult.get(i).maxDamage);
			}
		}
	}

}
