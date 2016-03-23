package largeflow.multistagefilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import largeflow.datatype.FlowId;
import largeflow.emulator.Detector;
import largeflow.emulator.Logger;
import largeflow.utils.RandomHashFunction;

abstract class MultistageFilter extends Detector {

	// parameters
	protected Integer numOfStages;
	protected Integer sizeOfStage;
	protected Integer numOfCounters;
	protected Integer linkCapacity;
	protected List<RandomHashFunction<FlowId>> hashFuncs;

	// states
	protected List<List<Bucket>> stages;

	public MultistageFilter(String detectorName,
			Integer numOfStages,
			Integer sizeOfStage,
			Integer linkCapacity) {
		super(detectorName);

		this.numOfStages = numOfStages;
		this.sizeOfStage = sizeOfStage;
		this.numOfCounters = numOfStages * sizeOfStage;
		this.linkCapacity = linkCapacity;

		initHashFuncs();
	}

	private void initHashFuncs() {
		hashFuncs = new ArrayList<>(numOfStages);
		for (int i = 0; i < numOfStages; i++) {
			hashFuncs.add(new RandomHashFunction<>(sizeOfStage));
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		initHashFuncs();
	}

	@Override
	public void setNumOfCounters(Integer numOfCounters) throws Exception {
		this.numOfCounters = numOfCounters;
		sizeOfStage = numOfCounters / numOfStages;
		reset();
	}
		
	public Integer getNumOfStages() {
		return numOfStages;
	}
	
	public Integer getSizeOfStage() {
		return sizeOfStage;
	}
	
	public Integer getNumOfCounters() {
		return numOfCounters;
	}
	
	public Integer getLinkCapacity() {
		return linkCapacity;
	}
	
	@Override
	public void logConfig(Logger logger) throws IOException {
		super.logConfig(logger);
		logger.logConfigMsg("Number of stages: " + getNumOfStages() + "\n");
		logger.logConfigMsg("Size of each stage: " + getSizeOfStage() + " \n");
		logger.logConfigMsg("Num of Counter: " + getNumOfCounters() + "\n");
		logger.logConfigMsg("Link capacity: " + getLinkCapacity() + " Byte/s \n");
	}

}
