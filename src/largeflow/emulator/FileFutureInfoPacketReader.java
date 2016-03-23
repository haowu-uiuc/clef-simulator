package largeflow.emulator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import largeflow.datatype.FlowId;
import largeflow.datatype.FutureInfoPacket;

@Deprecated
public class FileFutureInfoPacketReader implements FutureInfoPacketReader{
	
	private BufferedReader br;
	private File inputFile;
	private FutureInfoPacket nextPacket;
	
	public FileFutureInfoPacketReader(File inputFile) throws IOException{
		this.inputFile = inputFile;
		br = new BufferedReader(new FileReader(inputFile));
		nextPacket = readNextPacket();
	}
	
	public FileFutureInfoPacketReader(String inputFilePath) throws IOException{
		this.inputFile = new File(inputFilePath);
		br = new BufferedReader(new FileReader(inputFile));
		nextPacket = readNextPacket();
	}
	
	private FutureInfoPacket readNextPacket() throws IOException{
		String line;
		if((line = br.readLine()) == null){
			return null;
		}
		
		String[] strs = line.split(" ");
		FutureInfoPacket packet = 
				new FutureInfoPacket(new FlowId(strs[0]), Integer.valueOf(strs[1]), Double.valueOf(strs[2]));
		
		return packet;
	}
	
	public FutureInfoPacket getNextPacket() throws IOException{
		if (nextPacket == null) {
			return null;
		}
		
		FutureInfoPacket currentPacket = nextPacket;
		nextPacket = readNextPacket();
		if (nextPacket != null) {
			currentPacket.nextPacketTime = nextPacket.time;
		}
		
		return currentPacket;
	}

	@Override
	public void close() throws IOException {
		br.close();
	}

	@Override
	public void rewind() throws IOException {
		br = new BufferedReader(new FileReader(inputFile));
		nextPacket = readNextPacket();
	}
	
}
