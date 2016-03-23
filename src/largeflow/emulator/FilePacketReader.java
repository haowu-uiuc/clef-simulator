package largeflow.emulator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;


public class FilePacketReader implements PacketReader{
	
	private BufferedReader br;
	private File inputFile;
	
	public FilePacketReader(File inputFile) throws IOException{
		this.inputFile = inputFile;
		br = new BufferedReader(new FileReader(inputFile));
	}
	
	public FilePacketReader(String inputFilePath) throws IOException{
		this.inputFile = new File(inputFilePath);
		br = new BufferedReader(new FileReader(inputFile));
	}
	
	private Packet readNextPacket() throws IOException{
		String line;
		if((line = br.readLine()) == null){
			return null;
		}
		
		String[] strs = line.split(" ");
		Packet packet = new Packet(new FlowId(strs[0]), Integer.valueOf(strs[1]), Double.valueOf(strs[2]));
		
		return packet;
	}
	
	public Packet getNextPacket() throws IOException{
		return readNextPacket();
	}

	@Override
	public void close() throws IOException {
		br.close();
	}

	@Override
	public void rewind() throws IOException {
		br = new BufferedReader(new FileReader(inputFile));
	}
	
}
