package largeflow.deprecated;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Deprecated
public class EARDetFlowGenerator_Deprecated {
	
//	time_interval = int(sys.argv[3]); #5 seconds
//	pkt_size = 1500; # 500 Byte
//	link_capacity = 40000000; # B/s
//	max_pkt_per_second = int(link_capacity / pkt_size); # 30000pkts/s
//	num_small_flows = 2000;
//	num_burst = 0;
//	num_elephants = 5;
//	num__flows = num_small_flows + num_burst + num_elephants;
//	burst_size = 50; #50 contiunous pkts per burst
//	elephant_flow_rate = int(sys.argv[2]); # pkts/s in total
//	small_flow_rate = 10; # pkts/s rate of small flow
//	total_pkts = max_pkt_per_second * time_interval; #35,000 pkts
//	stream = [0 for i in range(total_pkts)];
	
	
	private Integer linkCapacity;	//Byte
	private Integer timeInterval; 	//seconds length of packet stream
	private Integer packetSize;		//Byte, packet size for generated flows
	private Integer numOfSmallFlows;	//number of small flows to generate
	private Integer numOfLargeFlows;	//number of large flows to generate
	private Integer numOfBurstFlows;	//number of burst flows to generate
	private Integer largeFlowRate;		//rate of large flows
	private Integer smallFlowRate;		//rate of small flows
	private Integer burstFlowSize;		//size of each burst
	
	private Integer totalNumPackets;
	private Integer maxNumOfPacketsPerSecond;
	
	public EARDetFlowGenerator_Deprecated(Integer linkCapacity, 
						 Integer timeInterval,
						 Integer packetSize,
						 Integer numOfSmallFlows,
						 Integer numOfLargeFlows,
						 Integer numOfBurstFlows,
						 Integer largeFlowRate,
						 Integer smallFlowRate,
						 Integer burstFlowSize){
		this.linkCapacity = linkCapacity;
		this.timeInterval = timeInterval;
		this.packetSize = packetSize;
		this.numOfSmallFlows = numOfSmallFlows;
		this.numOfLargeFlows = numOfLargeFlows;
		this.numOfBurstFlows = numOfBurstFlows;
		this.largeFlowRate = largeFlowRate;
		this.smallFlowRate = smallFlowRate;
		this.burstFlowSize = burstFlowSize;
	
		maxNumOfPacketsPerSecond = linkCapacity / packetSize;
		totalNumPackets = maxNumOfPacketsPerSecond * timeInterval;
	}
	
	public void setLinkCapacity(Integer linkCapacity){
		this.linkCapacity = linkCapacity;
	}
	
	public void setTimeInterval(Integer timeInterval){
		this.timeInterval = timeInterval;
	}
	
	public void setPacketSize(Integer packetSize){
		this.packetSize = packetSize;
	}
	
	public void setNumOfSmallFlows(Integer numOfSmallFlows){
		this.numOfSmallFlows = numOfSmallFlows;
	}
	
	public void setNumOfLargeFlows(Integer numOfLargeFlows){
		this.numOfSmallFlows = numOfLargeFlows;
	}
	
	public void setNumOfBurstFlows(Integer numOfBurstFlows){
		this.numOfSmallFlows = numOfBurstFlows;
	}
	
	public void setLargeFlowRate(Integer largeFlowRate){
		this.largeFlowRate = largeFlowRate;
	}
	
	public void setSmallFlowRate(Integer smallFlowRate){
		this.smallFlowRate = smallFlowRate;
	}
	
	public void setBurstFlowSize(Integer burstFlowSize){
		this.burstFlowSize = burstFlowSize;
	}
	
	public void generateFlowsWithRandomnessToFile(String outputFilePath) throws IOException{
		File outputFile = new File(outputFilePath);
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
		int[] streamBuf = new int[maxNumOfPacketsPerSecond];
//		int streamBufTimeInterval = 5;
		
		//TODO: generate burst
		
		//generate large flow
		for(int s = 0; s < timeInterval; s++){
			for(int j = 0; j < streamBuf.length; j++){
				streamBuf[j] = 0;
			}
			int emptyBuf = streamBuf.length;
			
			for(int fid = 1; fid <= numOfLargeFlows; fid++){
				int i = 0;
				while(i < largeFlowRate / packetSize && emptyBuf > 0){
					int pos = (int)(Math.random() * maxNumOfPacketsPerSecond - 0.00001);
					if(streamBuf[pos] != 0){
						continue;
					}
					
					streamBuf[pos] = fid;
					i++;
					emptyBuf--;
				}
			}
			
			//write the second into file
			for(int i = 0; i < maxNumOfPacketsPerSecond; i++){
				if(streamBuf[i] == 0){
					//TODO: generate small flows
				}
				
				if(streamBuf[i] != 0){
					bw.write(streamBuf[i] + " " + packetSize + " " + ((double)i * (double)packetSize / (double)linkCapacity + 0.0001 + s) + "\n");
				}
			}
			
		}

		
		
		bw.close();
	}
	
	
	public void generateFlowsWithUniformToFile(String outputFilePath) throws IOException{
		File outputFile = new File(outputFilePath);
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
		int[] streamBuf = new int[maxNumOfPacketsPerSecond * timeInterval];
		
		double[] flowTime = new double[numOfLargeFlows];
		double packetTimeIntervals = (double)packetSize / (double)largeFlowRate;
				
		
		//TODO: generate burst
		
		//randomly choose a starting time
		for(int fid = 1; fid <= numOfLargeFlows; fid++){
			while(true){
				int pos = (int)(Math.random() * maxNumOfPacketsPerSecond - 0.00001);
				if(streamBuf[pos] == 0){
					streamBuf[pos] = fid;
					flowTime[fid - 1] = (double)pos * (double)packetSize / (double)linkCapacity;
					flowTime[fid - 1] += packetTimeIntervals;
					
					break;
				}
			}
		}
		
		//generate large flow in uniform distribution
		for(int fid = 1; fid <= numOfLargeFlows; fid++){
			while(flowTime[fid - 1] < timeInterval){
				int s = (int)flowTime[fid - 1];
				int pos = (int)((flowTime[fid - 1] - s) * maxNumOfPacketsPerSecond - 0.00001) + s * maxNumOfPacketsPerSecond;
				
				while(streamBuf[pos] != 0 && pos < streamBuf.length){
					pos++;
				}
				
				if(pos >= streamBuf.length){
					break;
				}
				
				streamBuf[pos] = fid;
				flowTime[fid - 1] += packetTimeIntervals;

			}
			
		}
		
		//write the stream into file
		for(int i = 0; i < streamBuf.length; i++){
			if(streamBuf[i] == 0){
				//TODO: generate small flows
			}
			
			if(streamBuf[i] != 0){
				bw.write(streamBuf[i] + " " + packetSize + " " + ((double)i * (double)packetSize / (double)linkCapacity + 0.0001) + "\n");
			}
		}
		
		
		bw.close();
	}
	
	
}
