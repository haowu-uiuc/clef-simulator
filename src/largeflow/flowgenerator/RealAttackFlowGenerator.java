package largeflow.flowgenerator;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Logger;
import largeflow.emulator.PacketReader;
import largeflow.emulator.PacketReaderFactory;
import largeflow.emulator.PacketWriter;
import largeflow.emulator.QueueDiscipline;
import largeflow.utils.GenericUtils;

/**
 * combine the real traffic and attack flows (uniform distributed)
 * @author HaoWu
 *
 */
public class RealAttackFlowGenerator extends UniAttackRateFlowGenerator {

    // flow generator parameters.
	private UniAttackRateFlowGenerator attackFlowGenerator;
	private UniAttackRateFlowGenerator legitimateFlowGenerator;
	private RealTrafficFlowGenerator realTrafficFlowGenerator;
	private Integer fullRealFlowPacketSize;
	private Integer numOfFullRealFlows;
	private Integer numOfUnderUseRealFlows;
	private Integer perFlowReservation;    // Byte / sec
	
	// variables need to reset at beginning of each call of generateHelper()
	private Set<FlowId> attackFlowSet;
	private Set<FlowId> fullRealFlowSet;
	private Set<FlowId> underUseRealFlowSet;
	private long underUseTrafficVolume;
	private long fullRealTrafficVolume;
	
	// variables need to take care when reset the flow generator
	private Set<FlowId> originalUnderUseFlowSet;
	private boolean fullRealFlowsGenerated;

	public RealAttackFlowGenerator(Integer linkCapacity,
			Integer timeInterval,
			Integer largeFlowPacketSize,
			Integer numOfLargeFlows,
			Integer largeFlowRate,
			Integer perFlowReservation,
			Integer fullRealFlowPacketSize,
			Integer numOfFullRealFlows,
			Integer numOfUnderUseRealFlows,
			RealTrafficFlowGenerator realTrafficFlowGenerator) {
		super(linkCapacity,
                timeInterval,
                largeFlowPacketSize,
                numOfLargeFlows,
                largeFlowRate,
                perFlowReservation * (numOfFullRealFlows + numOfFullRealFlows + numOfUnderUseRealFlows));
		Integer totalPriorityCapacity = perFlowReservation 
                * (numOfFullRealFlows + numOfFullRealFlows + numOfUnderUseRealFlows);
		
		this.fullRealFlowPacketSize = fullRealFlowPacketSize;
		this.perFlowReservation = perFlowReservation;
		this.numOfFullRealFlows = numOfFullRealFlows;
		this.numOfUnderUseRealFlows = numOfUnderUseRealFlows;
		
		if (totalPriorityCapacity > linkCapacity) {
		    System.out.println("Over Reservation. "
		            + "Total reservation capacty "
		            + "is higher than the link capacity");
		}
		
		this.realTrafficFlowGenerator = realTrafficFlowGenerator;
		initAttackFlowGenerator();
		initLegitimateFlowGenerator();
		
		attackFlowSet = new HashSet<>(numOfLargeFlows);
	    fullRealFlowSet = new HashSet<>(numOfFullRealFlows);
	    underUseRealFlowSet = new HashSet<>(numOfUnderUseRealFlows);
	    underUseTrafficVolume = 0;
	    fullRealTrafficVolume = 0;
	    
	    fullRealFlowsGenerated = false; 
	    originalUnderUseFlowSet = null;
	}
	
	private void initAttackFlowGenerator() {
		attackFlowGenerator = new UniformFlowGenerator(linkCapacity,
				timeInterval,
				largeFlowPacketSize,
				numOfLargeFlows,
				largeFlowRate);
	}
	
	private void initLegitimateFlowGenerator() {
        legitimateFlowGenerator = new UniformFlowGenerator(linkCapacity,
                timeInterval,
                fullRealFlowPacketSize,
                numOfFullRealFlows,
                perFlowReservation);
    }

	@Override
	public void setOutputFile(File outputFile) {
	    super.setOutputFile(outputFile);
	    attackFlowGenerator.setOutputFile(new File(outputFile.getParentFile() + "/"
                + this.hashCode() + "tmpAtkflows.txt"));
	    legitimateFlowGenerator.setOutputFile(new File(outputFile.getParentFile() + "/"
                + this.hashCode() + "tmpLegalflows.txt"));
	}
	
