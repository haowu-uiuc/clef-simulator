import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import largeflow.deprecated.MemExperiment;


public class Main {

	public static void main(String[] args) throws IOException {

		int alpha = 1518;
		int beta_l = 6072;
		int gamma_l = 25000;
		int gamma_h = 250000;
		double maxIncubationTime = 0.8370;
		int linkCapacity = 25000000;
		
		int atkFlowRate = (int)(gamma_h * 0.8);
		int atkFlowPacketSize = 100;
		int atkFlowNum = 50;
		int atkFlowLength = 30; //20 seconds
		
		MemExperiment memExperiment = new MemExperiment(alpha, beta_l, gamma_l, gamma_h, maxIncubationTime, linkCapacity, 
														atkFlowRate, atkFlowPacketSize, atkFlowNum, atkFlowLength);
		memExperiment.run();
		
		
		
//		EARDet eardet = new EARDet();		
//		eardet.initEARDet(alpha, beta_l, gamma_h, gamma_l, maxIncubationTime, linkCapacity);
//		int beta_h = eardet.getBetaH();
//		
//		LeakyBucketFilter leakyBucketFilter_h = new LeakyBucketFilter(beta_h, gamma_h, linkCapacity);
//		LeakyBucketFilter leakyBucketFilter_l = new LeakyBucketFilter(beta_l, gamma_l, linkCapacity);
//		
//		int beta_th = eardet.getBetaTh();
//		int n = eardet.getNumOfCounters();
//		
//		System.out.println("Num Of counters = " + n);
//		System.out.println("Beta_TH = " + beta_th);
//		System.out.println("Beta_H = " + beta_h);
//		System.out.println("Beta_L = " + beta_l);
//		
//		
//		
//		
//		
//		FlowGenerator flowGenerator = new FlowGenerator(linkCapacity, 5, 100, 0, 50, 0, 
//				(int)(gamma_h * 0.8), (int)(gamma_l * 0.8), 0);
//
//		flowGenerator.generateFlowsToFile("./data/largeFlow.txt");
//		
//		//read packet stream
//		
////		PacketReader packetReader = new PacketReader("./data/realtrace_short_adjusted_with_atk_burstflow.txt");
//		PacketReader packetReader = new PacketReader("./data/largeFlow.txt");
//		
//		Packet packet;
//		
//		while((packet = packetReader.getNextPacket()) != null){
//			
//			eardet.processPacket(packet);
//			leakyBucketFilter_h.processPacket(packet);
//			leakyBucketFilter_l.processPacket(packet);
//			
//		}
//		
//		Map<String, Double> blackListEARDet = eardet.getBlackList();
//		Map<String, Double> blackListHigh = leakyBucketFilter_h.getBlackList();
//		Map<String, Double> blackListLow = leakyBucketFilter_l.getBlackList();
//		
//		Set<String> FNSet = new HashSet<>(blackListHigh.keySet());
//		FNSet.removeAll(blackListEARDet.keySet());
//		Set<String> FPSet = new HashSet<>(blackListEARDet.keySet());
//		FPSet.removeAll(blackListLow.keySet());
//		
//		
//		System.out.println("FN Set Size : " + FNSet.size());
//		System.out.println("FP Set Size : " + FPSet.size());
//		System.out.println("EADRDet Black List : " + blackListEARDet.size());
//		System.out.println("LeakyBucket High Bandwidth Threshold Black List : " + blackListHigh.size());
//		System.out.println("LeakyBucket Low Bandwidth Threshold Black List : " + blackListLow.size());
//		System.out.println(eardet.getBlackList());
//		System.out.println("FP Set: " + FPSet);
		
	}

}
