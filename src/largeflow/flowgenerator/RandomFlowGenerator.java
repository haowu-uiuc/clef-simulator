package largeflow.flowgenerator;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.PacketWriter;

/**
 * Random flow generator generates attack flows with rate largeFlowRate byte /
 * sec with some fixed packet size packetSize For each large flow, its starting
 * timing is randomly chosen, and for each second, we randomly distribute the
 * packets into this one second. (The {# of packets in one second} * packetSize
 * / 1 sec == largeFlowRate, but there could be burst in this one second) Such
 * flow could be easier to catch, because of the burts
 * 
 * TODO: Small flow and burst flow are still not finished in this flow generator
 * 
 * @author HaoWu
 *
 */
public class RandomFlowGenerator extends UniAttackRateFlowGenerator {

	public RandomFlowGenerator() {
		super();
	}

	public RandomFlowGenerator(Integer linkCapacity,
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

	public RandomFlowGenerator(Integer linkCapacity,
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

	public RandomFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfSmallFlows,
			Integer numOfLargeFlows,
			Integer numOfBurstFlows,
			Integer largeFlowRate,
			Integer smallFlowRate,
			Integer burstFlowSize,
			Integer priorityLinkCapacity) {
		super(linkCapacity,
				timeInterval,
				packetSize,
				numOfSmallFlows,
				numOfLargeFlows,
				numOfBurstFlows,
				largeFlowRate,
				smallFlowRate,
				burstFlowSize,
				priorityLinkCapacity);
	}

	public RandomFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			Integer priorityLinkCapacity) {
		super(linkCapacity,
				timeInterval,
				packetSize,
				numOfLargeFlows,
				largeFlowRate,
				priorityLinkCapacity);
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
		int[] streamBuf = new int[maxNumOfPacketsPerSecond];

		// TODO: generate burst

		// generate large flow
		for (int s = 0; s < timeInterval; s++) {
			for (int j = 0; j < streamBuf.length; j++) {
				streamBuf[j] = 0;
			}
			int emptyBuf = streamBuf.length;

			for (int fid = 1 + numOfBurstFlows; fid <= numOfLargeFlows
					+ numOfBurstFlows; fid++) {
				int i = 0;
				while (i < largeFlowRate / largeFlowPacketSize && emptyBuf > 0) {
					int pos = (int) (randGenerator.nextDouble() * maxNumOfPacketsPerSecond - 0.00001);
					if (streamBuf[pos] != 0) {
						continue;
					}

					streamBuf[pos] = fid;
					i++;
					emptyBuf--;
				}
			}

			// generate small flows: randomly, but the average rate / sec
			// is approximately the smallFlowRate
			int smallFlowIdStart = numOfBurstFlows + numOfLargeFlows + 1;
			int smallFlowIdEnd = numOfBurstFlows + numOfLargeFlows
					+ numOfSmallFlows;
			double ratioOfSmallPacket = (double) numOfSmallFlows
					* (double) smallFlowRate / ((double) linkCapacity
					- (double) (largeFlowRate * numOfLargeFlows) 
					- (double) (numOfBurstFlows * burstFlowSize));
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

			// write the second into file
			for (int i = 0; i < maxNumOfPacketsPerSecond; i++) {
				if (streamBuf[i] != 0) {
					Packet packet = new Packet(new FlowId(streamBuf[i]),
							largeFlowPacketSize,
							((double) i * (double) largeFlowPacketSize
									/ (double) linkCapacity + 0.0001 + s));

					packetWriter.writePacket(packet);
				}
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
			id <= numOfBurstFlows + numOfLargeFlows +numOfSmallFlows) {
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
	
	@Override
    public double getStartTimeOfLargeFlow(FlowId flowId) {
        return 0.;
    }

}
