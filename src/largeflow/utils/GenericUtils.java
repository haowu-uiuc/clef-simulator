package largeflow.utils;

import java.io.File;

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
    
}
