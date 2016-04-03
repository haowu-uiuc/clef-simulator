package largeflow.emulator;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import largeflow.datatype.Damage;
import largeflow.datatype.FlowId;
import largeflow.flowgenerator.AttackFlowGenerator;
import largeflow.utils.GenericUtils;

public class Logger implements Closeable, Flushable {

	private String expName;
	private File expDir;
	private Map<String, BufferedWriter> writerMap;
	private BufferedWriter configWriter;

	public Logger(String expName) throws IOException {
		this.expName = expName;
		writerMap = new HashMap<>();
		expDir = new File("./data/exp_logger/" + this.expName);
		if (!expDir.exists()) {
			expDir.mkdirs();
		}

		createConfigWriter();
	}

	public String getExperimentName() {
		return expName;
	}
	
	public boolean deleteLogs() {
		return GenericUtils.deleteDir(expDir);
	}

	private void createConfigWriter() throws IOException{
		configWriter = new BufferedWriter(new FileWriter(new File(
				expDir.toString() + "/readme.txt")));
	}
	
	public void logConfigMsg(String msg) throws IOException{
		if (configWriter == null) {
			createConfigWriter();
		}
		configWriter.write(msg);
	}
	
	public void logDetectorConfig(Detector detector,
			Boolean isBaseDetector) throws IOException {
		if (configWriter == null) {
			createConfigWriter();
		}

		if (isBaseDetector) {
			configWriter.write("=====Base Detector (flow spec)=====\n");
		} else {
			configWriter.write("=====Test Detector=====\n");
		}
		detector.logConfig(this);
		configWriter.newLine();
		configWriter.newLine();
	}
	
	public void logRouterConfig(Router router) throws IOException {
        if (configWriter == null) {
            createConfigWriter();
        }

        configWriter.write("=====Test Router=====\n");
        
        router.logConfig(this);
        configWriter.newLine();
        configWriter.newLine();
    }
	
	public void logFlowGeneratorConfig(AttackFlowGenerator flowGenerator) throws IOException {
		configWriter.write("=====Flow Generator=====\n");
		flowGenerator.logConfig(this);
		configWriter.newLine();
		configWriter.newLine();
	}
	
	public void logTestConfig(int maxAtkRate, int minAtkRate, int atkRateInterval, 
			int maxNumOfCounters, int minNumOfCounters, int numOfCounterInterval,
			int numOfRepeatRounds, int maxPacketSize, int linkCapacity) throws IOException{
		logTestConfigHelper(maxAtkRate, minAtkRate, atkRateInterval, 
				maxNumOfCounters, minNumOfCounters, numOfCounterInterval, 
				numOfRepeatRounds, maxPacketSize, linkCapacity);
		configWriter.newLine();
	}
	
	public void logTestConfig(int maxAtkRate, int minAtkRate, int atkRateInterval, 
			int maxNumOfCounters, int minNumOfCounters, int numOfCounterInterval,
			int numOfRepeatRounds, int maxPacketSize, int linkCapacity, 
			int priorityLinkCapacity, int bestEffortLinkCapacity, double bestEffortRatio) throws IOException{
		logTestConfigHelper(maxAtkRate, minAtkRate, atkRateInterval, 
				maxNumOfCounters, minNumOfCounters, numOfCounterInterval, 
				numOfRepeatRounds, maxPacketSize, linkCapacity);
		configWriter.write("Priority Link Capacity: " + priorityLinkCapacity + " Byte / sec");
		configWriter.newLine();
		configWriter.write("Best-Effort Link Capacity: " + bestEffortLinkCapacity + " Byte / sec");
		configWriter.newLine();
		configWriter.write("Best Effort Ratio: " + bestEffortRatio);
		configWriter.newLine();
		configWriter.newLine();
	}

