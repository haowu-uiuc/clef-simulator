package largeflow.emulator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogParser {

    private File logDir;
    private boolean stdoutEnabled = true;

    public LogParser(File logDir) throws Exception {
        System.out.println("Parse log directory: " + logDir);
        if (!logDir.exists() || !logDir.isDirectory()) {
            throw new Exception("The log directory to parse does not exist!");
        }
        
        this.logDir = logDir;
    }
    
    public LogParser(String expName) throws Exception {
        logDir = new File("./data/exp_logger/" + expName);
        System.out.println("Parse log directory: " + logDir);
        if (!logDir.exists() || !logDir.isDirectory()) {
            throw new Exception("The log directory to parse does not exist!");
        }
    }
    
    public void disableStdout() {
        stdoutEnabled = false;
    }
    
    public void parseTrafficAndDamage() throws Exception {
        
        File dir = new File(logDir + "/traffic_and_damage");
        if (!dir.exists() || !dir.isDirectory()) {
            throw new Exception("The damage and traffic directory to parse does not exist!");
        }
        
        int count = 0;

        // traverse all router folders
        for (File routerDir : dir.listFiles()) {
            boolean titleIsWritten = false;
            
            String routerName = routerDir.getName();
            File output = new File(logDir + "/total_" + routerName + "_traffic_damage.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(output));
            
            // traverse all round folders
            List<Integer> rounds = new ArrayList<>(routerDir.listFiles().length);
            for (File roundDir : routerDir.listFiles()) {
                if (roundDir.isDirectory()) {
                    rounds.add(Integer.valueOf(roundDir.getName()));
                }
            }
            
            // sort the rounds from small to large
            Collections.sort(rounds);
            
            for (int i = 0; i < rounds.size(); i++) {
                File roundDir = new File(routerDir + "/" + rounds.get(i));
                if (!roundDir.isDirectory()) {
                    bw.close();
                    throw new Exception("The round directory is not a directory!");
                }
                
                // traverse all files in the round directory
                List<Integer> rates = new ArrayList<>(roundDir.listFiles().length);
                for (File atkRateFile : roundDir.listFiles()) {
                    if (atkRateFile.isFile()) {
                        String[] tmpStrs = atkRateFile.getName().split("\\.");
                        rates.add( Integer.valueOf( tmpStrs[0]) );
                    }
                }
                
                Collections.sort(rates);
                
                for (int j = 0; j < rates.size(); j++) {
                    File rateFile = new File(roundDir + "/" + rates.get(j) + ".txt");
                    if (!rateFile.isFile()) {
                        bw.close();
                        throw new Exception("The rate file is not a file!");
                    }
                    
                    count++;
                    if (stdoutEnabled) {
                        System.out.println(count + ". " + routerName + ", "
                                + "round=" + roundDir.getName() + ", rate="
                                + rates.get(j));
                    }
                    
                    BufferedReader br = new BufferedReader(new FileReader(rateFile));
                    String line = br.readLine();
                    if (titleIsWritten) {
                        line = br.readLine();   // skip the title line
                    } else {
                        titleIsWritten = true;
                    }
                    
                    while (line != null) {
                        bw.write(line);
                        bw.newLine();
                        line = br.readLine();
                    }
                    
                    br.close();
                }
            }
            
            
            bw.close();
        }
    }
    
}
