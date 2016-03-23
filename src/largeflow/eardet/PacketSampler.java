package largeflow.eardet;
import java.util.Random;

import largeflow.datatype.Packet;


public class PacketSampler {

	private Double sampleRate;
	private Random randGenerator;
	
	public PacketSampler(Double sampleRate){
		this.sampleRate = sampleRate;
		randGenerator = new Random(System.currentTimeMillis());
	}
	
	public Packet samplePacket(Packet packet){
		double rand = randGenerator.nextDouble();
		if(rand < sampleRate){
			return packet;
		}
		
		return null;
	}
	
}
