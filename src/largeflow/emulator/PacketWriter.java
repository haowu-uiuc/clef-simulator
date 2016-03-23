package largeflow.emulator;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import largeflow.datatype.Packet;

public class PacketWriter implements Closeable{

	private BufferedWriter bw;
	
	public PacketWriter(File outputFile) throws IOException{
		bw = new BufferedWriter(new FileWriter(outputFile));
	}
	
	public PacketWriter(String outputFilePath) throws IOException {
		bw = new BufferedWriter(new FileWriter(new File(outputFilePath)));
	}
	
	public void writePacket(Packet packet) throws IOException{
		bw.write(packet.flowId + " " + packet.size + " " + packet.time + "\n");
	}

	@Override
	public void close() throws IOException {
		bw.close();
	}
	
}