	private void logTestConfigHelper(int maxAtkRate, int minAtkRate, int atkRateInterval, 
			int maxNumOfCounters, int minNumOfCounters, int numOfCounterInterval,
			int numOfRepeatRounds, int maxPacketSize, int linkCapacity) throws IOException{
		configWriter.write("=====Test Config=====\n");	
		configWriter.write("Number of Repeated Rounds: " + numOfRepeatRounds);
		configWriter.newLine();
		configWriter.write("Attack Rate Range: [" + minAtkRate + ", " + maxAtkRate + "] Byte / sec");
		configWriter.newLine();
		configWriter.write("Attack Rate Interval: " + atkRateInterval + " Byte / sec");
		configWriter.newLine();
		configWriter.write("Counter Number Range: [" + minNumOfCounters + ", " + maxNumOfCounters + "]");
		configWriter.newLine();
		configWriter.write("Counter Number Interval: " + numOfCounterInterval);
		configWriter.newLine();
		configWriter.write("Maximum Packet Size: " + maxPacketSize + " Byte");
		configWriter.newLine();
		configWriter.write("Link Capacity: " + linkCapacity + " Byte / sec");
		configWriter.newLine();
	}
	
	/**
	 * 
	 * @param writerKey key to distinguish different detector/router
	 * @param atkRate
	 * @param numOfCounters
	 * @param FP
	 * @param FN
	 * @param TP
	 * @param damage
	 * @throws Exception
	 */
	public void log(String writerKey, int atkRate, int numOfCounters,
			int FP, int FN, int TP, double damage) throws Exception {
		BufferedWriter writer = getWriter(writerKey);
		if (writer == null) {
			throw new Exception(
					"Cannot retrieve log writer for logger in class "
							+ this.getClass().getName());
		}

		List<String> valueList = Arrays.asList(String.valueOf(atkRate),
				String.valueOf(numOfCounters), String.valueOf(FP),
				String.valueOf(FN), String.valueOf(TP), String.valueOf(damage));
		writer.write(String.join("\t", valueList) + "\n");
	}

	/**
	 * 
	 * @param writerKey key to distinguish different detector/router
	 * @param msg
	 * @throws Exception
	 */
	public void log(String writerKey, String msg) throws Exception {
		BufferedWriter writer = getWriter(writerKey);
		if (writer == null) {
			throw new Exception(
					"Cannot retrieve log writer for logger in class "
							+ this.getClass().getName());
		}

		writer.write(msg);
	}
	
	public void logRouterDamage(Router router, int atkRate, int numOfCounters,
            Damage damage) throws Exception {
        BufferedWriter writer = getDamageWriter(router.name());
        if (writer == null) {
            throw new Exception(
                    "Cannot retrieve damage log writer for logger in class "
                            + this.getClass().getName());
        }

        List<String> valueList = Arrays.asList(String.valueOf(atkRate),
                String.valueOf(numOfCounters),
                String.valueOf(damage.FP),
                String.valueOf(damage.FN),
                String.valueOf(damage.TP),
                String.valueOf(damage.perFlowDamage),
                String.valueOf(damage.totalDamage),
                String.valueOf(damage.BE_damage),
                String.valueOf(damage.FP_damage),
                String.valueOf(damage.QD_drop_damage));
        writer.write(String.join("\t", valueList) + "\n");
    }
	
	public void logRouterDamage(Router router, String msg) throws Exception {
        BufferedWriter writer = getDamageWriter(router.name());
        if (writer == null) {
            throw new Exception(
                    "Cannot retrieve log writer for logger in class "
                            + this.getClass().getName());
        }

        writer.write(msg);
    }
	
	public void logRouterBlackList(Router router, int atkRate,
	        int numOfCounters, int round, Detector baseDetector) throws Exception {
	    File dir = new File(expDir + "/blacklist/" + router.name() + "/" + round);
	    if (!dir.exists()) {
	        dir.mkdirs();
	    }
	    
        BufferedWriter writer = new BufferedWriter(new FileWriter(
                new File(dir + "/" + atkRate + "-" + numOfCounters + ".txt")));
                
        // log blacklist of the router
        for (Map.Entry<FlowId, Double> entry : router.getBlackList().entrySet()) {
            Integer droppedTraffic;
            if ((droppedTraffic = router.getDroppdTrafficMap().get(entry.getKey())) == null) {
                droppedTraffic = 0;
            }
            
            if (baseDetector.getBlackList().containsKey(entry.getKey())) {
                writer.write(entry.getKey() + " : " + entry.getValue() + " " + droppedTraffic + " TP");
            } else {
                writer.write(entry.getKey() + " : " + entry.getValue() + " " + droppedTraffic + " FP");   
            }
            writer.newLine();
        }
        
        writer.close();
    }
	
