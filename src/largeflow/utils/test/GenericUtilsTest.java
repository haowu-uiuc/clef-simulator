package largeflow.utils.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

            Set<FlowId> subset = GenericUtils.getRandomSubFlowSet(subsetSize,
                    totalSet);

            assertTrue(subset.size() == subsetSize);
            // System.out.println(subset);
        }

    }

    @Test
    public void testAddAllNewEntriesIntoMap() {
        Map<FlowId, Double> baseMap = new HashMap<>();
        Map<FlowId, Double> newMap = new HashMap<>();
        baseMap.put(new FlowId(1), 0.1);
        baseMap.put(new FlowId(2), 0.2);
        baseMap.put(new FlowId(3), 0.3);
        baseMap.put(new FlowId(4), 0.4);
        newMap.put(new FlowId(1), 0.7);
        newMap.put(new FlowId(2), 0.8);
        newMap.put(new FlowId(5), 0.9);
        newMap.put(new FlowId(6), 1.0);
        GenericUtils.addAllNewEntriesIntoMap(baseMap, newMap);
        assertTrue(baseMap.size() == 6);
        assertTrue(checkMapEntry(baseMap, new FlowId(1), 0.1));
        assertTrue(checkMapEntry(baseMap, new FlowId(2), 0.2));
        assertTrue(checkMapEntry(baseMap, new FlowId(3), 0.3));
        assertTrue(checkMapEntry(baseMap, new FlowId(4), 0.4));
        assertTrue(checkMapEntry(baseMap, new FlowId(5), 0.9));
        assertTrue(checkMapEntry(baseMap, new FlowId(6), 1.0));
    }

    private boolean checkMapEntry(Map<FlowId, Double> map,
            FlowId key,
            Double value) {
        if (!map.containsKey(key)) {
            return false;
        }

        if (!map.get(key).equals(value)) {
            return false;
        }

        return true;
    }

}