	/**
	 * reset flow generator. 
	 * It will re-generate the full real traffic anyway next time.
	 * It will randomly re-pick up under-use real traffic anyway next time.
	 */
	public void reset() {
	    originalUnderUseFlowSet = null;
	    fullRealFlowsGenerated = false;
	}
	
	@Override
	public void generateFlowsHelper() throws Exception {

		parameterCheck();

		if (outputPackets != null) {
			outputPackets.clear();
		}
		outputPackets = null;
		
		attackFlowSet = new HashSet<>(numOfLargeFlows);
        fullRealFlowSet = new HashSet<>(numOfFullRealFlows);
        underUseRealFlowSet = new HashSet<>(numOfUnderUseRealFlows);
        fullRealTrafficVolume = 0;
        underUseTrafficVolume = 0;
        
		if (originalUnderUseFlowSet == null) {
    		originalUnderUseFlowSet = realTrafficFlowGenerator.getFlowIdSet();
    		if (realTrafficFlowGenerator.getNumOfFlows() < numOfUnderUseRealFlows) {
    		    System.out.println("Num of flows in real trace < "
    		            + "num of under-use real flows we need.");
    		} else {
        		originalUnderUseFlowSet = GenericUtils.
        		        getRandomSubFlowSet(numOfUnderUseRealFlows, 
        		                realTrafficFlowGenerator.getFlowIdSet());
    		}
		}
		
		
		attackFlowGenerator.generateFlows();
		if (!fullRealFlowsGenerated) {
		    legitimateFlowGenerator.generateFlows();
		    fullRealFlowsGenerated = true;
		}
		PacketReader attackPacketReader = PacketReaderFactory
				.getPacketReader(attackFlowGenerator.getOutputFile());
		PacketReader realPacketReader = PacketReaderFactory
				.getPacketReader(realTrafficFlowGenerator.getOutputFile());
		PacketReader fullRealPacketReader = PacketReaderFactory
		        .getPacketReader(legitimateFlowGenerator.getOutputFile());

		QueueDiscipline gatewayRouter = new QueueDiscipline(linkCapacity);
		PacketWriter packetWriter = new PacketWriter(outputFile);

		Packet[] packets = new Packet[3];
		PacketReader[] packetReaders = new PacketReader[3];
		packetReaders[0] = attackPacketReader;
        packetReaders[1] = fullRealPacketReader;
        packetReaders[2] = realPacketReader;
		
        // read the first packet
		for (int i = 0; i < 3; i++) {
		    packets[i] = packetReaders[i].getNextPacket();
		}
		
		while (packets[0] != null 
		        || packets[1] != null
		        || packets[2] != null) {

		    Packet packet = null;
		    int packet_index = -1;
		    for (int i = 0; i < 3; i++) {
		        if (packets[i] != null) {
		            if (packet == null || packet.time > packets[i].time) {
		                packet = packets[i];
		                packet_index = i;
		            }
		        }
		    }
		    
		    // read new packet for next round
		    packets[packet_index] = packetReaders[packet_index].getNextPacket();
		    
		    // deduplicate the flow Ids and save the flowId into flowSet
		    if (packet_index == 0) {
		        attackFlowSet.add(packet.flowId);
		    } else if (packet_index == 1) {
                packet.flowId = new FlowId(numOfLargeFlows 
                        + packet.flowId.getIntegerValue());
		    } else if (packet_index == 2) {
		        
		        if (!originalUnderUseFlowSet.contains(packet.flowId)) {
		            // skip packet of flows which are 
		            // not selected as one of numOfUnderUseRealFlows
		            continue;
		        }
		        
		        packet.flowId = new FlowId(numOfLargeFlows 
                        + numOfFullRealFlows + packet.flowId.getIntegerValue());
		    }
		    
            gatewayRouter.processPacket(packet);

			// output adjusted packets in the queue
			Packet adjustedPacket;
			while ((adjustedPacket = gatewayRouter.getNextPacket()) != null) {
				if (adjustedPacket.time > timeInterval) {
					break;
				}

                if (adjustedPacket.flowId.getIntegerValue() > numOfLargeFlows
                        + numOfFullRealFlows) {
                    // if the packet belongs to under-use flows
                    underUseRealFlowSet.add(adjustedPacket.flowId);
                    underUseTrafficVolume += adjustedPacket.size;
                } else if (adjustedPacket.flowId
                        .getIntegerValue() > numOfLargeFlows) {
                    // if the packet belongs to full-use flows
                    fullRealFlowSet.add(adjustedPacket.flowId);
                    fullRealTrafficVolume += adjustedPacket.size;
                }
				
				packetWriter.writePacket(adjustedPacket);
			}

			if (adjustedPacket != null && adjustedPacket.time > timeInterval) {
				break;
			}
			
		}
		
        if (underUseRealFlowSet.size() != numOfUnderUseRealFlows) {
            System.out.println("Warning: the number of under-use flows ("
                    + underUseRealFlowSet.size()
                    + ") is not equal to the number we set ("
                    + numOfUnderUseRealFlows + "). ");
        }
        
        if (fullRealFlowSet.size() != numOfFullRealFlows) {
            System.out.println("Warning: the number of full-use flows ("
                    + fullRealFlowSet.size()
                    + ") is not equal to the number we set ("
                    + numOfFullRealFlows + "). ");
        }


		attackPacketReader.close();
		realPacketReader.close();
		fullRealPacketReader.close();
		packetWriter.close();
		attackFlowGenerator.getOutputFile().delete();
	}

