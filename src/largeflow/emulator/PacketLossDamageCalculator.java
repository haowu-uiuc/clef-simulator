package largeflow.emulator;

import java.util.List;
import java.util.Map;

import largeflow.datatype.Damage;
import largeflow.datatype.FlowId;
import largeflow.utils.StepCurve;
import largeflow.utils.Tuple;

public class PacketLossDamageCalculator {

    /**
     * damageRatio shows that the priority damage is this time of the 
     * best-effort damage. 
     * i.e. damage = BE_damage + damageRatio * Priority_damage
     */
    static public Double damageRatio = 10.;
    
    /**
     * only applicable on one inbound link and one outbound link case
     * @param baseDetector
     * @param router
     * @param flowGenerator
     * @return
     * @throws Exception
     */
    public Damage getDamage(Detector baseDetector,
            AdvancedRouter router,
            RealAttackFlowGenerator flowGenerator) throws Exception {
        if (router.getNumOfInboundLinks() > 1) {
            throw new Exception("This damage calculator doesn't work with multiple outbound links!");
        }
        
        Map<FlowId, Double> baseBlackList = baseDetector.getBlackList();
        Map<FlowId, Double> routerBlackList = router.getBlackList();

        // find FN and calculate the damage caused by large flows
        StepCurve attTrafficRateCurve = new StepCurve(flowGenerator.getTraceLength());
        
        int TP = 0;
        int FN = 0;
        for (Map.Entry<FlowId, Double> entry : baseBlackList.entrySet()) {
            if (!flowGenerator.isLargeFlow(entry.getKey())) {
                // we only consider the large flow here
                // not considering real flow which violates the spec
                continue;
            }
            
            TP++;
            
            // find the curve of the attack flow rate
            Double baseTime = entry.getValue();
            Double routerTime;
            if ((routerTime = routerBlackList.get(entry.getKey())) == null) {
                FN ++;
            } else {
                if (routerTime > baseTime) {
                    attTrafficRateCurve.addCurve(
                            new Tuple<Double, Integer>(baseTime, flowGenerator.getAttackRate()), 
                            routerTime);
                }
            }
        }
        
        // calculate the damage caused by large flows based on the curve
        List<Tuple<Double, Integer>> jumpPoints = attTrafficRateCurve.getJumpPoints();
        Tuple<Double, Integer> prePoint = null;
        Double BE_damage = 0.; // best effort damage
        Double Priority_damage = 0.; // priority damage
        for (Tuple<Double, Integer> curPoint : jumpPoints) {
            if (prePoint == null) {
                prePoint = curPoint;
                continue;
            }
            
            Double T = curPoint.first - prePoint.first;
            Integer R_att = curPoint.second;
            Integer R_normal = flowGenerator.getAveRealTrafficRate();
            Integer C_out = router.getOutboundCapacity(0);
            Integer P_att = flowGenerator.getPriorityBandwidthOfOneFlow() * flowGenerator.getNumOfAttFlows();
            
            if (R_normal + R_att <= C_out) {
                if (R_att > P_att) {
                    BE_damage += (R_att - P_att) * T;
                }
            } else {
                Double PLR = 1. - (double)C_out / (double)(R_normal+R_att);
                Priority_damage += R_normal * PLR * T;
                BE_damage += (C_out - (P_att + R_normal)) * T;
                if (C_out - (P_att + R_normal) <  0) {
                    throw new Exception("C_out - (P_att + R_normal) should be above zero! Something wrong?");
                }
            }
            
            prePoint = curPoint;
        }
        
        // find FP
        // TODO: and FP damage over legitimate flows
        int FP = 0;
        for (Map.Entry<FlowId, Double> entry : routerBlackList.entrySet()) {
            if (!baseBlackList.containsKey(entry.getKey())
                    && !flowGenerator.isLargeFlow(entry.getKey())) {
                FP ++;
            }
        }
        
        //TODO: calculate the damage based on attTrafficVolumeCurve
        
        
        
        Damage damage = new Damage();
        damage.TP = TP; // only consider large flows but not real flows
        damage.FN = FN; // only consider large flows but not real flows
        damage.FP = FP; // only consider FP in real trace, should we ?
        
        return null;
    }

}

