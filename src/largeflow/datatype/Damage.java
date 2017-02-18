package largeflow.datatype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Damage {

    static private List<String> titleList = Arrays.asList(
            "FP",
            "FN",
            "TP",
            "perFlowDamage",
            "totalDamage",
            "best_effort_damage",
            "FP_damage",
            "qd_drop_damage",
            "baseline_damage");

    static public List<String> getTitleList() {
        return new ArrayList<>(titleList);
    }
    
    public Integer FN = null;
    public Integer FP = null;
    public Integer TP = null;
    public Double perFlowDamage = null;
    public Double totalDamage = null;
    public Double BE_damage = null;
    public Double FP_damage = null;
    public Double QD_drop_damage = null;
    
    /**
     * when there is no attack flows, baseline damage is 
     * TODO: priority_damage (maybe not true when FP damage happens
     * or priority_damage - FP_damage)
     */
    public Double baseline_damage = null;

}
