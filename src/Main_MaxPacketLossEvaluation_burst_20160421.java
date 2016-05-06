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

public class Main_MaxPacketLossEvaluation_burst_20160421 {

    public static void main(String[] args) throws Exception {

        // Experiment config
        String expName = "test_packet_loss_exp";
        int startRound = 0;
        int numOfRepeatRounds = 1;
        File rateFile = null;
        
        // read args from command-line
        if (args.length == 1) {
            expName = args[0];
        } else if (args.length >= 3) {
            expName = args[0];
            startRound = Integer.valueOf(args[1]);
            numOfRepeatRounds = Integer.valueOf(args[2]);
        } 
        
        if (args.length == 5 && args[3].equals("--rate")) {
            // each line of the file shows the ratio of the attack rate to the large flow spec
            rateFile = new File(args[4]);
        }
        
        // Network config
        int inboundLinkCapacity = 1000 * 1000 * 1000; // Byte / sec
        int outboundLinkCapacity = 500 * 1000 * 1000; // Byte / sec
        int timeInterval = 10; // seconds, length of packet stream
        int maxPacketSize = NetworkConfig.maxPacketSize;
        
        // for flow generator
        int perFlowReservation = outboundLinkCapacity / 10000; // per-flow reservation bandwidth
        int fullRealFlowPacketSize = 500; // packet size of synthetic full real flows
        int numOfFullRealFlows = 6500;     // number of real flows fully use the reservation
        int numOfUnderUseRealFlows = 500; // number of real flows under use the reservation
        int largeFlowRate = perFlowReservation; // rate of large flows
        int largeFlowPacketSize = 500; // Byte, packet size for generated flows
        int numOfLargeFlows = 10; // number of large flows to generate
        int burstTolerance = maxPacketSize * 4; // maximum burst size
        
        String outputFilePath = "./data/exp_logger/" + expName 
                + "/trace/round_" + startRound + "-" 
                + (startRound + numOfRepeatRounds - 1);
        
        if (rateFile != null) {
            String rateFileName = rateFile.getName().split("\\.txt")[0];
            outputFilePath += "-" + rateFileName;
        }
        
        File outputDir = new File(outputFilePath);
        
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File expInputTrafficFile = new File(outputDir.toString()
                + "/RealAttackFlowGeneratorFlows.txt");
        File realTrafficFile = new File("./data/real_traffic/realtrace_long.txt");
        File realTrafficOutputFile = new File(outputDir + "/RealTrafficFlowGeneratorFlows.txt");
        
        
        // for EARDet
        int gamma_l = largeFlowRate;
        int gamma_h = gamma_l * 4;
        int alpha = maxPacketSize;
        int beta_l = burstTolerance;
        double maxIncubationTime = 1.0;
        
        // for egregious detector
        int eg_gamma = largeFlowRate;
        int eg_burst = burstTolerance;
        double minPeriod = (double)burstTolerance / (double)largeFlowRate;
        double period = 1.5 * minPeriod;
        int tmpNumOfCounters = 100;
        // smarter burst attack flow based on EFD
        double burst_length = period;
        double burst_period = period * 10;
        
        // for FMF
        double T_fmf = 0.1;
        int numOfStages_fmf = 4;
        int sizeOfStage_fmf = 25;
        int threshold_fmf = (int) ((double) largeFlowRate * T_fmf);
        
        // for AMF
        int numOfStages_amf = 4;
        int sizeOfStage_amf = 25;
        int threshold_amf = burstTolerance;
        int drainRate_amf = largeFlowRate;
        
        
        // setup base detector
        LeakyBucketDetector baseDetector = new LeakyBucketDetector(
                "leakybucket", burstTolerance, largeFlowRate,
                outboundLinkCapacity);
        
        // setup EARDet
        EARDet eardet = new EARDet("eardet", alpha, beta_l, gamma_h, gamma_l,
                maxIncubationTime, outboundLinkCapacity);
        
        // setup egregious detector at pre QD
        EgregiousFlowDetector egDetector = new EgregiousFlowDetector("egregious-detector",
                eg_gamma,
                eg_burst,
                period,
                inboundLinkCapacity,
                tmpNumOfCounters);
        egDetector.setEstimatedNumOfFlows(numOfLargeFlows + numOfFullRealFlows + numOfUnderUseRealFlows);
//        egDetector.enableDebug();
        
        // setup FMF
        FMFDetector fmfDetector = new FMFDetector("fmf",
                numOfStages_fmf,
                sizeOfStage_fmf,
                inboundLinkCapacity,
                T_fmf,
                threshold_fmf);
        
        // setup AMF
        AMFDetector amfDetector = new AMFDetector("amf",
                numOfStages_amf,
                sizeOfStage_amf,
                inboundLinkCapacity,
                drainRate_amf,
                threshold_amf);
        
        
        // setup routers
        AdvancedRouter router1 = new AdvancedRouter("router_eardet", 
                inboundLinkCapacity, outboundLinkCapacity);
        router1.setPostQdDetector(eardet);

        AdvancedRouter router2 = new AdvancedRouter("router_eg", 
                inboundLinkCapacity, outboundLinkCapacity);
        router2.setPreQdDetector(egDetector);
        
        AdvancedRouter router3 = new AdvancedRouter("router_fmf", 
                inboundLinkCapacity, outboundLinkCapacity);
        router3.setPreQdDetector(fmfDetector);

        AdvancedRouter router4 = new AdvancedRouter("router_amf", 
                inboundLinkCapacity, outboundLinkCapacity);
        router4.setPreQdDetector(amfDetector);
        
        
        
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
        flowGenerator.setAttackDutyCycleAndPeriod(burst_length, burst_period); // then generate burst flows
        flowGenerator.setOutputFile(expInputTrafficFile);
        
        
        // setup evaluator
        int maxAtkRate = largeFlowRate * 1000;
        int minAtkRate = largeFlowRate;
        int atkRateInterval = largeFlowRate * 20;
//        int minNumOfCounters = eardet.getNumOfCounters() * 2 / 5;
//        int numOfCounterInterval = eardet.getNumOfCounters() / 5;
//        int maxNumOfCounters = eardet.getNumOfCounters() / 5 * 11;
        int minNumOfCounters = 20;
        int numOfCounterInterval = 20;
        int maxNumOfCounters = 200;
        
        List<Integer> atkRateList = new ArrayList<>();
        for (int rate = largeFlowRate; rate < largeFlowRate * 10; rate += largeFlowRate) {
            atkRateList.add(rate);
        }
        for (int rate = largeFlowRate * 10; rate < largeFlowRate * 100; rate += largeFlowRate * 10) {
            atkRateList.add(rate);
        }
        for (int rate = largeFlowRate * 100; rate <= largeFlowRate * 1000; rate += largeFlowRate * 100) {
            atkRateList.add(rate);
        }
        
        List<Integer> numOfCounterList = new ArrayList<>();
        for (int num = 20; num <= 200; num += 20) {
            numOfCounterList.add(num);
        }
        
        if (rateFile != null) {
            // each line of the file shows the ratio of the attack rate to the large flow spec
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
        
        MaxPacketLossDamageEvaluator evaluator = 
                new MaxPacketLossDamageEvaluator(atkRateList, numOfCounterList);
        
        if (args.length == 6) {
            minAtkRate = Integer.valueOf(args[3]);
            maxAtkRate = Integer.valueOf(args[4]);
            atkRateInterval = Integer.valueOf(args[5]);
            evaluator = new MaxPacketLossDamageEvaluator(maxAtkRate, minAtkRate, 
                            atkRateInterval, maxNumOfCounters, 
                            minNumOfCounters, numOfCounterInterval);
        }
        
        

        evaluator.setLogger(new Logger(expName));
        evaluator.setFlowGenerator(flowGenerator);
        evaluator.setBaseDetector(baseDetector);
        evaluator.addRouter(router1);
        evaluator.addRouter(router2);
//        evaluator.addRouter(router3);
        evaluator.addRouter(router4);
        evaluator.setStartRound(startRound);
        evaluator.setNumOfRepeatRounds(numOfRepeatRounds);
        evaluator.run();
        flowGenerator.deleteOutputFile();

    }

}
