package largeflow.emulator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import largeflow.datatype.Packet;

public class PacketReaderFactory {

	static public PacketReader getPacketReader(File inputTrafficFile)
			throws IOException {
		return new FilePacketReader(inputTrafficFile);
	}

	static public PacketReader getPacketReader(String inputTrafficFilePath)
			throws IOException {
		return new FilePacketReader(inputTrafficFilePath);
	}

	static public PacketReader getPacketReader(List<Packet> inputPackets) {
		return new MemPacketReader(inputPackets);		
	}
	
}
