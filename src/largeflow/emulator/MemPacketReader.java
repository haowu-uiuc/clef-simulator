package largeflow.emulator;

import java.io.IOException;
import java.util.List;

import largeflow.datatype.Packet;

public class MemPacketReader implements PacketReader {

	private List<Packet> inputPackets;
	private int nextPacketIndex;

	public MemPacketReader(List<Packet> inputPackets) {
		this.inputPackets = inputPackets;
		nextPacketIndex = 0;
	}

	public Packet getNextPacket() throws IOException{
		if (nextPacketIndex >= inputPackets.size()){
			return null;
		}
		
		Packet nextPacket = inputPackets.get(nextPacketIndex);
		nextPacketIndex++;
		Packet packetCopy = new Packet(nextPacket.flowId, nextPacket.size, nextPacket.time);
		
		return packetCopy;
	}

	@Override
	public void close() {
	}

	@Override
	public void rewind() {
		nextPacketIndex = 0;
	}

}
