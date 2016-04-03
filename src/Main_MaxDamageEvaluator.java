import java.io.File;
import java.util.List;
import java.util.Map;

import largeflow.datatype.MaxDamageEvaluatorResult;
import largeflow.eardet.EARDet;
import largeflow.emulator.Logger;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.MaxOveruseDamageEvaluator;
import largeflow.flowgenerator.RandomFlowGenerator;
import largeflow.flowgenerator.UniAttackRateFlowGenerator;
import largeflow.flowgenerator.UniformFlowGenerator;

public class Main_MaxDamageEvaluator {

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
	static private String expName;

	public static void main(String[] args) throws Exception {

		expName = "20160929_largePacketSize_random";
		linkCapacity = 25000000;
		numOfRepeatRounds = 20;

		// for flow generator
		timeInterval = 5;
		packetSize = 1518;
		numOfSmallFlows = 0;
		numOfLargeFlows = 10;
		numOfBurstFlows = 0;
		largeFlowRate = linkCapacity / 100;
		smallFlowRate = 1500;
		burstFlowSize = 450000;

		// for EARDet
		bias = (int) (0.00 * largeFlowRate);
		gamma_h = largeFlowRate - bias;
		gamma_l = gamma_h / 10;
		alpha = 1518;
		beta_l = 4 * alpha;
		maxIncubationTime = 1.0;

		File outputDir = new File("./data");
		inputTestTrafficFile = new File(outputDir.toString()
				+ "/GeneratedFlowsForExperiment.txt");

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		
		
		UniAttackRateFlowGenerator flowGenerator = new RandomFlowGenerator(
				linkCapacity, timeInterval, packetSize, numOfSmallFlows,
				numOfLargeFlows, numOfBurstFlows, largeFlowRate, smallFlowRate,
				burstFlowSize);
		flowGenerator.setOutputFile(inputTestTrafficFile);

		EARDet eardet = new EARDet("eardet", alpha, beta_l, gamma_h, gamma_l,
				maxIncubationTime, linkCapacity);
		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector(
				"leakybucket", eardet.getBetaL(), 230000, linkCapacity);

		int maxAtkRate = (int) (gamma_h * 1.5);
		int minAtkRate = (int) (gamma_l / 1.5);
		int atkRateInterval = (maxAtkRate - minAtkRate) / 100;
		int maxNumOfCounters = eardet.getNumOfCounters();
		int minNumOfCounters = eardet.getNumOfCounters();
		// int numOfCounterInterval = (maxNumOfCounters - minNumOfCounters) /
		// 20;
		int numOfCounterInterval = maxNumOfCounters;

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
