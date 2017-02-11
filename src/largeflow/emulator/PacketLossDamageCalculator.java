package largeflow.emulator;

import java.util.List;
import java.util.Map;

import largeflow.datatype.Damage;
import largeflow.datatype.FlowId;
import largeflow.flowgenerator.RealAttackFlowGenerator;
import largeflow.utils.StepCurve;
import largeflow.utils.Tuple;

public class PacketLossDamageCalculator {

    /**
     * damageRatio shows that the priority damage is this time of the 
     * best-effort damage. 
     * i.e. damage = BE_damage + damageRatio * Priority_damage
     */
    public Double damageRatio = 10.;
    private Detector baseDetector;
    private AdvancedRouter router;
    private RealAttackFlowGenerator flowGenerator;
    
    public PacketLossDamageCalculator(Detector baseDetector,
            AdvancedRouter router, RealAttackFlowGenerator flowGenerator) {
        this.baseDetector = baseDetector;
        this.router = router;
        this.flowGenerator = flowGenerator;
    }
    
    
    // TODO: modify the damage measurer to use blockedRealTrafficVolume
    /**
     * calculate the damage
     * @param attackReservedTrafficVolume
     *      The sum of {{reservation_bandwidth} * {lifetime of each attack flow}}
     *      An attack flow can be caught before the end of experiment, thus,
     *      the rest of bandwidth are for best-effort traffic
     * @param preQdRealTrafficVolume
     *      The amount of legitimate traffic from inbound link
     * @param postQdAttackTrafficVolume
     *      The amount of attack traffic in the outbound link
     * @param postQdRealTrafficVolume
     *      The amount of legitimate traffic in the outbound link
     * @param blockedRealTrafficVolume
     *      The amount of legitimate traffic blocked by detectors
     * @param outboundCapacity
     * 
     * @return
     * @throws Exception
     */
    public Damage getMeasuredDamage(long attackReservedTrafficVolume,
            long preQdRealTrafficVolume,
            long postQdAttackTrafficVolume, 
            long postQdRealTrafficVolume, 
            long blockedRealTrafficVolume,
            long outboundCapacity) throws Exception {
        
        Damage damage = new Damage();
        damage.FN = getFN();
        damage.FP = getFP();
        damage.TP = getTP();

        Long BE_theo = (long)(outboundCapacity * flowGenerator.getTraceLength()) - attackReservedTrafficVolume 
                - preQdRealTrafficVolume + blockedRealTrafficVolume;
        Long BE_actual = (long)(outboundCapacity * flowGenerator.getTraceLength())
                - postQdAttackTrafficVolume - postQdRealTrafficVolume;
        
        Long damage_BE = BE_theo - BE_actual;
        Long damage_Priority = preQdRealTrafficVolume - postQdRealTrafficVolume;
        if (damage_BE < 0) {
            System.out.println("damage_BE is less than zero! damage_BE = " + damage_BE);
            damage_Priority += damage_BE; // exclude the packet drop caused by legitimate flow itself.
            damage_BE = (long) 0;
            if (damage_Priority < 0) {
                // the negative damage should be just some small error
                System.out.println("damage_Priority is less than zero! damage_Priority = " + damage_Priority);
                damage_Priority = (long) 0;
            }
        }
        System.out.println("damage_priority = " + damage_Priority);
        System.out.println("FN = " + damage.FN);
        
        damage.totalDamage = damage_Priority * damageRatio + damage_BE;
        damage.perFlowDamage = damage.totalDamage / flowGenerator.getNumOfAttFlows();
        damage.BE_damage = (double) damage_BE;
        damage.FP_damage = (double) blockedRealTrafficVolume; // TODO: may change later
        damage.QD_drop_damage = (double) (damage_Priority - blockedRealTrafficVolume);
        
        return damage;
    }
    
    
    
    
    /**
     * TODO: unfinished. Undesign is not finalized.
     * only applicable on one inbound link and one outbound link case
     * @param baseDetector
     * @param router
     * @param flowGenerator
     * @return
     * @throws Exception
     */
    public Damage getStatisticalDamage() throws Exception {
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
            Integer P_att = flowGenerator.getPerFlowReservation() * flowGenerator.getNumOfAttFlows();
            
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
        
        return damage;
    }
    
    
    public Integer getFN() {
        Map<FlowId, Double> baseBlackList = baseDetector.getBlackList();
        Map<FlowId, Double> routerBlackList = router.getBlackList();
        
        int FN = 0;
        for (Map.Entry<FlowId, Double> entry : baseBlackList.entrySet()) {
            if (!flowGenerator.isLargeFlow(entry.getKey())) {
                // we only consider the large flow or burst flows here
                // not considering real flow which violates the spec
                continue;
            }
            
            if (routerBlackList.get(entry.getKey()) == null) {
                FN ++;
            }
        }
        
        return FN;
    }
    
    public Integer getTP() {
        Map<FlowId, Double> baseBlackList = baseDetector.getBlackList();
        
        int TP = 0;
        for (Map.Entry<FlowId, Double> entry : baseBlackList.entrySet()) {
            if (!flowGenerator.isLargeFlow(entry.getKey())) {
                // we only consider the large flow or burst flows here
                // not considering real flow which violates the spec
//                System.out.println("TP in real traffic");
                continue;
            }
            
            TP++;
        }
        
        return TP;
    }
    
    public Integer getFP() {
        Map<FlowId, Double> baseBlackList = baseDetector.getBlackList();
        Map<FlowId, Double> routerBlackList = router.getBlackList();
        
        int FP = 0;
        for (Map.Entry<FlowId, Double> entry : routerBlackList.entrySet()) {
            if (flowGenerator.isLargeFlow(entry.getKey())) {
                // do not consider the attack flows as FP.
                continue;
            }
            
            if (!baseBlackList.containsKey(entry.getKey())
                    && !flowGenerator.isLargeFlow(entry.getKey())) {
                FP ++;
            }
        }
        
        return FP;
    }
    
    

}

