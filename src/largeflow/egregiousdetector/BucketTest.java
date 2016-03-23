package largeflow.egregiousdetector;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BucketTest {

	private Bucket bucket;
	
	@Before
	public void setUp() throws Exception {
		bucket = new Bucket(1);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		assertTrue(bucket.getDepth() == 1);

		bucket.addReservation(10);
		bucket.addReservation(20);
		assertTrue(bucket.getReservation() == 30);
		
		bucket.addValue(10);
		bucket.addValue(20);
		assertTrue(bucket.getValue() == 30);
		
		assertTrue(bucket.getChildBucketList() == null);
		assertTrue(bucket.isChildBucketListNull());
		
		bucket.createChildBucketList(10);
		assertTrue(bucket.getChildBucketList() != null);
		assertTrue(!bucket.isChildBucketListNull());
		
		BucketList childBucketList = bucket.getChildBucketList();
		assertTrue(childBucketList.size() == 10);
		
	}

}
