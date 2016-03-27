package largeflow.emulator.test;

import static org.mockito.Mockito.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.SingleRouterRunner;
import largeflow.emulator.SimpleRouter;
import largeflow.emulator.PacketReaderFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class RouterRunnerTest {

	static private File trafficFile;
	
	@BeforeClass
	static public void setUpClass() throws IOException {
		File outputDir = new File("./data/test");
		if (!outputDir.exists()){
			outputDir.mkdirs();
		}
		
		trafficFile = new File(outputDir + "/test_traffic.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(trafficFile));
		bw.write("65 100 0.026824");
		bw.newLine();
		bw.write("65 100 0.027488");
		bw.newLine();
		bw.write("71 100 0.035016000000000005");
		bw.newLine();
		bw.close();
	}
	
	@AfterClass
	static public void tearDownClass() {
		trafficFile.delete();
	}

	@Test
	public void test() throws Exception {
		SingleRouterRunner routerRunner = new SingleRouterRunner();
		routerRunner.setPacketReader(PacketReaderFactory.getPacketReader(trafficFile));
		
		//mock
	    Detector baseDetector = Mockito.mock(Detector.class);
		Detector mockDetector1 = Mockito.mock(Detector.class);
		Detector mockDetector2 = Mockito.mock(Detector.class);
		when(mockDetector1.processPacket(any(Packet.class))).thenReturn(true);
		when(mockDetector2.processPacket(any(Packet.class))).thenReturn(true);
        when(baseDetector.processPacket(any(Packet.class))).thenReturn(true);
		
        routerRunner.setBaseDetector(baseDetector);
        routerRunner.addRouter(new SimpleRouter(mockDetector1));
		routerRunner.addRouter(new SimpleRouter(mockDetector2));
		
		routerRunner.run();
	}

}
