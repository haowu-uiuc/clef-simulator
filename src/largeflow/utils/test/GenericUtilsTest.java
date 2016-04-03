package largeflow.utils.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import largeflow.datatype.FlowId;
import largeflow.utils.GenericUtils;

public class GenericUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetRandomSubFlowSet() {

        Set<FlowId> totalSet = new HashSet<>();
        int subsetSize = 10;
        
        for (int j = 0; j < 1000; j++) {
            for (int i = 0; i < 1000; i++) {
                totalSet.add(new FlowId(i));
            }
            
            Set<FlowId> subset = GenericUtils.getRandomSubFlowSet(subsetSize, totalSet);
            
            assertTrue(subset.size() == subsetSize);
            // System.out.println(subset);
        }
    
    }

}
