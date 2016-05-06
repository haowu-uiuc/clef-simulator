package largeflow.flowgenerator;

// flow generator that only generates attack flows with the same attack rate
public abstract class UniAttackRateFlowGenerator extends AttackFlowGenerator {

 // if duty cycle < period, then the large flow is burst flow
    protected Double dutyCycle = null;    // sec; duty cycle of the burst in one period
    protected Double period = null;       // sec; period of the burst
    
	public UniAttackRateFlowGenerator() {
		super();
	}

	public UniAttackRateFlowGenerator(Integer linkCapacity,
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
	}

	public UniAttackRateFlowGenerator(Integer linkCapacity,
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

	/**
	 * attack flow rate is large flow rate here
	 */
	public void setAttackRate(Integer attackFlowRate) {
		this.largeFlowRate = attackFlowRate;
	}
	
	public Integer getAttackRate() {
	    return largeFlowRate;
	}
	
    public void setDutyCycleAndPeriod(double dutyCycle, double period) {
        this.dutyCycle = dutyCycle;
        this.period = period;
        
        if (period / dutyCycle * numOfLargeFlows * largeFlowRate > linkCapacity) {
            System.out.println("Warning: the burst duration may exceed the duty cycle! "
                    + "The duty cycle is too small for this link capacity"
                    + " and average large flow rate");
        }
    }
	
}
