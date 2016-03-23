package largeflow.utils.test;

import static org.junit.Assert.*;

import largeflow.datatype.FlowId;
import largeflow.utils.RandomHashFunction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RandomHashFunctionTest {

	private RandomHashFunction<FlowId> hashFunc;
	private int numOfFlows = 1000000;
	private int numOfBuckets = 100;
	private double bias = 0.05;

	@Before
	public void setUp() throws Exception {
		hashFunc = new RandomHashFunction<>(numOfBuckets);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		for (int i = 0; i < numOfFlows; i++) {
			int hashCode = hashFunc.getHashCode(new FlowId(i));
			assertTrue(hashCode < numOfBuckets);
		}

		for (int hashCode = 0; hashCode < numOfBuckets; hashCode++) {
			// System.out.println(hashFunc.getFlowIds(hashCode).size());
			int flowsInBucket = hashFunc.getKeys(hashCode).size();
			int idealFlowsInBucket = numOfFlows / numOfBuckets;
			assertTrue(Math.abs((double) flowsInBucket
					- (double) idealFlowsInBucket) < bias
					* (double) idealFlowsInBucket);
		}
	}
}
