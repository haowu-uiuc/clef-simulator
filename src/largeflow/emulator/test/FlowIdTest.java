package largeflow.emulator.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import largeflow.datatype.FlowId;

public class FlowIdTest {

	@Test
	public void test() {
		
		FlowId fid1 = new FlowId(1234);
		assertTrue(fid1.getStringValue().equals("1234"));
		assertTrue(fid1.isVirtualFlowId() == false);
		
		FlowId fid2 = new FlowId(12345, true);
		assertTrue(fid2.getStringValue().equals("12345"));
		assertTrue(fid2.isVirtualFlowId() == true);
		
		FlowId fid3 = new FlowId("12345");
		assertTrue(fid3.getStringValue().equals("12345"));
		assertTrue(fid3.isVirtualFlowId() == false);
		
		assertTrue(fid1.compareTo(fid2) == -1);
		assertTrue(fid2.compareTo(fid3) == -1);
		assertTrue(fid1.compareTo(fid3) == -1);
		
		assertTrue(fid2.compareTo(fid1) == 1);
		assertTrue(fid3.compareTo(fid2) == 1);
		assertTrue(fid3.compareTo(fid1) == 1);
		
		
		FlowId fid4 = new FlowId("1234");
		assertTrue(fid4.getStringValue().equals("1234"));
		assertTrue(fid4.isVirtualFlowId() == false);
	
		assertTrue(fid1.equals(fid4));
		assertTrue(fid1.compareTo(fid4) == 0);
		
		fid1.set("12345");
		assertTrue(!fid1.equals(fid4));
		assertTrue(fid1.compareTo(fid4) == 1);
		
		assertTrue(fid1.toString().equals("12345"));
		assertTrue(fid2.toString().equals("v12345"));
		
		
		// test hashCode()
		assertTrue(fid1.hashCode() == fid1.getIntegerValue().hashCode() * 2);
		assertTrue(fid2.hashCode() == fid2.getIntegerValue().hashCode() * 2 + 1);		
		assertTrue(fid3.hashCode() == fid3.getIntegerValue().hashCode() * 2);	
		
		fid1.set("1234");
		
		Set<FlowId> set = new HashSet<>();
		set.add(fid1);
		set.add(fid4);
		assertTrue(set.size() == 1);
	}

}
