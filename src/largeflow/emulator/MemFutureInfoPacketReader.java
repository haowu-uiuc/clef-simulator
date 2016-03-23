package largeflow.emulator;

import java.io.IOException;
import java.util.List;

import largeflow.datatype.FutureInfoPacket;

@Deprecated
public class MemFutureInfoPacketReader implements FutureInfoPacketReader {

	private List<FutureInfoPacket> inputPackets;
	private int nextPacketIndex;

	public MemFutureInfoPacketReader(List<FutureInfoPacket> inputPackets) {
		this.inputPackets = inputPackets;
		nextPacketIndex = 0;
	}

	public FutureInfoPacket getNextPacket() throws IOException{
		if (nextPacketIndex >= inputPackets.size()){
			return null;
		}
		
		FutureInfoPacket nextPacket = inputPackets.get(nextPacketIndex);
		nextPacketIndex++;
		FutureInfoPacket packetCopy = 
				new FutureInfoPacket(nextPacket.flowId, nextPacket.size, nextPacket.time);
		
		packetCopy.nextPacketTime = null;
		if (nextPacketIndex < inputPackets.size()) {
			packetCopy.nextPacketTime = inputPackets.get(nextPacketIndex).time;
		}
		
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
