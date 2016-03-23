package largeflow.egregiousdetector;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class BucketListTreeTest {

	private BucketListTree tree;
	
	@Before
	public void setUp() throws Exception {
		// mock
		ReservationDatabase resDb = Mockito.mock(ReservationDatabase.class);
		when(resDb.getReservation(any(FlowId.class))).thenReturn(600);
		
//		resDb = new UniReservationDatabase(600);
		tree = new BucketListTree(3, 5, 2, resDb);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {

		assertTrue(tree.getMaxDepth() == 3);
		
		List<FlowId> largeflows = tree.checkBottomBuckets(0.0);
		assertTrue(largeflows.isEmpty());
		assertTrue(tree.getCurrentLevel() == 1);
		
		tree.processPacket(new Packet(new FlowId("1"), 1000, 0.1));
		tree.processPacket(new Packet(new FlowId("1"), 1000, 0.2));
		tree.processPacket(new Packet(new FlowId("2"), 1000, 0.3));
		tree.processPacket(new Packet(new FlowId("2"), 1000, 0.3));
		tree.processPacket(new Packet(new FlowId("3"), 1000, 0.4));
		
		assertTrue(tree.splitBuckets());
		assertTrue(tree.getCurrentLevel() == 2);

		largeflows = tree.checkBottomBuckets(1.0);
		assertTrue(largeflows.isEmpty());
		largeflows = tree.checkBottomBuckets(0.1);
		assertTrue(largeflows.isEmpty());
		
		tree.processPacket(new Packet(new FlowId("1"), 1000, 1.1));
		tree.processPacket(new Packet(new FlowId("1"), 1000, 1.2));
		tree.processPacket(new Packet(new FlowId("2"), 1000, 1.3));
		tree.processPacket(new Packet(new FlowId("2"), 1000, 1.3));
		tree.processPacket(new Packet(new FlowId("3"), 1000, 1.4));
		
		assertTrue(tree.splitBuckets());
		assertTrue(tree.getCurrentLevel() == 3);
		largeflows = tree.checkBottomBuckets(1.0);
		assertTrue(largeflows.isEmpty());
		largeflows = tree.checkBottomBuckets(0.1);
		assertTrue(largeflows.isEmpty());
		
		tree.processPacket(new Packet(new FlowId("1"), 1000, 2.1));
		tree.processPacket(new Packet(new FlowId("1"), 1000, 2.2));
		tree.processPacket(new Packet(new FlowId("2"), 1000, 2.3));
		tree.processPacket(new Packet(new FlowId("2"), 1000, 2.3));
		tree.processPacket(new Packet(new FlowId("3"), 1000, 2.4));
		
		assertTrue(!tree.splitBuckets());
		largeflows = tree.checkBottomBuckets(1.0);
		System.out.println(largeflows);
		assertTrue(!largeflows.isEmpty());
		largeflows = tree.checkBottomBuckets(10.0);
		largeflows.isEmpty();
	}

}
