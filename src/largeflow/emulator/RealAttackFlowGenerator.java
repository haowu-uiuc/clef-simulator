package largeflow.emulator;

import java.io.File;
import java.io.IOException;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

public class RealAttackFlowGenerator extends UniAttackRateFlowGenerator {

	private UniAttackRateFlowGenerator attackFlowGenerator;
	private RealTrafficFlowGenerator realTrafficFlowGenerator;

	public RealAttackFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer largeFlowPacketSize,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			RealTrafficFlowGenerator realTrafficFlowGenerator) {
		super(linkCapacity,
				timeInterval,
				largeFlowPacketSize,
				numOfLargeFlows,
				largeFlowRate);
		
		this.realTrafficFlowGenerator = realTrafficFlowGenerator;
		initAttackFlowGenerator();
	}
	
	public RealAttackFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer largeFlowPacketSize,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			RealTrafficFlowGenerator realTrafficFlowGenerator,
			Integer bestEffortLinkCapacity) {
		super(linkCapacity,
				timeInterval,
				largeFlowPacketSize,
				numOfLargeFlows,
				largeFlowRate,
				bestEffortLinkCapacity);
		
		this.realTrafficFlowGenerator = realTrafficFlowGenerator;
		initAttackFlowGenerator();
	}
	
	private void initAttackFlowGenerator() {
		attackFlowGenerator = new UniformFlowGenerator(linkCapacity,
				timeInterval,
				largeFlowPacketSize,
				numOfLargeFlows,
				largeFlowRate);
		attackFlowGenerator.setOutputFile(new File("./" + this.hashCode() + "tmpflows.txt"));
	}

	@Override
	public void generateFlowsHelper() throws Exception {

		parameterCheck();

		if (outputPackets != null) {
			outputPackets.clear();
		}
		outputPackets = null;

		attackFlowGenerator.generateFlows();
		PacketReader attackPacketReader = PacketReaderFactory
				.getPacketReader(attackFlowGenerator.getOutputFile());
		PacketReader realPacketReader = PacketReaderFactory
				.getPacketReader(realTrafficFlowGenerator.getOutputFile());

		QueueDiscipline gatewayRouter = new QueueDiscipline(linkCapacity);
		PacketWriter packetWriter = new PacketWriter(outputFile);

		Packet attackPacket = attackPacketReader.getNextPacket();
		Packet realPacket = realPacketReader.getNextPacket();

		while (attackPacket != null || realPacket != null) {

			if (attackPacket != null && realPacket != null) {
				if (realPacket.time < attackPacket.time) {
					realPacket.flowId = deduplicateRealFlowId(realPacket.flowId);
					gatewayRouter.processPacket(realPacket);
					realPacket = realPacketReader.getNextPacket();
				} else {
					gatewayRouter.processPacket(attackPacket);
					attackPacket = attackPacketReader.getNextPacket();
				}
			} else if (attackPacket != null) {
				gatewayRouter.processPacket(attackPacket);
				attackPacket = attackPacketReader.getNextPacket();
			} else {
				realPacket.flowId = deduplicateRealFlowId(realPacket.flowId);
				gatewayRouter.processPacket(realPacket);
				realPacket = realPacketReader.getNextPacket();
			}

			// output adjusted packets in the queue
			Packet adjustedPacket;
			while ((adjustedPacket = gatewayRouter.getNextPacket()) != null) {
				if (adjustedPacket.time > timeInterval) {
					break;
				}

				packetWriter.writePacket(adjustedPacket);
			}

			if (adjustedPacket != null && adjustedPacket.time > timeInterval) {
				break;
			}

		}

		attackPacketReader.close();
		realPacketReader.close();
		packetWriter.close();
		attackFlowGenerator.getOutputFile().delete();
	}

	@Override
	public void logConfig(Logger logger) throws IOException {
		super.logConfig(logger);
		logger.logConfigMsg("Real Traffic File: " + realTrafficFlowGenerator.getOutputFile() + "\n");
	}

	@Override
	public boolean isLargeFlow(FlowId flowId) {
		return attackFlowGenerator.isLargeFlow(flowId);
	}

	@Override
	public boolean isSmallFlow(FlowId flowId) {
		return attackFlowGenerator.isSmallFlow(flowId);
	}

	@Override
	public boolean isBurstFlow(FlowId flowId) {
		return attackFlowGenerator.isBurstFlow(flowId);
	}

	@Override
	public void setAttackRate(Integer attackFlowRate) {
		this.largeFlowRate = attackFlowRate;
		attackFlowGenerator.setAttackRate(attackFlowRate);
	}

	private FlowId deduplicateRealFlowId(FlowId realFlowId) {
		return new FlowId(numOfLargeFlows + realFlowId.getIntegerValue());
	}
	
	public Integer getNumOfAttFlows() throws Exception {
	    return attackFlowGenerator.getNumOfFlows();
	}
	
	public Integer getNumOfRealFlows() throws Exception {
	    return realTrafficFlowGenerator.getNumOfFlows();
	}
	
	@Override
	public Integer getNumOfFlows() throws Exception {
	    return realTrafficFlowGenerator.getNumOfFlows() 
	            + attackFlowGenerator.getNumOfFlows();
	}
	
	public Integer getAveRealTrafficRate() throws Exception {
	    return realTrafficFlowGenerator.getAveRealTrafficRate();
	}
	
	public Integer getPriorityBandwidthOfOneFlow() throws Exception {
	    return priorityLinkCapacity / getNumOfFlows();
	}
	
	@Override
	protected void parameterCheck() throws Exception {
		super.parameterCheck();
		if (realTrafficFlowGenerator == null) {
			throw new Exception("Please set the real traffic flow generator");
		}
		if (!realTrafficFlowGenerator.getOutputFile().exists()) {
			throw new Exception("Please let RealTrafficFlowGenerator generate the flow first");
		}
	}

}
