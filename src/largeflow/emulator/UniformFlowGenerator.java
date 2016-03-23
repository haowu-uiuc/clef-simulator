package largeflow.emulator;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

/**
 * Uniform flow generator generates attack flows with rate largeFlowRate byte /
 * sec with some fixed packet size packetSize For each large flow, its starting
 * timing is randomly chosen, but the time interval of each two neighbour
 * packets are almost the same. (Attacker tries his best to make no burst in the
 * attack flow, because busrt is more likely to result detection by large flow
 * detector) Such uniform flow could result in more damage to the network,
 * because attacker can achieve higher flow rate without caugth by detector.
 * 
 * TODO: Small flow and burst flow are still not finished in this flow generator
 * 
 * @author HaoWu
 *
 */
public class UniformFlowGenerator extends UniAttackRateFlowGenerator {

	public UniformFlowGenerator() {
		super();
	}

	public UniformFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfSmallFlows,
			Integer numOfLargeFlows,
			Integer numOfBurstFlows,
			Integer largeFlowRate,
			Integer smallFlowRate,
			Integer burstFlowSize) {
		super(linkCapacity,
				timeInterval,
				packetSize,
				numOfSmallFlows,
				numOfLargeFlows,
				numOfBurstFlows,
				largeFlowRate,
				smallFlowRate,
				burstFlowSize);
	}

	public UniformFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfLargeFlows,
			Integer largeFlowRate) {
		super(linkCapacity,
				timeInterval,
				packetSize,
				numOfLargeFlows,
				largeFlowRate);
	}

	public UniformFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfSmallFlows,
			Integer numOfLargeFlows,
			Integer numOfBurstFlows,
			Integer largeFlowRate,
			Integer smallFlowRate,
			Integer burstFlowSize,
			Integer bestEffortLinkCapacity) {
		super(bestEffortLinkCapacity,
				timeInterval,
				packetSize,
				numOfSmallFlows,
				numOfLargeFlows,
				numOfBurstFlows,
				largeFlowRate,
				smallFlowRate,
				burstFlowSize);
	}

	public UniformFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			Integer bestEffortLinkCapacity) {
		super(linkCapacity,
				timeInterval,
				packetSize,
				numOfLargeFlows,
				largeFlowRate,
				bestEffortLinkCapacity);
	}

	@Override
	public void generateFlowsHelper() throws Exception {
		parameterCheck();
		
		if (outputPackets != null) {
			outputPackets.clear();
		}
		outputPackets = null;

		Integer maxNumOfPacketsPerSecond = linkCapacity / largeFlowPacketSize;

		PacketWriter packetWriter = new PacketWriter(outputFile);
		int[] streamBuf = new int[maxNumOfPacketsPerSecond * timeInterval];

		double[] flowTime = new double[numOfLargeFlows];
		double packetTimeIntervals = (double) largeFlowPacketSize
				/ (double) largeFlowRate;

		// TODO: generate burst

		// randomly choose a starting time
		for (int fid = 1 + numOfBurstFlows; fid <= numOfLargeFlows
				+ numOfBurstFlows; fid++) {
			while (true) {
				int pos = (int) (randGenerator.nextDouble() * maxNumOfPacketsPerSecond - 0.00001);
				if (streamBuf[pos] == 0) {
					streamBuf[pos] = fid;
					flowTime[fid - 1] = (double) pos * (double) largeFlowPacketSize
							/ (double) linkCapacity;
					flowTime[fid - 1] += packetTimeIntervals;

					break;
				}
			}
		}

		// generate large flow in uniform distribution
		for (int fid = 1 + numOfBurstFlows; fid <= numOfLargeFlows
				+ numOfBurstFlows; fid++) {
			while (flowTime[fid - 1] < timeInterval) {
				int s = (int) flowTime[fid - 1];
				int pos = (int) ((flowTime[fid - 1] - s)
						* maxNumOfPacketsPerSecond - 0.00001)
						+ s * maxNumOfPacketsPerSecond;

				while (pos < streamBuf.length && streamBuf[pos] != 0) {
					pos++;
				}

				if (pos >= streamBuf.length) {
					break;
				}

				streamBuf[pos] = fid;
				flowTime[fid - 1] += packetTimeIntervals;
			}

		}

		// generate small flows: randomly, but the average rate / sec
		// is approximately the smallFlowRate
		int smallFlowIdStart = numOfBurstFlows + numOfLargeFlows + 1;
		int smallFlowIdEnd = numOfBurstFlows + numOfLargeFlows
				+ numOfSmallFlows;
		double ratioOfSmallPacket = (double) numOfSmallFlows
				* (double) smallFlowRate
				/ ((double) linkCapacity
						- (double) (largeFlowRate * numOfLargeFlows) - (double) (numOfBurstFlows * burstFlowSize));
		if (ratioOfSmallPacket > 1.0) {
			System.out.println("The volume of small flow is larger"
					+ " than the rest of link capacity!");
		}

		for (int i = 0; i < streamBuf.length; i++) {
			if (streamBuf[i] != 0) {
				continue;
			}

			if (randGenerator.nextDouble() < ratioOfSmallPacket) {
				int fid = (int) ((randGenerator.nextDouble() - 0.00001) * (smallFlowIdEnd
						- smallFlowIdStart + 1))
						+ smallFlowIdStart;
				streamBuf[i] = fid;
			}

		}

		// write the stream into file
		for (int i = 0; i < streamBuf.length; i++) {
			if (streamBuf[i] != 0) {
				Packet packet = new Packet(new FlowId(streamBuf[i]),
						largeFlowPacketSize,
						((double) i * (double) largeFlowPacketSize
								/ (double) linkCapacity + 0.0001));

				packetWriter.writePacket(packet);
			}
		}

		packetWriter.close();
	}

	@Override
	public boolean isLargeFlow(FlowId flowId) {
		int id = flowId.getIntegerValue();
		if (id >= 1 + numOfBurstFlows
				&& id <= numOfBurstFlows + numOfLargeFlows) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isSmallFlow(FlowId flowId) {
		int id = flowId.getIntegerValue();
		if (id > numOfBurstFlows + numOfLargeFlows && 
			id <= numOfBurstFlows + numOfLargeFlows + numOfSmallFlows) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isBurstFlow(FlowId flowId) {
		int id = flowId.getIntegerValue();
		if (id >= 0 && id <= numOfBurstFlows) {
			return true;
		}
		return false;
	}
	
	@Override
	public Integer getNumOfFlows() {
	    return numOfBurstFlows + numOfLargeFlows + numOfSmallFlows;
	}

}
