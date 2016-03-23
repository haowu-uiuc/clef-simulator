package largeflow.emulator;

import java.io.IOException;

import largeflow.datatype.FlowId;

public abstract class AttackFlowGenerator extends FlowGenerator {
	
	protected Integer largeFlowPacketSize; // Byte, packet size for generated large flows
	protected Integer burstFlowPacketSize; // Byte, packet size for generated burst flows
	protected Integer smallFlowPacketSize; // Byte, packet size for generated small flows	
	protected Integer numOfSmallFlows; // number of small flows to generate
	protected Integer numOfLargeFlows; // number of large flows to generate
	protected Integer numOfBurstFlows; // number of burst flows to generate
	protected Integer largeFlowRate; // rate of large flows
	protected Integer smallFlowRate; // rate of small flows
	protected Integer burstFlowSize; // size of each burst

	public AttackFlowGenerator() {
		super();
	}
	
	public AttackFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfSmallFlows,
			Integer numOfLargeFlows,
			Integer numOfBurstFlows,
			Integer largeFlowRate,
			Integer smallFlowRate,
			Integer burstFlowSize) {
		super(linkCapacity, timeInterval);
		
		this.largeFlowPacketSize = packetSize;
		this.smallFlowPacketSize = packetSize;
		this.burstFlowPacketSize = packetSize;
		this.numOfSmallFlows = numOfSmallFlows;
		this.numOfLargeFlows = numOfLargeFlows;
		this.numOfBurstFlows = numOfBurstFlows;
		this.largeFlowRate = largeFlowRate;
		this.smallFlowRate = smallFlowRate;
		this.burstFlowSize = burstFlowSize;
		this.bestEffortLinkCapacity = 0;
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
		this.numOfBurstFlows = 0;
		this.largeFlowRate = largeFlowRate;
		this.smallFlowRate = 0;
		this.burstFlowSize = 0;
		this.bestEffortLinkCapacity = 0;
		this.priorityLinkCapacity = this.linkCapacity;
	}
	
	public AttackFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfSmallFlows,
			Integer numOfLargeFlows,
			Integer numOfBurstFlows,
			Integer largeFlowRate,
			Integer smallFlowRate,
			Integer burstFlowSize,
			Integer bestEffortLinkCapacity) {
		super(linkCapacity, timeInterval, bestEffortLinkCapacity);
		
		this.largeFlowPacketSize = packetSize;
		this.smallFlowPacketSize = packetSize;
		this.burstFlowPacketSize = packetSize;
		this.numOfSmallFlows = numOfSmallFlows;
		this.numOfLargeFlows = numOfLargeFlows;
		this.numOfBurstFlows = numOfBurstFlows;
		this.largeFlowRate = largeFlowRate;
		this.smallFlowRate = smallFlowRate;
		this.burstFlowSize = burstFlowSize;
	}

	public AttackFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			Integer bestEffortLinkCapacity) {
		super(linkCapacity, timeInterval, bestEffortLinkCapacity);
		
		this.largeFlowPacketSize = packetSize;
		this.numOfSmallFlows = 0;
		this.numOfLargeFlows = numOfLargeFlows;
		this.numOfBurstFlows = 0;
		this.largeFlowRate = largeFlowRate;
		this.smallFlowRate = 0;
		this.burstFlowSize = 0;
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

	public void setNumOfBurstFlows(Integer numOfBurstFlows) {
		this.numOfSmallFlows = numOfBurstFlows;
	}

	public void setLargeFlowRate(Integer largeFlowRate) {
		this.largeFlowRate = largeFlowRate;
	}

	public void setSmallFlowRate(Integer smallFlowRate) {
		this.smallFlowRate = smallFlowRate;
	}

	public void setBurstFlowSize(Integer burstFlowSize) {
		this.burstFlowSize = burstFlowSize;
	}
	
	public void logConfig(Logger logger) throws IOException {
		super.logConfig(logger);
		logger.logConfigMsg("Large Flow Packet Size: " + largeFlowPacketSize + " Byte\n");
		logger.logConfigMsg("Burst Flow Packet Size: " + burstFlowPacketSize + " Byte\n");
		logger.logConfigMsg("Small Flow Packet Size: " + smallFlowPacketSize + " Byte\n");
		logger.logConfigMsg("Small Flow Number: " + numOfSmallFlows + "\n");
		logger.logConfigMsg("Burst Flow Number: " + numOfBurstFlows + "\n");
		logger.logConfigMsg("Large Flow Number: " + numOfLargeFlows + "\n");
		logger.logConfigMsg("Small Flow Rate: " + smallFlowRate
				+ " Byte / sec\n");
		logger.logConfigMsg("Large Flow Rate: " + largeFlowRate
				+ " Byte / sec\n");
		logger.logConfigMsg("Burst Flow Size: " + burstFlowSize + " Byte\n");
	}
	
	abstract public boolean isLargeFlow(FlowId flowId);
	
	abstract public boolean isSmallFlow(FlowId flowId);
	
	abstract public boolean isBurstFlow(FlowId flowId);
		
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

		if (numOfBurstFlows == null) {
			throw new Exception("Please set number of burst flows to generates.");
		}

		if (largeFlowRate == null) {
			throw new Exception("Please set rate of small flows to generate.");
		}

		if (smallFlowRate == null) {
			throw new Exception("Please set rate of large flows to generate.");
		}

		if (burstFlowSize == null) {
			throw new Exception("Please set rate of burst flows to generates.");
		}
	}
	
}
