package largeflow.deprecated;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;

@Deprecated
public class MemExperiment implements Experiment{

	int alpha;
	int beta_l;
	int beta_h;
	int gamma_l;
	int gamma_h;
	int atkFlowRate;
	int atkFlowPacketSize;
	int atkFlowNum;
	int atkFlowLength;
	double maxIncubationTime;
	int linkCapacity;
	EARDet_Deprecated eardet;
	SampledEARDet_Deprecated sampledEARDet;
	LeakyBucketFilter_Deprecated leakyBucketH;
	LeakyBucketFilter_Deprecated leakyBucketL;
	int maxN, minN;
	
	
	public MemExperiment(int alpha, int beta_l, int gamma_l, int gamma_h, 
					     double maxIncubationTime, int linkCapacity, 
					     int atkFlowRate, int atkFlowPacketSize, int atkFlowNum, int atkFlowLength) {
		this.atkFlowRate = atkFlowRate;
		this.atkFlowPacketSize = atkFlowPacketSize;
		this.atkFlowNum = atkFlowNum;
		this.atkFlowLength = atkFlowLength;
		
		this.alpha = alpha;
		this.beta_l = beta_l;
		this.gamma_l = gamma_l;
		this.gamma_h = gamma_h;
		this.maxIncubationTime = maxIncubationTime;
		this.linkCapacity = linkCapacity;
		
		
		eardet = new EARDet_Deprecated();
		eardet.initEARDet(alpha, beta_l, gamma_h, gamma_l, maxIncubationTime, linkCapacity);
		maxN = eardet.getNumOfCounters();
		minN = (int)(maxN * 0.05);
		beta_h = eardet.getBetaH();
		
		sampledEARDet = new SampledEARDet_Deprecated();
		sampledEARDet.initSampledEARDet(alpha, eardet.getBetaTh(), maxN, linkCapacity, eardet.getGammaH());
		
		leakyBucketH = new LeakyBucketFilter_Deprecated(beta_h, gamma_h, linkCapacity);
		leakyBucketL = new LeakyBucketFilter_Deprecated(beta_l, gamma_l, linkCapacity);
		
	}
	
	
	@Override
	public void run() throws IOException {

		BufferedWriter bwMean = new BufferedWriter(new FileWriter(new File("./data/MemExp/mean.txt")));
		BufferedWriter bwMedian = new BufferedWriter(new FileWriter(new File("./data/MemExp/median.txt")));
		BufferedWriter bwNinty = new BufferedWriter(new FileWriter(new File("./data/MemExp/ninty.txt")));
		BufferedWriter bwFN = new BufferedWriter(new FileWriter(new File("./data/MemExp/FN.txt")));
		BufferedWriter bwFP = new BufferedWriter(new FileWriter(new File("./data/MemExp/FP.txt")));

		
		//generate flows
		EARDetFlowGenerator_Deprecated flowGenerator = new EARDetFlowGenerator_Deprecated(linkCapacity, atkFlowLength, atkFlowPacketSize, 0, atkFlowNum, 0, 
				atkFlowRate, (int)(gamma_l * 0.8), 0);

		flowGenerator.generateFlowsWithUniformToFile("./data/MemExp/largeFlow.txt");
		
		//for eardet with different memeory
		for(int numOfCounters = maxN; numOfCounters >= minN; numOfCounters -= (int)(maxN * 0.05)){
			System.out.println("Run with Memory = " + numOfCounters);
			//set the numOfCounters
			eardet.setNumOfCounters(numOfCounters);
			sampledEARDet.setNumOfCounters(numOfCounters);
			
			//init
			Map<FlowId, Double> eardetFlowToCatchDelayMap = new HashMap<>();
			Map<FlowId, Double> sampleEARDetFlowToCatchDelayMap = new HashMap<>();			
			
			PacketReader packetReader = PacketReaderFactory.getPacketReader("./data/MemExp/largeFlow.txt");
			
			Packet packet;
			
			while((packet = packetReader.getNextPacket()) != null){
				eardet.processPacket(packet);
				sampledEARDet.processPacket(packet);
				leakyBucketH.processPacket(packet);
				leakyBucketL.processPacket(packet);
			}

			
			Map<FlowId, Double> blackListEARDet = eardet.getBlackList();
			Map<FlowId, Double> blackListSampleEARDet = sampledEARDet.getBlackList();
			Map<FlowId, Double> blackListHigh = leakyBucketH.getBlackList();
			Map<FlowId, Double> blackListLow = leakyBucketL.getBlackList();
			
			Set<FlowId> eardetFNSet = new HashSet<>(blackListLow.keySet());
			eardetFNSet.removeAll(blackListEARDet.keySet());
			Set<FlowId> eardetFPSet = new HashSet<>(blackListEARDet.keySet());
			eardetFPSet.removeAll(blackListLow.keySet());
			
			Set<FlowId> sampleEARDetFNSet = new HashSet<>(blackListLow.keySet());
			sampleEARDetFNSet.removeAll(blackListSampleEARDet.keySet());
			Set<FlowId> sampleEARDetFPSet = new HashSet<>(blackListSampleEARDet.keySet());
			sampleEARDetFPSet.removeAll(blackListLow.keySet());
			
			Set<FlowId> TPSet = new HashSet<>(blackListLow.keySet());
			
			
			for(FlowId key : TPSet){
				Double eardetTimeToCatch = Double.POSITIVE_INFINITY;
				Double sampleEARDetTimeToCatch = Double.POSITIVE_INFINITY;
				
				
				if(blackListEARDet.containsKey(key)){
					eardetTimeToCatch = blackListEARDet.get(key);
				}
				
				if(blackListSampleEARDet.containsKey(key)){
					sampleEARDetTimeToCatch = blackListSampleEARDet.get(key);
				}
				
				Double timeToViolate = blackListLow.get(key);
				
				eardetFlowToCatchDelayMap.put(key, eardetTimeToCatch - timeToViolate);
				sampleEARDetFlowToCatchDelayMap.put(key, sampleEARDetTimeToCatch - timeToViolate);

			}

			//sort the catchDelay in a list
			//for eardet
			ArrayList<Double> eardetCatchDelayList = new ArrayList<>(eardetFlowToCatchDelayMap.values());
			Collections.sort(eardetCatchDelayList);
			double eardetMax = eardetCatchDelayList.get(eardetCatchDelayList.size() - 1);
			double eardetMedian = (eardetCatchDelayList.get((eardetCatchDelayList.size()-1) / 2) + eardetCatchDelayList.get(eardetCatchDelayList.size() / 2)) / 2;
			double eardetNinty = eardetCatchDelayList.get((int)(eardetCatchDelayList.size() * 0.9) - 1);

			double eardetMean = 0;
			for(int i = 0; i < eardetCatchDelayList.size(); i++){
				eardetMean += eardetCatchDelayList.get(i);
			}
			eardetMean /= eardetCatchDelayList.size();
			
			//for sampled eardet
			ArrayList<Double> sampleEARDetCatchDelayList = new ArrayList<>(sampleEARDetFlowToCatchDelayMap.values());
			Collections.sort(sampleEARDetCatchDelayList);
			double sampleEARDetMax = sampleEARDetCatchDelayList.get(sampleEARDetCatchDelayList.size() - 1);
			double sampleEARDetMedian = (sampleEARDetCatchDelayList.get((sampleEARDetCatchDelayList.size()-1) / 2) + sampleEARDetCatchDelayList.get(sampleEARDetCatchDelayList.size() / 2)) / 2;
			double sampleEARDetNinty = sampleEARDetCatchDelayList.get((int)(sampleEARDetCatchDelayList.size() * 0.9) - 1);

			double sampleEARDetMean = 0;
			for(int i = 0; i < sampleEARDetCatchDelayList.size(); i++){
				sampleEARDetMean += sampleEARDetCatchDelayList.get(i);
			}
			sampleEARDetMean /= sampleEARDetCatchDelayList.size();
			
			
			//output result into files
			bwMean.write(numOfCounters + "\t" + eardetMean + "\t" + sampleEARDetMean + "\n");
			bwMedian.write(numOfCounters + "\t" + eardetMedian + "\t" + sampleEARDetMedian + "\n");
			bwNinty.write(numOfCounters + "\t" + eardetNinty + "\t" + sampleEARDetNinty + "\n");
			bwFN.write(numOfCounters + "\t" + ((double)eardetFNSet.size() / (double)TPSet.size()) + "\t" + ((double)sampleEARDetFNSet.size() / (double)TPSet.size()) + "\n");
			bwFP.write(numOfCounters + "\t" + (double)eardetFPSet.size() / (atkFlowNum - TPSet.size()) + "\t" + (double)sampleEARDetFPSet.size() / (atkFlowNum - TPSet.size()) + "\n");
			
		}
		
		
		
		bwMean.close();
		bwMedian.close();
		bwNinty.close();
		bwFN.close();
		bwFP.close();
	}

}
