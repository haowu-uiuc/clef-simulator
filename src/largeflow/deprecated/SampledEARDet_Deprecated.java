package largeflow.deprecated;
import java.util.Map;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.eardet.PacketSampler;
import largeflow.emulator.QueueDiscipline;

@Deprecated
public class SampledEARDet_Deprecated {
	
	private Integer alpha;
	private Integer beta_th;
	private Integer linkCapacity;
	private Integer gamma_th; 	//large flow threshold
	private Double sampleRate;	//sample packets with probability of sample rate
	private Integer sampledLinkCapacity;
	
	//parameter to set
	private EARDet_Deprecated eardet;
	private QueueDiscipline gatewayRouter;
	private PacketSampler packetSampler;
	
	public SampledEARDet_Deprecated(){
		alpha = 0;
		beta_th = 0;
		linkCapacity= 0;
		sampledLinkCapacity = 0;
		gamma_th = 0;
		sampleRate = 0.;
		eardet = new EARDet_Deprecated();
	}
	
//	public void setAlpha(Integer alpha){
//		eardet.setAlpha(alpha);
//	}
	
	public void setNumOfCounters(Integer numOfCounters){
		eardet.setNumOfCounters(numOfCounters);
		sampleRate = (double)gamma_th / ((double)linkCapacity / (double)(numOfCounters + 1));
		sampledLinkCapacity = (int)(linkCapacity * sampleRate);
		System.out.println(sampleRate + "\t" + sampledLinkCapacity);
//		sampledLinkCapacity = linkCapacity;
		
		eardet.initEARDet(alpha, beta_th, numOfCounters, sampledLinkCapacity);
		gatewayRouter = new QueueDiscipline(sampledLinkCapacity);
		packetSampler = new PacketSampler(sampleRate);
	}
	
//	public void setBetaTh(Integer beta_th){
//		eardet.setBetaTh(beta_th);
//	}
	
//	public void setGammaTh(Integer gamma_th){
//		this.gamma_th = gamma_th;
//	}
	
//	public void setLinkCapacity(Integer linkCapacity){
//		this.linkCapacity = linkCapacity;
//	}
	
	public void initSampledEARDet(Integer alpha, 
						   Integer beta_th, 
						   Integer numOfCounters, 
						   Integer linkCapacity,
						   Integer gamma_th
						   ){
		this.alpha = alpha;
		this.beta_th = beta_th;
		this.linkCapacity = linkCapacity;
		this.gamma_th = gamma_th;
		sampleRate = (double)gamma_th / ((double)linkCapacity / (double)(numOfCounters + 1));
		sampledLinkCapacity = (int)(linkCapacity * sampleRate);
		
		eardet.initEARDet(alpha, beta_th, numOfCounters, sampledLinkCapacity);
		
		gatewayRouter = new QueueDiscipline(sampledLinkCapacity);
		packetSampler = new PacketSampler(sampleRate);
		
	}
	
	public void reset(){
		eardet.reset();
		gatewayRouter.reset();
	}
	
	public Integer getBetaTh(){
		return eardet.getBetaTh();
	}
	
	public Integer getNumOfCounters(){
		return eardet.getNumOfCounters();
	}
	
	
	public Double getBlackListTime(String flowId){
		// if flow id is not in the map, then return null
		return eardet.getBlackListTime(flowId);
	}	
	
	public Map<FlowId, Double> getBlackList(){
		return eardet.getBlackList();
	}
	
	
	public void processPacket(Packet packet){
				
		//TODO:process packet
		Packet sampledPacket = packetSampler.samplePacket(packet);
		if(sampledPacket == null){
			return;
		}
		
		gatewayRouter.processPacket(sampledPacket);
		
		Packet adjustedPacket;
		while((adjustedPacket = gatewayRouter.getNextPacket()) != null){
			eardet.processPacket(adjustedPacket);
		}		
	}

	
}
