import java.io.File;

import largeflow.emulator.LogParser;

public class Main_LogParser {

    public static void main(String[] args) throws Exception {

        LogParser logParser;
        
        if (args.length == 2 && args[0].equals("-p")) {
            System.out.println(args[0]);
            System.out.println(args[1]);
            logParser = new LogParser(new File(args[1]));
        } else if (args.length == 2 && args[0].equals("-n")) {
            System.out.println(args[0]);
            System.out.println(args[1]);
            logParser = new LogParser(args[1]);
        } else {
            logParser = new LogParser("test_packet_loss_exp");
        }
        
        logParser.parseTrafficAndDamage();
        
    }
    
}
