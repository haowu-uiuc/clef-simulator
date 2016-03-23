package largeflow.emulator.test;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.AdvancedRouter;
import largeflow.emulator.Detector;
import largeflow.emulator.FilePacketReader;
import largeflow.emulator.LeakyBucketDetector;
import largeflow.emulator.Logger;
import largeflow.emulator.PacketReader;
import largeflow.emulator.UniformFlowGenerator;

public class AdvancedRouterTest {

    private Logger logger;
    private UniformFlowGenerator flowGenerator;
    private Integer inboundLinkCapacity = 50000000;
    private Integer outboundLinkCapacity = 25000000;
    private Integer timeInterval = 5; // sec
    private Integer packetSize = 500;
    private Integer numOfLargeFlows = 10;
    private Integer largeFlowRate = 500000;
    
    @Before
    public void setUp() throws Exception {
        logger = new Logger("test_AdvancedRouter");
        flowGenerator = new UniformFlowGenerator(outboundLinkCapacity,
                timeInterval,
                packetSize,
                numOfLargeFlows,
                largeFlowRate);
        flowGenerator.setOutputFile(new File("./data/testflow_advanced_router_test.txt"));
        flowGenerator.generateFlows();
    }

    @After
    public void tearDown() throws Exception {
        logger.close();
        logger.deleteLogs();
    }

    @Test
    public void testSingleLink() throws Exception {

        AdvancedRouter router = new AdvancedRouter("test_advanced_router", inboundLinkCapacity,
                outboundLinkCapacity);
        assertTrue(router.getNumOfInboundLinks() == 1);
        assertTrue(router.getNumOfOutboundLinks() == 1);
        assertTrue(router.getTotalInboundCapacity() == inboundLinkCapacity);
        assertTrue(router.getOutboundCapacity(0) == outboundLinkCapacity);
        assertTrue(router.getPreQdDtector() == null);
        assertTrue(router.getPostQdDetector(0) == null);
        assertTrue(router.getAllPostQdDetectors().get(0) == null);
        assertTrue(router.getAllPostQdDetectors().size() == 1);
        try {
            router.getPostQdDetector(1);
            fail("Router should throw IndexOutOfBoundException");
        } catch (Exception e) {
            assertTrue(e.getClass() == IndexOutOfBoundsException.class);
        }
        try {
            Detector tmpDetector = null;
            router.setPostQdDetector(1, tmpDetector);
            fail("Router should throw IndexOutOfBoundException");
        } catch (Exception e) {
            assertTrue(e.getClass() == IndexOutOfBoundsException.class);
        }
        
        Detector baseDetector = new LeakyBucketDetector("base_detector_test",
                5000,
                largeFlowRate / 2,
                router.getTotalInboundCapacity());
        
        // only post QD detector:
        
        Detector postQdDetector = new LeakyBucketDetector("post_detector_test",
                5000,
                largeFlowRate / 2,
                router.getOutboundCapacity(0));
        router.setPostQdDetector(postQdDetector);
        
        // run the router
        PacketReader pr = new FilePacketReader(flowGenerator.getOutputFile());
        Packet packet;
        while ((packet = pr.getNextPacket()) != null) {
            if (!baseDetector.getBlackList().containsKey(packet.flowId)) {
                baseDetector.processPacket(packet);
            }
            router.processPacket(packet);
        }
        router.processEnd();
        
        //System.out.println(postQdDetector.getBlackList());
        
        for (FlowId flowId : baseDetector.getBlackList().keySet()) {
            assertTrue(router.flowIsInBlackList(flowId));
            assertTrue(router.getBlackList().containsKey(flowId));
        }
        
        
        
        // post QD detector and pre QD detector:
        Detector preQdDetector = new LeakyBucketDetector("pre_detector_test",
                5000,
                largeFlowRate / 2,
                router.getTotalInboundCapacity());
        router.setPreQdDetector(preQdDetector);
        
        logger.logRouterConfig(router);
        
        // run the router again
        router.reset();
        baseDetector.reset();
        pr.rewind();
        
        while ((packet = pr.getNextPacket()) != null) {
            if (!baseDetector.getBlackList().containsKey(packet.flowId)) {
                baseDetector.processPacket(packet);
            }
            router.processPacket(packet);
        }
        router.processEnd();
        
        for (FlowId flowId : baseDetector.getBlackList().keySet()) {
            assertTrue(router.flowIsInBlackList(flowId));
            assertTrue(router.getBlackList().containsKey(flowId));
        }
        
        //System.out.println(router.getBlacklist());
        //System.out.println(preQdDetector.getBlackList());
        //System.out.println(postQdDetector.getBlackList());
        
        pr.close();
    }

}
