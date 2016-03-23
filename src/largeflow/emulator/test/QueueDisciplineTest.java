package largeflow.emulator.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.QueueDiscipline;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QueueDisciplineTest {

	private List<Packet> inputPackets;
	private Integer sampledLinkCapacity;

	@Before
	public void setUp() throws Exception {
		sampledLinkCapacity = 25000;
		inputPackets = new ArrayList<>();
		inputPackets.add(new Packet(new FlowId("80"), 100, 0.006468000000000001));
		inputPackets.add(new Packet(new FlowId("42"), 100, 0.006816));
		inputPackets.add(new Packet(new FlowId("80"), 100, 0.007132));
		inputPackets.add(new Packet(new FlowId("42"), 100, 0.0074800000000000005));
		inputPackets.add(new Packet(new FlowId("80"), 100, 0.0078000000000000005));
		inputPackets.add(new Packet(new FlowId("42"), 100, 0.008147999999999999));
	}

	@After
	public void tearDown() throws Exception {
		QueueDiscipline gatewayRouter = new QueueDiscipline(sampledLinkCapacity);
		for (int i = 0; i < inputPackets.size(); i++) {
			gatewayRouter.processPacket(inputPackets.get(i));
		}

		gatewayRouter.processEnd();
		
		int n = 0;
		Packet nextPacket;
		double lastPacketTime = 0.0;
		while ((nextPacket = gatewayRouter.getNextPacket()) != null) {
			System.out.println((++n) + "\t" + nextPacket.flowId + "\t" + nextPacket.size + "\t" + nextPacket.time);
			assertTrue(nextPacket.size == 100);
			assertTrue(nextPacket.time > lastPacketTime);
			lastPacketTime = nextPacket.time;
		}
	}

	@Test
	public void test() {

	}

}
