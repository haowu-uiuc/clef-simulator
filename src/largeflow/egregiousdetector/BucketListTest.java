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

public class BucketListTest {

	private BucketList bucketList;

	@Before
	public void setUp() throws Exception {
		bucketList = new BucketList(10, 1);
		for (int i = 0; i < 10; i++) {
			bucketList.addValueTo(i, i * 10);
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void generalTest() throws Exception {
		bucketList.addValueTo(1, 10);
		assertTrue(bucketList.get(1).getValue() == 10 + 10);

		// mock
		ReservationDatabase resDb = Mockito.mock(ReservationDatabase.class);
		when(resDb.getReservation(any(FlowId.class))).thenReturn(1000);

		Packet packet = new Packet(new FlowId("1"), 1000, 1.0);
		bucketList.processPacket(packet, resDb);

		int bucketIndex = -1;
		for (int i = 0; i < bucketList.size(); i++) {
			if (bucketList.getFlowsInBucket(i).contains(new FlowId("1"))) {
				bucketIndex = i;
				break;
			}
		}
		
		Bucket bucket = bucketList.get(bucketIndex);
		assertTrue(bucket.getReservation() == 1000);
		assertTrue(bucket.getValue() >= 1000);
		
		bucketList.splitTopKBuckets(2);
		assertTrue(!bucket.isChildBucketListNull());
		if(bucketIndex != 9) {
			assertTrue(!bucketList.get(9).isChildBucketListNull());
		} else {
			assertTrue(!bucketList.get(8).isChildBucketListNull());
		}
	}

	@Test
	public void testGetTopKBuckets() throws Exception {
		List<Bucket> topBuckets = bucketList.getTopKBuckets(4);
		assertTrue(topBuckets.size() == 4);
		assertTrue(topBuckets.get(0).getValue() == 90);
		assertTrue(topBuckets.get(1).getValue() == 80);
		assertTrue(topBuckets.get(2).getValue() == 70);
		assertTrue(topBuckets.get(3).getValue() == 60);
	}

	@Test
	public void testGetTopOneBucket() throws Exception {
		List<Bucket> topBuckets = bucketList.getTopKBuckets(1);
		assertTrue(topBuckets.size() == 1);
		assertTrue(topBuckets.get(0).getValue() == 90);
	}

}
