package largeflow.datatype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Damage {

    static private List<String> titleList = Arrays.asList("FP",
            "FN",
            "TP",
            "perFlowDamage",
            "totalDamage");

    static public List<String> getTitleList() {
        return new ArrayList<>(titleList);
    }
    
    public int FN;
    public int FP;
    public int TP;
    public double perFlowDamage;
    public double totalDamage;

}
