package largeflow.emulator.test;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;

public class PacketReaderTest {
	
	private File testTrafficFile;
	private List<Packet> testPacketList;

	@Before
	public void setUp() throws Exception {
		
		testTrafficFile = new File("./data/test/test_traffic.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(testTrafficFile));
		bw.write("10001 60 0.000000\n" + 
				"10001 60 0.000351\n" +
				"10002 347 0.002117\n" +
				"10002 283 0.002453\n" +
				"10003 60 0.002666\n" +
				"10004 60 0.003219\n" +
				"10005 62 0.003364\n" +
				"10006 1514 0.003518");
		bw.close();
		
		testPacketList = new ArrayList<>();
		testPacketList.add(new Packet(new FlowId("10001"), 60, 0.0));
		testPacketList.add(new Packet(new FlowId("10001"), 60, 0.000351));
		testPacketList.add(new Packet(new FlowId("10002"), 347, 0.002117));
		testPacketList.add(new Packet(new FlowId("10002"), 283, 0.002453));
		testPacketList.add(new Packet(new FlowId("10003"), 60, 0.002666));
		testPacketList.add(new Packet(new FlowId("10004"), 60, 0.003219));
		testPacketList.add(new Packet(new FlowId("10005"), 62, 0.003364));
		testPacketList.add(new Packet(new FlowId("10006"), 61514, 0.003518));

	}

	@After
	public void tearDown() throws Exception {
		if (testTrafficFile.exists()) {
			testTrafficFile.delete();
		}
	}

	@Test
	public void testFilePacketReader() throws IOException {
		PacketReader pr = PacketReaderFactory.getPacketReader(testTrafficFile);
		testPacketReader(pr);
	}
	
	@Test
	public void testMemPacketReader() throws IOException {
		PacketReader pr = PacketReaderFactory.getPacketReader(testPacketList);
		testPacketReader(pr);
	}
	
	private void testPacketReader(PacketReader pr) throws IOException {
		Packet packet1 = pr.getNextPacket();
				
		assertTrue(packet1.flowId.equals(new FlowId("10001")) 
				&& packet1.size == 60 
				&& packet1.time == 0.);	
				
		pr.getNextPacket(); // skip one packet
		Packet packet2 = pr.getNextPacket();
		assertTrue(packet2.flowId.equals(new FlowId("10002")) 
				&& packet2.size == 347 
				&& packet2.time == 0.002117);
		
		for (int i = 0; i < 5; i++) {
			Packet tmp = pr.getNextPacket();			
			assertTrue(tmp != null);
		}
		
		Packet packet3 = pr.getNextPacket();
		assertTrue(packet3 == null);
			
	}

}
