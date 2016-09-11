import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import largeflow.eardet.EARDet;
import largeflow.egregiousdetector.EgregiousFlowDetector;
import largeflow.emulator.AdvancedRouter;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.Logger;
import largeflow.emulator.MaxPacketLossDamageEvaluator;
import largeflow.emulator.NetworkConfig;
import largeflow.flowgenerator.RealAttackFlowGenerator;
import largeflow.flowgenerator.RealTrafficFlowGenerator;
import largeflow.multistagefilter.AMFDetector;
import largeflow.multistagefilter.FMFDetector;
import largeflow.multistagefilter.FlowMemoryFactory;
import org.json.JSONObject;

public class Main_MaxPacketLossEvaluation_configurable {

    public static void main(String[] args) throws Exception {

        // Experiment config
        String expName = "test_packet_loss_exp";
        boolean DEBUG = false;

        int startRound = 0;
        int numOfRepeatRounds = 1;
        File rateFile = null;
        File counterFile = null;

        JSONObject config = new JSONObject();        
        for (int i = 0; i < args.length; i += 2) {
            // read the cmd line parameters
            String paraKey = args[i];
            String paraValue = args[i+1];
            if (paraKey.equals("--config") || paraKey.equals("-cfg")) {
                File configFile = new File(paraValue);
                String jsonData = "";
                try(BufferedReader br = new BufferedReader(new FileReader(configFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        jsonData += line + "\n";
                    }
                } catch (Exception e) {
                    throw new Exception("Failed to read the config file.");
                }
                
                config = new JSONObject(jsonData);
            } else if (paraKey.equals("--start_round") || paraKey.equals("-sr")) {
                startRound = Integer.valueOf(paraValue); 
            } else if (paraKey.equals("--repeat_rounds") || paraKey.equals("-rr")) {
                numOfRepeatRounds = Integer.valueOf(paraValue);
            } else if (paraKey.equals("--rate") || paraKey.equals("-r")) {
                rateFile = new File(paraValue);
            } else if (paraKey.equals("--counter") || paraKey.equals("-c")) {
                counterFile = new File(paraValue);
            }
        }
        
        if (config.has("debug_mode")) {
            DEBUG = config.getBoolean("debug_mode");
        }
        if (config.has("exp_name")) {
            expName = config.getString("exp_name");
        }
        JSONObject traffic_config = new JSONObject();
        if (config.has("traffic_config")) {
            traffic_config = config.getJSONObject("traffic_config");
        }
        JSONObject EARDet_config = new JSONObject();
        if (config.has("EARDet_config")) {
            EARDet_config = config.getJSONObject("EARDet_config");
        }
        JSONObject EFD_config = new JSONObject();
        if (config.has("EFD_config")) {
            EFD_config = config.getJSONObject("EFD_config");
        }
        JSONObject FMF_config = new JSONObject();
        if (config.has("FMF_config")) {
            FMF_config = config.getJSONObject("FMF_config");
        }
        JSONObject AMF_config = new JSONObject();
        if (config.has("AMF_config")) {
            AMF_config = config.getJSONObject("AMF_config");
        }
        
        // DEBUG
        if (DEBUG) {
            System.out.println("Config: " + config);
            System.out.println("Start: " + startRound);
            System.out.println("Repeats: " + numOfRepeatRounds);
            System.out.println("rateFile: " + rateFile);
            System.out.println("counterFile: " + counterFile);
        }
        
        // Network config
        int inboundLinkCapacity = 1000 * 1000 * 1000; // Byte / sec
        int outboundLinkCapacity = 500 * 1000 * 1000; // Byte / sec
        int timeInterval = 10; // seconds, length of packet stream
        
        boolean BURST_ATTACK = false; // if false then we do flat attack
        double dutyCycle = 0.1; // from 0 to 1.0
        
        if (traffic_config.length() > 0) {
            inboundLinkCapacity = traffic_config.getInt("inbound_link_capacity");
            outboundLinkCapacity = traffic_config.getInt("outbound_link_capacity");
            timeInterval = traffic_config.getInt("time_interval");
            NetworkConfig.maxPacketSize = traffic_config.getInt("max_packet_size");
            BURST_ATTACK = traffic_config.getBoolean("is_burst_attack");
            dutyCycle = traffic_config.getDouble("burst_duty_cycle_ratio");
        }
        int maxPacketSize = NetworkConfig.maxPacketSize;
        
        // for flow generator
        // per-flow reservation bandwidth
        int perFlowReservation = outboundLinkCapacity / 10000; 
        // packet size of synthetic full real flows
        int fullRealFlowPacketSize = 500; 
        // number of real flows fully use the reservation
        int numOfFullRealFlows = 6500;
        // number of real flows under use the reservation
        int numOfUnderUseRealFlows = 500;
        // Byte, packet size for generated flows
        int largeFlowPacketSize = 500;
        // number of large flows to generate
        int numOfLargeFlows = 10;
        // maximum burst size
        int burstTolerance = maxPacketSize * 4;
        
        if (traffic_config.length() > 0) {
            perFlowReservation = traffic_config.getInt("per_flow_reservation");
            fullRealFlowPacketSize = traffic_config.getInt("full_use_flow_packet_size");
            numOfFullRealFlows = traffic_config.getInt("num_full_use_flows");
            numOfUnderUseRealFlows = traffic_config.getInt("num_under_use_flows");
            largeFlowPacketSize = traffic_config.getInt("attack_flow_packet_size");
            numOfLargeFlows = traffic_config.getInt("num_attack_flows");
            burstTolerance = traffic_config.getInt("flow_spec_burst_tolerance");
        }
        
        // rate of large flows
        int largeFlowRate = perFlowReservation;

        String outputFilePath = "./data/exp_logger/" + expName + "/trace/round_"
                + startRound + "-" + (startRound + numOfRepeatRounds - 1);

        if (rateFile != null) {
            String rateFileName = rateFile.getName().split("\\.txt")[0];
            outputFilePath += "-" + rateFileName;
        }

        File outputDir = new File(outputFilePath);

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File expInputTrafficFile = new File(
                outputDir.toString() + "/RealAttackFlowGeneratorFlows.txt");
        File realTrafficFile = new File(
                "./data/real_traffic/realtrace_long.txt");
        File realTrafficOutputFile = new File(
                outputDir + "/RealTrafficFlowGeneratorFlows.txt");

        // for EARDet
        int gamma_l = largeFlowRate;
        int gamma_h = gamma_l * 4;
        int alpha = maxPacketSize;
        int beta_l = burstTolerance;
        double maxIncubationTime = 1.0;
        
        if (EARDet_config.length() > 0) {
            gamma_l = EARDet_config.getInt("gamma_low");
            gamma_h = EARDet_config.getInt("gamma_high");
            beta_l = EARDet_config.getInt("beta_low");
            maxIncubationTime = EARDet_config.getInt("max_incubation_time");
        }

        // for egregious detector
        int eg_gamma = largeFlowRate;
        int eg_burst = burstTolerance;
        double minPeriod = (double) burstTolerance / (double) largeFlowRate;
        double period = 1.5 * minPeriod;
        int tmpNumOfCounters = 100;
        // smarter burst attack flow based on EFD
        double burst_length = period;
        double burst_period = period / dutyCycle;
        
        boolean split_by_relative_value = false;
        
        if (EFD_config.length() > 0) {
            eg_gamma = EFD_config.getInt("gamma");
            eg_burst = EFD_config.getInt("burst");
            split_by_relative_value = EFD_config.getBoolean("split_by_relative_value");
        }
        
        // for FMF
        double T_fmf = 0.1;
        int numOfStages_fmf = 4;
        int sizeOfStage_fmf = 25;
        int threshold_fmf = (int) ((double) largeFlowRate * T_fmf);
        // ratio of # of counters used in flow memory to the total memory
        double ratioOfFlowMemoryToFMF = 0.5;
        boolean useFlowMemoryFMF = true;

        if (FMF_config.length() > 0) {
            T_fmf = FMF_config.getDouble("period");
            numOfStages_fmf = FMF_config.getInt("num_stages");
            ratioOfFlowMemoryToFMF = FMF_config.getDouble("flow_memory_counter_ratio");
            useFlowMemoryFMF = FMF_config.getBoolean("flow_memory");
        }
        
        // for AMF
        int numOfStages_amf = 4;
        int sizeOfStage_amf = 25;
        int threshold_amf = burstTolerance;
        int drainRate_amf = largeFlowRate;
        // ratio of # of counters used in flow memory to the total memory
        double ratioOfFlowMemoryToAMF = 0.5;
        boolean useFlowMemoryAMF = true;

        if (AMF_config.length() > 0) {
            numOfStages_amf = AMF_config.getInt("num_stages");
            ratioOfFlowMemoryToAMF = AMF_config.getDouble("flow_memory_counter_ratio");
            useFlowMemoryAMF = AMF_config.getBoolean("flow_memory");
        }

        // setup base detector
        LeakyBucketDetector baseDetector = new LeakyBucketDetector(
                "leakybucket",
                burstTolerance,
                largeFlowRate,
                outboundLinkCapacity);

        // setup EARDet
        EARDet eardet = new EARDet("eardet",
                alpha,
                beta_l,
                gamma_h,
                gamma_l,
                maxIncubationTime,
                outboundLinkCapacity);

        // setup egregious detector at pre QD
        EgregiousFlowDetector egDetector = new EgregiousFlowDetector(
                "egregious-detector",
                eg_gamma,
                eg_burst,
                period,
                inboundLinkCapacity,
                tmpNumOfCounters);
        egDetector.setEstimatedNumOfFlows(
                numOfLargeFlows + numOfFullRealFlows + numOfUnderUseRealFlows);
        if (split_by_relative_value) {
            egDetector.splitBucketByRelativeValue();
        }
        // egDetector.enableDebug();

        // setup FMF
        FlowMemoryFactory fm_factory = new FlowMemoryFactory(burstTolerance,
                largeFlowRate,
                outboundLinkCapacity);
        FMFDetector fmfDetector = new FMFDetector("fmf",
                numOfStages_fmf,
                sizeOfStage_fmf,
                inboundLinkCapacity,
                T_fmf,
                threshold_fmf);
        fmfDetector.setRatioOfFlowMemory(ratioOfFlowMemoryToFMF);
        if (useFlowMemoryFMF) {
            fmfDetector.setFlowMemoryFactory(fm_factory);
        }

        // setup AMF
        AMFDetector amfDetector = new AMFDetector("amf",
                numOfStages_amf,
                sizeOfStage_amf,
                inboundLinkCapacity,
                drainRate_amf,
                threshold_amf);
        amfDetector.setRatioOfFlowMemory(ratioOfFlowMemoryToAMF);
        if (useFlowMemoryAMF) {
            amfDetector.setFlowMemoryFactory(fm_factory);
        }
        
        // setup routers
        AdvancedRouter router1 = new AdvancedRouter("router_eardet",
                inboundLinkCapacity,
                outboundLinkCapacity);
        router1.setPostQdDetector(eardet);

        AdvancedRouter router2 = new AdvancedRouter("router_eg",
                inboundLinkCapacity,
                outboundLinkCapacity);
        router2.setPreQdDetector(egDetector);

        AdvancedRouter router3 = new AdvancedRouter("router_fmf",
                inboundLinkCapacity,
                outboundLinkCapacity);
        router3.setPreQdDetector(fmfDetector);

        AdvancedRouter router4 = new AdvancedRouter("router_amf",
                inboundLinkCapacity,
                outboundLinkCapacity);
        router4.setPreQdDetector(amfDetector);

        // setup flow generator
        RealTrafficFlowGenerator realTrafficFlowGenerator = new RealTrafficFlowGenerator(
                outboundLinkCapacity, timeInterval, realTrafficFile);
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
        if (BURST_ATTACK) {
            // then generate burst flows
            System.out.println("===THIS IS BURST ATTACK!===");
            flowGenerator.setAttackDutyCycleAndPeriod(burst_length,
                    burst_period);
        }
        flowGenerator.setOutputFile(expInputTrafficFile);

        // setup evaluator
        List<Integer> atkRateList = new ArrayList<>();
        for (int rate = largeFlowRate; rate < largeFlowRate
                * 10; rate += largeFlowRate) {
            atkRateList.add(rate);
        }
        for (int rate = largeFlowRate * 10; rate < largeFlowRate
                * 100; rate += largeFlowRate * 10) {
            atkRateList.add(rate);
        }
        for (int rate = largeFlowRate * 100; rate <= largeFlowRate
                * 1000; rate += largeFlowRate * 100) {
            atkRateList.add(rate);
        }

        List<Integer> numOfCounterList = new ArrayList<>();
        for (int num = 20; num <= 200; num += 20) {
            numOfCounterList.add(num);
        }

        if (rateFile != null) {
            // each line of the file shows the ratio of the attack rate to the
            // large flow spec
            BufferedReader br = new BufferedReader(new FileReader(rateFile));
            atkRateList = new ArrayList<>();
            String line;

            System.out.println("attack rate to test: ");
            while ((line = br.readLine()) != null) {
                int rate = Integer.valueOf(line) * largeFlowRate;
                atkRateList.add(rate);
                System.out.println(rate + " Byte / sec");
            }
            br.close();
        }

        if (counterFile != null) {
            BufferedReader br = new BufferedReader(new FileReader(counterFile));
            numOfCounterList = new ArrayList<>();
            String line;

            System.out.println("number of counter to test: ");
            while ((line = br.readLine()) != null) {
                int counterNum = Integer.valueOf(line);
                numOfCounterList.add(counterNum);
                System.out.println(counterNum);
            }
            br.close();
        }

        MaxPacketLossDamageEvaluator evaluator = new MaxPacketLossDamageEvaluator(
                atkRateList, numOfCounterList);

        evaluator.setLogger(new Logger(expName));
        evaluator.setFlowGenerator(flowGenerator);
        evaluator.setBaseDetector(baseDetector);
        if (!config.has("run_EARDet") || config.getBoolean("run_EARDet")) {
            evaluator.addRouter(router1);
            System.out.println("Added EARDet router.");
        }
        if (!config.has("run_EFD") || config.getBoolean("run_EFD")) {
            evaluator.addRouter(router2);
            System.out.println("Added EFD router.");
        }
        if (!config.has("run_FMF") || config.getBoolean("run_FMF")) {
            evaluator.addRouter(router3);
            System.out.println("Added FMF router.");
        }
        if (!config.has("run_AMF") || config.getBoolean("run_AMF")) {
            evaluator.addRouter(router4);
            System.out.println("Added AMF router.");
        }
        evaluator.setStartRound(startRound);
        evaluator.setNumOfRepeatRounds(numOfRepeatRounds);
        evaluator.run();
        flowGenerator.deleteOutputFile();

    }

}