	@Override
	public void deleteOutputFile() {
	    if (outputFile != null && outputFile.exists()) {
	        outputFile.delete();
	    }
	    
	    deleteTmpFiles();
	}
	
	public void deleteTmpFiles() {
	    if (attackFlowGenerator.outputFile != null 
                && attackFlowGenerator.outputFile.exists()) {
            attackFlowGenerator.outputFile.delete();
        }
        
        if (legitimateFlowGenerator.outputFile != null 
                && legitimateFlowGenerator.outputFile.exists()) {
            legitimateFlowGenerator.outputFile.delete();
        }
	}
	
	@Override
	public void logConfig(Logger logger) throws IOException {
		super.logConfig(logger);
		logger.logConfigMsg("Real Traffic File: " + realTrafficFlowGenerator.getOutputFile() + "\n");
		logger.logConfigMsg("Number of under-use real flows: " + numOfUnderUseRealFlows + "\n");
		logger.logConfigMsg("Number of full-use real flows: " + numOfFullRealFlows + "\n");
		logger.logConfigMsg("Per-flow reservation bandwidth: " + perFlowReservation + "Byte/sec \n");
		logger.logConfigMsg("Full-use real flow packet size: " + fullRealFlowPacketSize + "Byte\n");
	}

	@Override
	public boolean isLargeFlow(FlowId flowId) {
		return attackFlowGenerator.isLargeFlow(flowId);
	}

	@Override
	public boolean isSmallFlow(FlowId flowId) {
		return ! ( attackFlowGenerator.isLargeFlow(flowId)
		        || attackFlowGenerator.isBurstFlow(flowId) );
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
	
	public Integer getNumOfAttFlows() throws Exception {
	    return attackFlowGenerator.getNumOfFlows();
	}
	
	public Integer getNumOfUnderUseRealFlows() throws Exception {
	    return numOfUnderUseRealFlows;
	}
	
	public Long getUnderUseRealTrafficVolume() {
	    return underUseTrafficVolume;
	}
	
	public Integer getNumOfFullRealFlows() throws Exception {
        return legitimateFlowGenerator.getNumOfFlows();
    }
    
    public Long getFullRealTrafficVolume() throws Exception {
        return fullRealTrafficVolume;
    }
	
	@Override
	public Integer getNumOfFlows() throws Exception {
	    return numOfUnderUseRealFlows 
	            + attackFlowGenerator.getNumOfFlows()
	            + legitimateFlowGenerator.getNumOfFlows();
	}
	
	public Integer getAveRealTrafficRate() throws Exception {
	    return realTrafficFlowGenerator.getAveRealTrafficRate();
	}
	
	public Integer getPerFlowReservation() {
	    return perFlowReservation;
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

    @Override
    public double getStartTimeOfLargeFlow(FlowId flowId) {
        return attackFlowGenerator.getStartTimeOfLargeFlow(flowId);
    }
    
    public Set<FlowId> getAttackFlowIdSet() {
        return attackFlowSet;
    }
    
    public Set<FlowId> getFullRealFlowIdSet() {
        return fullRealFlowSet;
    }
    
    public Set<FlowId> getUnderUseRealFlowIdSet() {
        return underUseRealFlowSet;
    }
    
}
