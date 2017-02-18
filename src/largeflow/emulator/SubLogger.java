package largeflow.emulator;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import largeflow.datatype.Damage;

public class SubLogger implements Closeable, Flushable {

    private File logFile;
    private BufferedWriter writer;
    private boolean titleIsWritten;
    
    public SubLogger(String logFilePath) throws IOException {
        logFile = new File(logFilePath);
        if (!logFile.getParentFile().exists()) {
            logFile.getParentFile().mkdirs();
        }
        writer = new BufferedWriter(new FileWriter(logFile));
        titleIsWritten = false;
    }
    
    @Override
    public void close() throws IOException {
        writer.close();
    }
    
    @Override
    public void flush() throws IOException {
        writer.flush();
    }
    
    /**
     * log the damage result and traffic result into this log file
     * @param router
     * @param atkRate
     * @param numOfCounters
     * @param round
     * @param attackReservedTraffic
     * @param preQdRealTraffic
     * @param postQdAttackTraffic
     * @param postQdRealTraffic
     * @param blockedRealTraffic
     * @param outboundCapacity
     * @param damage
     * @throws IOException
     */
    public void logDamageAndTraffic(Router router,
            int atkRate,
            int numOfCounters,
            int round,
            long attackReservedTraffic,
            long preQdRealTraffic,
            long postQdAttackTraffic,
            long postQdRealTraffic,
            long blockedRealTraffic,
            long outboundCapacity, 
            Damage damage) throws IOException {
        if (!titleIsWritten) {
            writer.write("round\t");
            writer.write("attack_rate\t");
            writer.write("number_of_counters\t");
            writer.write("attack_reservation\t");
            writer.write("pre_QD_real_traffic\t");
            writer.write("post_QD_attack_traffic\t");
            writer.write("post_QD_real_traffic\t");
            writer.write("blocked_real_traffic\t");
            writer.write("outbound_link_capacity\t");
            List<String> columnNameList = Damage.getTitleList();            
            writer.write(String.join("\t", columnNameList) + "\n");
            titleIsWritten = true;
        }
        
        List<String> valueList = Arrays.asList(
                String.valueOf(round),
                String.valueOf(atkRate),
                String.valueOf(numOfCounters),
                String.valueOf(attackReservedTraffic),
                String.valueOf(preQdRealTraffic),
                String.valueOf(postQdAttackTraffic),
                String.valueOf(postQdRealTraffic),
                String.valueOf(blockedRealTraffic),
                String.valueOf(outboundCapacity),
                String.valueOf(damage.FP),
                String.valueOf(damage.FN),
                String.valueOf(damage.TP),
                String.valueOf(damage.perFlowDamage),
                String.valueOf(damage.totalDamage),
                String.valueOf(damage.BE_damage),
                String.valueOf(damage.FP_damage),
                String.valueOf(damage.QD_drop_damage),
                String.valueOf(damage.baseline_damage));
        writer.write(String.join("\t", valueList) + "\n");
    }

}
