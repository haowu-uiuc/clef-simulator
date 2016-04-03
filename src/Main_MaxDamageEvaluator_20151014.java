import java.io.File;
import java.util.List;
import java.util.Map;

import largeflow.datatype.MaxDamageEvaluatorResult;
import largeflow.eardet.EARDet;
import largeflow.egregiousdetector.EgregiousFlowDetector;
import largeflow.emulator.Logger;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.MaxOveruseDamageEvaluator;
import largeflow.flowgenerator.RandomFlowGenerator;
import largeflow.flowgenerator.UniAttackRateFlowGenerator;
import largeflow.flowgenerator.UniformFlowGenerator;

public class Main_MaxDamageEvaluator_20151014 {

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

	// for Leaky Bucket
	static private Integer rateThreshold;
	static private Integer burstThreshold;

	// for EARDet
	static private Integer gamma_h;
	static private Integer gamma_l;
	static private Integer alpha;
	static private Integer beta_l;
	static private Double maxIncubationTime;

	// for Egregious Flow Detector
	static private Integer gamma;
	static private Integer burst;
	static private Double period; // minPeriod = 6072 / 250000 = 0.025

	// for evaluator
	static private Integer numOfRepeatRounds;
	static private String expName;
	static private Integer maxAtkRate;
	static private Integer minAtkRate;
	static private Integer atkRateInterval;
	static private Integer maxNumOfCounters;
	static private Integer minNumOfCounters;
	static private Integer numOfCounterInterval;

	public static void main(String[] args) throws Exception {

		expName = "20151014_MaxDamageEvaluator_4";
		linkCapacity = 250000000;
		alpha = 1518;

		// for flow generator
		timeInterval = 10;
		packetSize = 100;
		numOfSmallFlows = 950;
		numOfLargeFlows = 50;
		numOfBurstFlows = 0;
		largeFlowRate = linkCapacity / 100; 	// will be re-set in the evaluation
		smallFlowRate = linkCapacity / 5000;	
		burstFlowSize = 0;

		File outputDir = new File("./data");
		inputTestTrafficFile = new File(outputDir.toString()
				+ "/UniformFlowGeneratorFlows.txt");

		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		UniAttackRateFlowGenerator flowGenerator = new UniformFlowGenerator(linkCapacity,
				timeInterval,
				packetSize,
				numOfSmallFlows,
				numOfLargeFlows,
				numOfBurstFlows,
				largeFlowRate,
				smallFlowRate,
				burstFlowSize);
		flowGenerator.setOutputFile(inputTestTrafficFile);
		
		// for leaky bucket
		rateThreshold = linkCapacity / 1000;
		burstThreshold = 4 * alpha;

		LeakyBucketDetector leakyBucketDetector = new LeakyBucketDetector("leakybucket",
				burstThreshold,
				rateThreshold,
				linkCapacity);

		// for EARDet
		gamma_h = rateThreshold * 10;
		gamma_l = rateThreshold;
		beta_l = burstThreshold;
		maxIncubationTime = 1.0;

		EARDet eardet = new EARDet("eardet",
				alpha,
				beta_l,
				gamma_h,
				gamma_l,
				maxIncubationTime,
				linkCapacity);

		// for Egregious Flow Detector
		gamma = rateThreshold;
		burst = burstThreshold;
		period = 0.1; // minPeriod = 6072 / 250000 = 0.025
		int tmpNumOfCounters = 100;
		
		EgregiousFlowDetector egreDetector = new EgregiousFlowDetector("egregious-detector",
				gamma,
				burst,
				period,
				linkCapacity,
				tmpNumOfCounters);

		// for evaluator
		maxAtkRate = (int) (rateThreshold * 15);
		minAtkRate = (int) (rateThreshold / 2);
		atkRateInterval = (maxAtkRate - minAtkRate) / 50;
		maxNumOfCounters = 200;
		minNumOfCounters = 22;
		numOfCounterInterval = (maxNumOfCounters - minNumOfCounters) / 20;
		numOfRepeatRounds = 10;

		MaxOveruseDamageEvaluator maxDamageEvaluator = new MaxOveruseDamageEvaluator(maxAtkRate,
				minAtkRate,
				atkRateInterval,
				maxNumOfCounters,
				minNumOfCounters,
				numOfCounterInterval,
				packetSize,
				linkCapacity);

		maxDamageEvaluator.setLogger(new Logger(expName));
		maxDamageEvaluator.setFlowGenerator(flowGenerator);
		maxDamageEvaluator.setBaseDetector(leakyBucketDetector);
		maxDamageEvaluator.addDetector(eardet);
		maxDamageEvaluator.addDetector(egreDetector);
		maxDamageEvaluator.setNumOfRepeatRounds(numOfRepeatRounds);
		Map<String, List<MaxDamageEvaluatorResult>> resultMap = maxDamageEvaluator
				.run();

		for (String detectorName : resultMap.keySet()) {
			List<MaxDamageEvaluatorResult> detectorResult = resultMap
					.get(detectorName);
			for (int i = 0; i < maxDamageEvaluator.getNumOfRepeatRounds(); i++) {
				System.out.println("Detector: " + detectorName + "\tRound: "
						+ i + "\tRate: " + detectorResult.get(i).atkRate
						+ "\t Counter: " + detectorResult.get(i).numOfCounter
						+ ", Max Damage: " + detectorResult.get(i).maxDamage);
			}
		}
	}

}
