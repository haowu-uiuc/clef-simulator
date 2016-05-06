package largeflow.flowgenerator;

import java.io.IOException;

import largeflow.datatype.FlowId;
import largeflow.emulator.Logger;

public abstract class AttackFlowGenerator extends FlowGenerator {
	
	protected Integer largeFlowPacketSize; // Byte, packet size for generated large flows
	protected Integer smallFlowPacketSize; // Byte, packet size for generated small flows	
	protected Integer numOfSmallFlows; // number of small flows to generate
	protected Integer numOfLargeFlows; // number of large flows to generate
	protected Integer largeFlowRate; // rate of large flows
	protected Integer smallFlowRate; // rate of small flows
	
	public AttackFlowGenerator() {
		super();
	}
	
	public AttackFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfSmallFlows,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			Integer smallFlowRate) {
		super(linkCapacity, timeInterval);
		
		this.largeFlowPacketSize = packetSize;
		this.smallFlowPacketSize = packetSize;
		this.numOfSmallFlows = numOfSmallFlows;
		this.numOfLargeFlows = numOfLargeFlows;
		this.largeFlowRate = largeFlowRate;
		this.smallFlowRate = smallFlowRate;
		this.priorityLinkCapacity = this.linkCapacity;
	}

	public AttackFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfLargeFlows,
			Integer largeFlowRate) {
		super(linkCapacity, timeInterval);
		
		this.largeFlowPacketSize = packetSize;
		this.numOfSmallFlows = 0;
		this.numOfLargeFlows = numOfLargeFlows;
		this.largeFlowRate = largeFlowRate;
		this.smallFlowRate = 0;
		this.priorityLinkCapacity = this.linkCapacity;
	}
	
	public AttackFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfSmallFlows,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			Integer smallFlowRate,
			Integer priorityLinkCapacity) {
		super(linkCapacity, timeInterval, priorityLinkCapacity);
		
		this.largeFlowPacketSize = packetSize;
		this.smallFlowPacketSize = packetSize;
		this.numOfSmallFlows = numOfSmallFlows;
		this.numOfLargeFlows = numOfLargeFlows;
		this.largeFlowRate = largeFlowRate;
		this.smallFlowRate = smallFlowRate;
	}

	public AttackFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			Integer priorityLinkCapacity) {
		super(linkCapacity, timeInterval, priorityLinkCapacity);
		
		this.largeFlowPacketSize = packetSize;
		this.numOfSmallFlows = 0;
		this.numOfLargeFlows = numOfLargeFlows;
		this.largeFlowRate = largeFlowRate;
		this.smallFlowRate = 0;
	}
	
	public void setPacketSize(Integer packetSize) {
		this.largeFlowPacketSize = packetSize;
	}

	public void setNumOfSmallFlows(Integer numOfSmallFlows) {
		this.numOfSmallFlows = numOfSmallFlows;
	}

	public void setNumOfLargeFlows(Integer numOfLargeFlows) {
		this.numOfSmallFlows = numOfLargeFlows;
	}

	public void setLargeFlowRate(Integer largeFlowRate) {
		this.largeFlowRate = largeFlowRate;
	}

	public void setSmallFlowRate(Integer smallFlowRate) {
		this.smallFlowRate = smallFlowRate;
	}
	
	public void logConfig(Logger logger) throws IOException {
		super.logConfig(logger);
		logger.logConfigMsg("Large Flow Packet Size: " + largeFlowPacketSize + " Byte\n");
		logger.logConfigMsg("Small Flow Packet Size: " + smallFlowPacketSize + " Byte\n");
		logger.logConfigMsg("Small Flow Number: " + numOfSmallFlows + "\n");
		logger.logConfigMsg("Large Flow Number: " + numOfLargeFlows + "\n");
		logger.logConfigMsg("Small Flow Rate: " + smallFlowRate
				+ " Byte / sec\n");
		logger.logConfigMsg("Large Flow Rate: " + largeFlowRate
				+ " Byte / sec\n");
	}
	
	abstract public boolean isLargeFlow(FlowId flowId);
	
	abstract public boolean isSmallFlow(FlowId flowId);
	
	abstract public double getStartTimeOfLargeFlow(FlowId flowId);
		
	protected void parameterCheck() throws Exception {
		super.parameterCheck();
		
		if (largeFlowPacketSize == null) {
			throw new Exception("Please set packet size for flow generator.");
		}

		if (numOfSmallFlows == null) {
			throw new Exception("Please set number of small flows to generate.");
		}

		if (numOfLargeFlows == null) {
			throw new Exception("Please set number of large flows to generate.");
		}

		if (largeFlowRate == null) {
			throw new Exception("Please set rate of small flows to generate.");
		}

		if (smallFlowRate == null) {
			throw new Exception("Please set rate of large flows to generate.");
		}
	}
}
