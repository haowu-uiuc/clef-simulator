package largeflow.emulator;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;

import largeflow.datatype.Packet;


public interface PacketReader extends Closeable{
	
	public Packet getNextPacket() throws IOException;

	public void rewind() throws FileNotFoundException, IOException;
	
	@Override
	public void close() throws IOException;
	
}
