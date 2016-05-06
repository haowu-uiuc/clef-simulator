package largeflow.emulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import largeflow.datatype.FlowId;
import largeflow.datatype.MaxDamageEvaluatorResult;
import largeflow.flowgenerator.UniAttackRateFlowGenerator;

/**
 * Calculate the maximum damage could be caused by all attack flows, among
 * different number of counters and different attack rate
 * 
 * 1. Only count the Best Effort damage in.
 * 2. Assume the inbound link = outbound link.
 * 
 * @author HaoWu
 *
 */
public class MaxOveruseDamageEvaluator {

	private List<Detector> detectorsToEvalList;
	private Detector baseDetector; // ideal detector to indicate flow spec, e.g.
									// leaky bucket filter
	private UniAttackRateFlowGenerator flowGenerator;
	private int maxAtkRate; // bytes / second
	private int minAtkRate; // bytes / second
	private int atkRateInterval; // bytes / second
	private int maxNumOfCounters;
	private int minNumOfCounters;
	private int numOfCounterInterval;
	private int numOfRepeatRounds;
	private int maxPacketSize;
	private int linkCapacity;
	private Logger logger;
	private SingleRouterRunner routerRunner;
	private Map<String, List<MaxDamageEvaluatorResult>> resultMap;

	public MaxOveruseDamageEvaluator() {
		detectorsToEvalList = new ArrayList<>();
		maxAtkRate = -1;
		minAtkRate = -1;
		atkRateInterval = -1;
		maxNumOfCounters = -1;
		minNumOfCounters = -1;
		numOfCounterInterval = -1;
		numOfRepeatRounds = 1;
	}

	public MaxOveruseDamageEvaluator(int maxAtkRate,
			int minAtkRate,
			int atkRateInterval,
			int maxNumOfCounters,
			int minNumOfCounters,
			int numOfCounterInterval,
			int maxPacketSize,
			int linkCapacity) {
		this.linkCapacity = linkCapacity;
		this.maxPacketSize = maxPacketSize;
		detectorsToEvalList = new ArrayList<>();
		configAtkRate(maxAtkRate, minAtkRate, atkRateInterval);
		configNumOfCounters(maxNumOfCounters,
				minNumOfCounters,
				numOfCounterInterval);
	}

	public void configAtkRate(int max,
			int min,
			int interval) {
		maxAtkRate = max;
		minAtkRate = min;
		atkRateInterval = interval;
	}

	public void configNumOfCounters(int max,
			int min,
			int interval) {
		maxNumOfCounters = max;
		minNumOfCounters = min;
		numOfCounterInterval = interval;
	}

	public void setNumOfRepeatRounds(int numOfRepeatRounds) throws Exception {
		if (numOfRepeatRounds <= 0) {
			throw new Exception("numOfRepeatRounds cannot be lower or equal to zero!");
		}
		this.numOfRepeatRounds = numOfRepeatRounds;
	}

	public int getNumOfRepeatRounds() {
		return numOfRepeatRounds;
	}

	public int getMaxPacketSize() {
		return maxPacketSize;
	}

	public int getLinkCapacity() {
		return linkCapacity;
	}

	public void setBaseDetector(Detector baseDetector) {
		this.baseDetector = baseDetector;
	}

	public void addDetector(Detector detector) {
		detectorsToEvalList.add(detector);
	}

