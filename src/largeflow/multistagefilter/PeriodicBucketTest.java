package largeflow.multistagefilter;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import largeflow.datatype.FlowId;
import largeflow.datatype.FutureInfoPacket;

@Deprecated
public class PeriodicBucketTest {

	private Double T = 0.1;
	private Integer threshold = 1500;
	private Integer linkCapacity = 100000;
	
	private List<FutureInfoPacket> packetList;
	
	@Before
	public void setUp() throws Exception {
		
		packetList = new ArrayList<>();
		packetList.add(new FutureInfoPacket(new FlowId("1"), 1000, 0.0, 0.05));
		packetList.add(new FutureInfoPacket(new FlowId("1"), 501, 0.05, 0.11));
		packetList.add(new FutureInfoPacket(new FlowId("1"), 1000, 0.11, 0.15));
		packetList.add(new FutureInfoPacket(new FlowId("1"), 500, 0.15, 0.21));
		packetList.add(new FutureInfoPacket(new FlowId("1"), 1000, 0.21, 0.2951));
		packetList.add(new FutureInfoPacket(new FlowId("1"), 1000, 0.2951, 0.35));	// packet that crosses periods
		packetList.add(new FutureInfoPacket(new FlowId("1"), 1000, 0.35, null));
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {

		PeriodicBucket bucket = new PeriodicBucket(T, threshold, linkCapacity);
		bucket.considerPacketCrossPeriods();
		
		int i = 0;
		// period 0
		assertTrue(bucket.processAndCheckPacket(packetList.get(i++)));
		assertFalse(bucket.processAndCheckPacket(packetList.get(i++)));
		
		// period 1
		assertTrue(bucket.processAndCheckPacket(packetList.get(i++)));
		assertTrue(bucket.processAndCheckPacket(packetList.get(i++)));
		
		// period 2
		assertTrue(bucket.processAndCheckPacket(packetList.get(i++)));
		assertTrue(bucket.processAndCheckPacket(packetList.get(i++)));
		
		// period 3
		assertFalse(bucket.processAndCheckPacket(packetList.get(i++)));
		
		bucket.reset();
		bucket.notConsiderPacketCrossPerids();
		
		i = 0;
		// period 0
		assertTrue(bucket.processAndCheckPacket(packetList.get(i++)));
		assertFalse(bucket.processAndCheckPacket(packetList.get(i++)));
		
		// period 1
		assertTrue(bucket.processAndCheckPacket(packetList.get(i++)));
		assertTrue(bucket.processAndCheckPacket(packetList.get(i++)));
		
		// period 2
		assertTrue(bucket.processAndCheckPacket(packetList.get(i++)));
		assertFalse(bucket.processAndCheckPacket(packetList.get(i++)));
		
		// period 3
		assertTrue(bucket.processAndCheckPacket(packetList.get(i++)));		
	
	}

}
