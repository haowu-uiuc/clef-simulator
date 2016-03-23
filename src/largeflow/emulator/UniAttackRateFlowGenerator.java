package largeflow.emulator;

// flow generator that only generates attack flows with the same attack rate
public abstract class UniAttackRateFlowGenerator extends AttackFlowGenerator {

	public UniAttackRateFlowGenerator() {
		super();
	}

	public UniAttackRateFlowGenerator(Integer linkCapacity,
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

	public UniAttackRateFlowGenerator(Integer linkCapacity,
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

	public UniAttackRateFlowGenerator(Integer linkCapacity,
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

	public UniAttackRateFlowGenerator(Integer linkCapacity,
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

	/**
	 * attack flow rate is large flow rate here
	 */
	public void setAttackRate(Integer attackFlowRate) {
		this.largeFlowRate = attackFlowRate;
	}
	
	public Integer getAttackRate() {
	    return largeFlowRate;
	}
}