	public void setFlowGenerator(UniAttackRateFlowGenerator flowGenerator) {
		this.flowGenerator = flowGenerator;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	/**
	 * run the evaluator and return a result map: {detector name} -> result
	 * 
	 * @return
	 * @throws Exception
	 */
	public Map<String, List<MaxDamageEvaluatorResult>> run() throws Exception {
		if (baseDetector == null) {
			throw new Exception("Please set base detector in the evaluator");
		}

		if (detectorsToEvalList.isEmpty()) {
			throw new Exception("No detector to evaluate, please add detectors");
		}

		if (flowGenerator == null) {
			throw new Exception("Please set flow generator in the evaluator");
		}

		// log configs
		logger.logTestConfig(maxAtkRate,
				minAtkRate,
				maxNumOfCounters,
				minNumOfCounters,
				numOfRepeatRounds,
				maxPacketSize,
				linkCapacity);
		logger.logFlowGeneratorConfig(flowGenerator);
		logger.logDetectorConfig(baseDetector, true);
		for (Detector detector : detectorsToEvalList) {
			logger.logDetectorConfig(detector, false);
		}
		logger.flush();

		// init emulator and result map
		resultMap = new HashMap<>();
		routerRunner = new SingleRouterRunner();
		routerRunner.setBaseDetector(baseDetector);
		for (Detector detector : detectorsToEvalList) {
			List<MaxDamageEvaluatorResult> resultList = new ArrayList<>();
			for (int i = 0; i < numOfRepeatRounds; i++) {
				resultList.add(new MaxDamageEvaluatorResult(-1,
						-1,
						Double.NEGATIVE_INFINITY));
			}
			resultMap.put(detector.name(), resultList);
			routerRunner.addRouter(new SimpleRouter(detector));
		}

		for (int round = 0; round < numOfRepeatRounds; round++) {
			for (Detector detector : detectorsToEvalList) {
				logger.log(detector.name(), "Round # " + round
						+ "\n");
			}

			int i = 0;
			for (int atkRate = minAtkRate; atkRate <= maxAtkRate; atkRate += atkRateInterval) {
				flowGenerator.setAttackRate(atkRate);
				flowGenerator.generateFlows();
				routerRunner.setPacketReader(PacketReaderFactory
						.getPacketReader(flowGenerator.getOutputFile()));

				for (int numOfCounters = minNumOfCounters; numOfCounters <= maxNumOfCounters; numOfCounters += numOfCounterInterval) {
					i++;
					System.out.println(round + "." + i + "\tRate: " + atkRate
							+ "\tCounter: " + numOfCounters);

					try {
						for (Detector detector : detectorsToEvalList) {
							detector.setNumOfCounters(numOfCounters);
						}
						routerRunner.reset();
						routerRunner.run();

						// calculate the damage for each detector
						Map<FlowId, Double> baseBlackList = baseDetector
								.getBlackList();
						for (Detector detector : detectorsToEvalList) {
							double damage = 0;
							int FP = 0; // count of False Positive
							int FN = 0; // count of False Negative
							int TP = 0; // count of True Positive

							Map<FlowId, Double> blackList = detector
									.getBlackList();
							for (FlowId flowId : blackList.keySet()) {
								if (baseBlackList.containsKey(flowId)) {
									if (flowGenerator.isLargeFlow(flowId)) {
										// only consider the damage by large
										// attack flows
										double delay = blackList.get(flowId) - baseBlackList
												.get(flowId);
										if (delay > 0.) {
											damage += delay	* (double) atkRate;
										}
									}
								} else if (!flowGenerator.isLargeFlow(flowId)) {
									// only consider the false positive over
									// non-attack flows
									// false positive happens here
									FP++;
								}
							}

							for (FlowId flowId : baseBlackList.keySet()) {
								// true positive happens here
								if (flowGenerator.isLargeFlow(flowId)) {
									// only consider the large attack flow
									// for true positive and false negative
									TP++;

									if (!blackList.containsKey(flowId)) {
										// false negative happens here
										FN++;
										//damage = Double.POSITIVE_INFINITY;
										damage = (flowGenerator.getTraceLength() 
												- baseBlackList.get(flowId))
												* (double) atkRate;
									}
								}
							}

							double perFlowDamage = Double.NaN;
							if (TP > 0) {
								perFlowDamage = damage / TP;
							}

							// logging
							if (logger != null) {
								logger.log(detector.name(),
										atkRate,
										numOfCounters,
										FP,
										FN,
										TP,
										perFlowDamage);
							}

							MaxDamageEvaluatorResult result = resultMap
									.get(detector.name()).get(round);
							if (result.maxDamage < perFlowDamage) {
								result.maxDamage = perFlowDamage;
								result.atkRate = atkRate;
								result.numOfCounter = numOfCounters;
							}
						}

						logger.flush();
					} catch (Exception e) {
						System.out.println(e.getMessage());
					}					
				}
				System.gc();
			}
		}

		logger.close();

		return resultMap;
	}

}
