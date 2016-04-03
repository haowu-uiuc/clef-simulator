package largeflow.utils;

import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import largeflow.datatype.FlowId;

public class GenericUtils {

    /**
     * delete the file or
     * delete this directory dir and all its children files and directories.
     * @param dir
     * @return
     */
    static public boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            for (File subDir : dir.listFiles()) {
                deleteDir(subDir);
            }
        }
        
        return dir.delete();
    }
    
    /**
     * randomly pick a subset of a flowId set
     * @param subsetSize
     * @param totalSet
     * @return
     */
    static public Set<FlowId> getRandomSubFlowSet(int subsetSize, Set<FlowId> totalSet) {
        HashSet<FlowId> subFlowSet = new HashSet<>();
        
        HashSet<Integer> indexSet = new HashSet<>(subsetSize);
        Random rand = new Random((int)(Math.random() * 1000000.));
        for (int i = 0; i < subsetSize; i++) {
            int newIndex = rand.nextInt(totalSet.size());
            while (indexSet.contains(newIndex)) {
                newIndex = rand.nextInt(totalSet.size());
            }
            indexSet.add(newIndex);
        }
        
        int index = 0;
        for (FlowId flowId : totalSet) {
            
            if (indexSet.contains(index)) {
                subFlowSet.add(flowId);
            }
            
            index++;
        }
        
        return subFlowSet;
    }
    
}