	public void logBaseDetectorBlackList(Detector baseDetector, int atkRate,
	        int round) throws Exception {
        File dir = new File(expDir + "/blacklist/ground_truth/" + round);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(
                new File(dir + "/" + atkRate + ".txt")));
        
        // log blacklist of base detector
        for (Map.Entry<FlowId, Double> entry : baseDetector.getBlackList().entrySet()) {
            writer.write(entry.getKey() + " : " + entry.getValue());
            writer.newLine();
        }
        
        writer.close();
    }
	
	public void logRouterTotalTrafficVolume(Router router, 
	        int atkRate, int numOfCounters, int round,
	        long attackReservedTraffic, long preQdRealTraffic,
	        long postQdAttackTraffic, long postQdRealTraffic, 
	        long blockedRealTraffic, long outboundCapacity) throws Exception {
	    BufferedWriter writer = getTotalTrafficWriter(router.name());
        if (writer == null) {
            throw new Exception(
                    "Cannot retrieve damage log writer for logger in class "
                            + this.getClass().getName());
        }

        List<String> valueList = Arrays.asList(
                String.valueOf(round),
                String.valueOf(atkRate),
                String.valueOf(numOfCounters),
                String.valueOf(attackReservedTraffic),
                String.valueOf(preQdRealTraffic),
                String.valueOf(postQdAttackTraffic),
                String.valueOf(postQdRealTraffic),
                String.valueOf(blockedRealTraffic),
                String.valueOf(outboundCapacity));
        writer.write(String.join("\t", valueList) + "\n");
	}

	@Override
	public void close() throws IOException {
		for (String key : writerMap.keySet()) {
			writerMap.get(key).close();
		}
		writerMap.clear();
		configWriter.close();
	}

	@Override
	public void flush() throws IOException {
		for (String key : writerMap.keySet()) {
			writerMap.get(key).flush();
		}
		configWriter.flush();
	}

	private BufferedWriter getWriter(String writerKey) throws IOException {
		BufferedWriter writer;
		if (!writerMap.containsKey(writerKey)) {
			writer = new BufferedWriter(new FileWriter(new File(
					expDir.toString() + "/" + writerKey + ".txt")));
			writerMap.put(writerKey, writer);

			List<String> columnNameList = Arrays.asList("attack_rate",
					"number_of_counters", "FP", "FN", "TP", "damage");
			writer.write(String.join("\t", columnNameList) + "\n");
		} else {
			writer = writerMap.get(writerKey);
		}

		return writer;
	}
	
    private BufferedWriter getDamageWriter(String writerKey) throws IOException {
        BufferedWriter writer;
        String key = writerKey  + "_damage";
        if (!writerMap.containsKey(key)) {
            writer = new BufferedWriter(new FileWriter(new File(
                    expDir.toString() + "/" + key + ".txt")));
            writerMap.put(key, writer);

            writer.write("attack_rate\t");
            writer.write("number_of_counters\t");
            List<String> columnNameList = Damage.getTitleList();            
            writer.write(String.join("\t", columnNameList) + "\n");
        } else {
            writer = writerMap.get(key);
        }

        return writer;
    }
    
    private BufferedWriter getTotalTrafficWriter(String writerKey) throws IOException {
        BufferedWriter writer;
        String key = writerKey  + "_total_traffic";
        if (!writerMap.containsKey(key)) {
            writer = new BufferedWriter(new FileWriter(new File(
                    expDir.toString() + "/" + key + ".txt")));
            writerMap.put(key, writer);

            writer.write("round\t");
            writer.write("attack_rate\t");
            writer.write("number_of_counters\t");
            writer.write("attack_reservation\t");
            writer.write("pre_QD_real_traffic\t");
            writer.write("post_QD_attack_traffic\t");
            writer.write("post_QD_real_traffic\t");
            writer.write("blocked_real_traffic\t");
            writer.write("outbound_link_capacity\n");
            
        } else {
            writer = writerMap.get(key);
        }

        return writer;
    }
}
