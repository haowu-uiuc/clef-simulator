package largeflow.flowgenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import largeflow.datatype.Packet;
import largeflow.emulator.Logger;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;

public abstract class FlowGenerator {
	
	protected Integer linkCapacity; // Byte
	protected Integer priorityLinkCapacity; // Byte
	
	protected Integer timeInterval; // seconds, length of packet stream

	protected File outputFile; // File to output the flow
	protected List<Packet> outputPackets; // list of packets of the flows
	protected boolean flowsAreGenerated = false;
	
	protected Random randGenerator;
	
	public FlowGenerator() {}
	
	public FlowGenerator(Integer linkCapacity,
			Integer timeInterval) {
		this.linkCapacity = linkCapacity;
		this.timeInterval = timeInterval;
		this.priorityLinkCapacity = this.linkCapacity;

		randGenerator = new Random((long) (Long.MAX_VALUE * Math.random()));
	}
	
	public FlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer priorityLinkCapacity) {
		this.linkCapacity = linkCapacity;
		this.timeInterval = timeInterval;
		this.priorityLinkCapacity = priorityLinkCapacity;
		
		randGenerator = new Random((long) (Long.MAX_VALUE * Math.random()));
	}
	
	public void setLinkCapacity(Integer linkCapacity) {
		this.linkCapacity = linkCapacity;
	}

	public void setTimeInterval(Integer timeInterval) {
		this.timeInterval = timeInterval;
	}
	
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	public File getOutputFile() {
		return outputFile;
	}
	
	public void logConfig(Logger logger) throws IOException {
		logger.logConfigMsg("Flow Generator Class: "
				+ this.getClass().getName() + "\n");
		logger.logConfigMsg("Link Capacity: " + linkCapacity + "\n");
		logger.logConfigMsg("Priority Link Capacity: " + priorityLinkCapacity + " Byte\n");
		logger.logConfigMsg("Time Period: " + timeInterval + " sec\n");
	}
	
	public List<Packet> getOutputPackets() throws IOException {
		if (outputPackets != null) {
			return outputPackets;
		}
		
		// read packets into list
		outputPackets = new ArrayList<>();
		PacketReader packetReader = PacketReaderFactory.getPacketReader(outputFile);
		Packet packet;
		while ((packet = packetReader.getNextPacket()) != null) {
			outputPackets.add(packet);
		}
		
		return outputPackets;
	}
	
	public Double getTraceLength() {
		return (double) timeInterval;
	}
	
	public int getLinkCapacity() {
		return linkCapacity;
	}

	public int getPriorityLinkCapacity() {
		return priorityLinkCapacity;
	}
	
	public void generateFlows() throws Exception {		
		generateFlowsHelper();
		flowsAreGenerated = true;
	}
	
	abstract public Integer getNumOfFlows() throws Exception;
	
	/**
	 * Implement method of generating flows here
	 * @throws Exception
	 */
	abstract public void generateFlowsHelper() throws Exception;
		
	protected void parameterCheck() throws Exception {
		if (linkCapacity == null) {
			throw new Exception("Please set link capacity for flow generator.");
		}

		if (timeInterval == null) {
			throw new Exception("Please set time interval for flow generator.");
		}

		if (outputFile == null) {
			throw new Exception("Please set output file");
		}
	}
	
	public void deleteOutputFile() {
	    if (outputFile != null && outputFile.exists()) {
	        outputFile.delete();
	    }
	}
	
}
