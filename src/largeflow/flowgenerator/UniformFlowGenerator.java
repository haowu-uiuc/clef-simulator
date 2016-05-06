package largeflow.flowgenerator;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.PacketWriter;

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

    private double[] largeFlowStartTimes;
    
	public UniformFlowGenerator() {
		super();
	}

	public UniformFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfSmallFlows,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			Integer smallFlowRate) {
		super(linkCapacity,
				timeInterval,
				packetSize,
				numOfSmallFlows,
				numOfLargeFlows,
				largeFlowRate,
				smallFlowRate);
		largeFlowStartTimes = new double[numOfLargeFlows];
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
        largeFlowStartTimes = new double[numOfLargeFlows];
	}

	public UniformFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer packetSize,
			Integer numOfSmallFlows,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			Integer smallFlowRate,
			Integer priorityLinkCapacity) {
		super(linkCapacity,
				timeInterval,
				packetSize,
				numOfSmallFlows,
				numOfLargeFlows,
				largeFlowRate,
				smallFlowRate,
				priorityLinkCapacity);
		largeFlowStartTimes = new double[numOfLargeFlows];
	}

	public UniformFlowGenerator(Integer linkCapacity,
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
		largeFlowStartTimes = new double[numOfLargeFlows];
	}

	@Override
	public void setNumOfLargeFlows(Integer numOfLargeFlows) {
        largeFlowStartTimes = new double[numOfLargeFlows];
        super.setNumOfLargeFlows(numOfLargeFlows);
    }
	
	@Override
	public void generateFlowsHelper() throws Exception {
		parameterCheck();
		
		if (outputPackets != null) {
			outputPackets.clear();
		}
		outputPackets = null;

	    largeFlowStartTimes = new double[numOfLargeFlows];
		
		Integer maxNumOfPacketsPerSecond = linkCapacity / largeFlowPacketSize;

		PacketWriter packetWriter = new PacketWriter(outputFile);
		int[] streamBuf = new int[maxNumOfPacketsPerSecond * timeInterval];

		double[] flowTime = new double[numOfLargeFlows];
		double packetTimeIntervals = (double) largeFlowPacketSize
				/ (double) largeFlowRate;

		if (dutyCycle != null && period != null && dutyCycle < period) {
		    // to keep the average rate the same, 
		    // we have to reduce the packet time interval 
		    // (increase rate in the duty cycle)
		    packetTimeIntervals *=  dutyCycle / period;
		}
		
		// randomly choose a starting time
		for (int fid = 1; fid <= numOfLargeFlows; fid++) {
			while (true) {
				int pos = (int) (randGenerator.nextDouble() * maxNumOfPacketsPerSecond - 0.00001);
				if (streamBuf[pos] == 0) {
					streamBuf[pos] = fid;
					flowTime[fid - 1] = (double) pos * (double) largeFlowPacketSize
							/ (double) linkCapacity;
					setStartTimeOfLargeFlow(fid, flowTime[fid - 1]);
					flowTime[fid - 1] += packetTimeIntervals;

					break;
				}
			}
		}

		// generate large flow in uniform distribution
        if (dutyCycle != null && period != null && dutyCycle < period) {
            // generate burst flows
            for (int fid = 1; fid <= numOfLargeFlows; fid++) {
                double periodStartTime = flowTime[fid - 1];
                
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
                    
                    if (flowTime[fid - 1] - periodStartTime + packetTimeIntervals >= dutyCycle) {
                        // if the current duty cycle is finished, 
                        // jump to the next period
                        periodStartTime += period;
                        flowTime[fid - 1] = periodStartTime;
                    }
                }

            }
            
        } else {
            // generate normal large flows
            for (int fid = 1; fid <= numOfLargeFlows; fid++) {
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
        }

		// generate small flows: randomly, but the average rate / sec
		// is approximately the smallFlowRate
		int smallFlowIdStart = numOfLargeFlows + 1;
		int smallFlowIdEnd = numOfLargeFlows
				+ numOfSmallFlows;
		double ratioOfSmallPacket = (double) numOfSmallFlows
				* (double) smallFlowRate
				/ ((double) linkCapacity
						- (double) (largeFlowRate * numOfLargeFlows));
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
		if (id >= 1 && id <= numOfLargeFlows) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isSmallFlow(FlowId flowId) {
		int id = flowId.getIntegerValue();
		if (id > numOfLargeFlows && 
			id <= numOfLargeFlows + numOfSmallFlows) {
			return true;
		}
		return false;
	}
	
	@Override
	public Integer getNumOfFlows() {
	    return numOfLargeFlows + numOfSmallFlows;
	}

    @Override
    public double getStartTimeOfLargeFlow(FlowId flowId) {
        return largeFlowStartTimes[flowId.getIntegerValue() - 1];
    }
    
    private void setStartTimeOfLargeFlow(int fid, double startTime) {
        largeFlowStartTimes[fid - 1] = startTime;
    }

}
