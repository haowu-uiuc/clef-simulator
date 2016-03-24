package largeflow.emulator.test;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import largeflow.datatype.Damage;
import largeflow.datatype.FlowId;
import largeflow.emulator.Detector;
import largeflow.emulator.Logger;
import largeflow.emulator.Router;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * please check the output files in ./data/exp_logger/test_exp/
 * to tell whether the result is correct
 * @author HaoWu
 *
 */
public class LoggerTest {

	Logger logger;
	
	@Before
	public void init() throws Exception {
		logger = new Logger("test_exp");
	}

	@After
	public void tearDown() throws Exception {
		logger.close();
//		logger.deleteLogs();
	}

	@Test
	public void test() throws Exception {
		logger.log("detector_1", 1000, 1000, 100, 100, 100, 999.999);
		logger.log("detector_2", 1000, 1000, 100, 100, 100, 999.999);
		logger.log("detector_3", 1000, 1000, 100, 100, 100, 999.999);
		
		Damage damage = new Damage();
		damage.FN = 10;
		damage.FP = 15;
		damage.TP = 20;
		damage.perFlowDamage = 111.111;
		damage.totalDamage = 999.999;
		
		//mock
        Detector baseDetector = Mockito.mock(Detector.class);
        Router router1 = Mockito.mock(Router.class);
        Router router2 = Mockito.mock(Router.class);
        Router router3 = Mockito.mock(Router.class);
        when(baseDetector.name()).thenReturn("base_detector");
        when(router1.name()).thenReturn("router_1");
        when(router2.name()).thenReturn("router_2");
        when(router3.name()).thenReturn("router_3");
        
        Map<FlowId, Double> baseDetector_BlackList = new HashMap<>();
        baseDetector_BlackList.put(new FlowId(1), 0.11);
        baseDetector_BlackList.put(new FlowId(2), 0.22);
        baseDetector_BlackList.put(new FlowId(3), 0.33);
        
        Map<FlowId, Double> router1_BlackList = new HashMap<>();
        router1_BlackList.put(new FlowId(1), 0.15);
        router1_BlackList.put(new FlowId(2), 0.25);
        router1_BlackList.put(new FlowId(3), 0.45);
        
        Map<FlowId, Double> router2_BlackList = new HashMap<>();
        router2_BlackList.put(new FlowId(1), 0.15);
        router2_BlackList.put(new FlowId(2), 0.25);
        router2_BlackList.put(new FlowId(3), 0.45);
        
        Map<FlowId, Double> router3_BlackList = new HashMap<>();
        router3_BlackList.put(new FlowId(1), 0.35);
        router3_BlackList.put(new FlowId(2), 0.65);
        
        Map<FlowId, Integer> router1_droppedTraffic = new HashMap<>();
        router1_droppedTraffic.put(new FlowId(1), 1111);
        router1_droppedTraffic.put(new FlowId(2), 1222);
        router1_droppedTraffic.put(new FlowId(3), 1333);
        
        Map<FlowId, Integer> router2_droppedTraffic = new HashMap<>();
        router2_droppedTraffic.put(new FlowId(1), 2111);
        router2_droppedTraffic.put(new FlowId(2), 2222);
        router2_droppedTraffic.put(new FlowId(3), 2333);
        
        Map<FlowId, Integer> router3_droppedTraffic = new HashMap<>();
        router3_droppedTraffic.put(new FlowId(1), 3111);
        router3_droppedTraffic.put(new FlowId(2), 3222);
        
        when(baseDetector.getBlackList()).thenReturn(baseDetector_BlackList);
        when(router1.getBlackList()).thenReturn(router1_BlackList);
        when(router2.getBlackList()).thenReturn(router2_BlackList);
        when(router3.getBlackList()).thenReturn(router3_BlackList);
        
        when(router1.getDroppdTrafficMap()).thenReturn(router1_droppedTraffic);
        when(router2.getDroppdTrafficMap()).thenReturn(router2_droppedTraffic);
        when(router3.getDroppdTrafficMap()).thenReturn(router3_droppedTraffic);
        
		
		//test logRouterDamage
		logger.logRouterDamage(router1, 1000, 1000, damage);
	    logger.logRouterDamage(router2, 2000, 4000, damage);
	    logger.logRouterDamage(router3, 3000, 5000, damage);
	    
	    //test logRouterBlackList
        for (int round = 0; round < 3; round ++) {
            for (int rate = 1000; rate <= 3000; rate += 1000) {
                logger.logBaseDetectorBlackList(baseDetector, rate, round);
                for (int counter = 100; counter <= 3000; counter += 1000) {
                    logger.logRouterBlackList(router1, rate, counter, round);
                    logger.logRouterBlackList(router2, rate, counter, round);
                    logger.logRouterBlackList(router3, rate, counter, round);
                }
            }
        }
        
        

	    
	}

}
