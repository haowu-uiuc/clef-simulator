package largeflow.emulator;

import java.io.IOException;

import largeflow.datatype.FutureInfoPacket;

@Deprecated
public interface FutureInfoPacketReader extends PacketReader {
	
	@Override
	public FutureInfoPacket getNextPacket() throws IOException;
	
}
