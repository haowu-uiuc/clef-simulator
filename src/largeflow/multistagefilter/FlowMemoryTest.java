package largeflow.multistagefilter;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

public class FlowMemoryTest {

    List<Packet> packets;
    
    @Before
    public void setUp() throws Exception {
        packets = new ArrayList<>();
        packets.add(new Packet(new FlowId(1), 1000 , 0.));
        packets.add(new Packet(new FlowId(2), 1000 , 0.01));
        packets.add(new Packet(new FlowId(3), 1000 , 0.02));
        packets.add(new Packet(new FlowId(1), 1500 , 0.03));
        packets.add(new Packet(new FlowId(1), 1500 , 0.04));
        packets.add(new Packet(new FlowId(1), 1500 , 0.05));
        packets.add(new Packet(new FlowId(4), 1500 , 0.06));
        packets.add(new Packet(new FlowId(5), 1500 , 0.07));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testFlowMemory() {
        FlowMemory fm = new FlowMemory(3, 4000, 10000, 1000000);
        for (int i = 0; i < 3; i++) {
            Packet packet = packets.get(i);
            boolean isLargeFlow = fm.processPacket(packet);
            assertTrue(fm.flowIsInFlowMemory(packet.flowId));
            assertTrue(!isLargeFlow);
        }
        
        for (int i = 3; i < 5; i++) {
            Packet packet = packets.get(i);
            boolean isLargeFlow = fm.processPacket(packet);
            assertTrue(fm.flowIsInFlowMemory(new FlowId(1)));
            assertTrue(fm.flowIsInFlowMemory(new FlowId(2)));
            assertTrue(fm.flowIsInFlowMemory(new FlowId(3)));
            assertTrue(!isLargeFlow);
        }
        
        // the flow 1 should be caught at this time
        // the flow should be removed from the flow memory
        boolean isLargeFlow = fm.processPacket(packets.get(5));
        assertTrue(isLargeFlow);
        assertTrue(!fm.flowIsInFlowMemory(new FlowId(1)));
        assertTrue(fm.flowIsInFlowMemory(new FlowId(2)));
        assertTrue(fm.flowIsInFlowMemory(new FlowId(3)));
        
        isLargeFlow = fm.processPacket(packets.get(6));
        assertTrue(!isLargeFlow);
        assertTrue(fm.flowIsInFlowMemory(new FlowId(4)));
        assertTrue(fm.flowIsInFlowMemory(new FlowId(2)));
        assertTrue(fm.flowIsInFlowMemory(new FlowId(3)));
        
        isLargeFlow = fm.processPacket(packets.get(7));
        // we randomly pick one from 2,3,4 and remove the flow.
        // So only two of them are left at this time
        int numFlowLeft = 0;
        for (int i = 2; i < 5; i++) {
            if (fm.flowIsInFlowMemory(new FlowId(i))) {
                numFlowLeft++;
                System.out.println(i);
            }
        }
        assertTrue(numFlowLeft == 2);
        assertTrue(fm.flowIsInFlowMemory(new FlowId(5)));
    }
    
    @Test
    public void testFlowMemoryWithNoLimit() {
        FlowMemory fm = new FlowMemory(3, 4000, 10000, 1000000);
        fm.setNoSizeLimit();
        for (int i = 0; i < 8; i++) {
            Packet packet = packets.get(i);
            fm.processPacket(packet);
        }
        assertTrue(fm.numOfFlows() == 4);
    }

}
