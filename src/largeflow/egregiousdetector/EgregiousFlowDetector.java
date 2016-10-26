package largeflow.egregiousdetector;

import java.io.IOException;
import java.util.List;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.Logger;

public class EgregiousFlowDetector extends Detector {

	// pre-set config of the flow spec, link, and window model
	private Integer linkCapacity; // Byte / sec
	private Integer gamma; // Byte / sec. large flow rate threshold
	private Integer burst; // Byte. the max burst to alow
	private Double period; // sec. period for periodic checking

	// pre-set config of BucketListTree
	private Integer fanout; // size of bucket lists in the tree
	private Integer maxDepth; // maximum depth of the tree of bucket arrays
	private Integer numOfCounters; // max number of buckets can used in the tree
	
	private Integer numOfFlows = -1; // number of priority flows in the link 

	// calculated parameters
	private Integer numOfBranches; // number of branches to have from the root
									// level to the bottom level
	private Double minPeriod; // sec. the min

	// state of the detector
	private BucketListTree tree;
	private Double timestampOfPeriodBegin; // start of current period
	private Double timestampOfPeriodEnd; // end of current period

	// reservation information
	private ReservationDatabase resDb; // the system to query the reservation
										// for each bandwidth
	private boolean debug = false;
	
	// split the bucket by {value} / {amount of reservation}
	private boolean splitByRelativeValue = false;

	public EgregiousFlowDetector(String detectorName,
			Integer gamma,
			Integer burst,
			Double period,
			Integer linkCapacity,
			Integer numOfCounters) throws Exception {
		super(detectorName);

		this.linkCapacity = linkCapacity;
		this.numOfCounters = numOfCounters;
		this.gamma = gamma;
		this.burst = burst;
		this.period = period;

		numOfFlows = linkCapacity / gamma;    // default value
		
		minPeriod = calculateMinPeriod(burst, gamma);
		if (period <= minPeriod) {
			throw new Exception("Period is too small to tolerate the burst.");
		}

		// we use a simple setting: every flow has the same reservation = gamma.
		resDb = new UniReservationDatabase(gamma);

		optimizeConfig(numOfCounters);

		// init states
		timestampOfPeriodBegin = 0.0;
		timestampOfPeriodEnd = timestampOfPeriodBegin + period;
		initTree();
	}
	
	private void initTree() throws Exception {
	    if (numOfBranches < 1) {
            throw new Exception("Number of branches is below 1. Need more counters");
        }
	    
	    tree = new BucketListTree(maxDepth,
                fanout,
                numOfBranches,
                burst,
                resDb);
	    if (splitByRelativeValue) {
	        tree.splitByRelativeValue();
	    }
	}

	@Override
	public void reset() {
		super.reset();
		tree.reset();
		timestampOfPeriodBegin = 0.0;
		timestampOfPeriodEnd = timestampOfPeriodBegin + period;
	}

	public Integer getLinkCapacity() {
		return linkCapacity;
	}

	public Integer getGamma() {
		return gamma;
	}

	public Integer getBurst() {
		return burst;
	}

	public Double getPeriod() {
		return period;
	}

	public Integer getFanout() {
		return fanout;
	}

	public Integer getMaxDepth() {
		return maxDepth;
	}

	public double getTwinEFDHighPeriod() {
	    double n = (double)linkCapacity / (double)gamma;
	    double m = (double)numOfCounters;
	    double d = (double)maxDepth;
	    double a05 = Math.sqrt(2 * n / m * Math.log(n));
	    double gamma_h = n / (2 * m + 1); // EARDet has twice number of counters
	    double highPeriod = 2 * d * gamma_h / a05 * period;
	    return highPeriod;
	}
	
	public Integer getNumOfCounters() {
		return numOfCounters;
	}

	public Integer getNumOfBranches() {
		return numOfBranches;
	}
	
	public void splitBucketByRelativeValue() {
	    splitByRelativeValue = true;
	    tree.splitByRelativeValue();
	}

	@Override
	public boolean processPacket(Packet packet) throws Exception {
		if (packet.time < timestampOfPeriodBegin) {
			throw new Exception("The timing of packet is incorrect!"
					+ " Time of packet is smaller than current time");
		}

		if (blackList.containsKey(packet.flowId)) {
			// filter out caught flows
			return false;
		}

		// TODO: this works, but not efficient in simulation
		// Because if the packets are sparse in time at some time,
		// we still split the tree without adding any packet.
		while (packet.time >= timestampOfPeriodEnd) {
		 // we check buckets in current level
            // if there is a bucket occupied by one flow 
            // and it violates the flow spec, then it is large flow
            // and put large flows into blacklist
            List<FlowId> largeFlows = tree.checkBottomBuckets(period);

            // add these large flows into blacklist
            for (FlowId flowId : largeFlows) {
                blackList.put(flowId, timestampOfPeriodEnd);
            }

			if (!tree.splitBuckets()) {
				// if we cannot split bucket => tree reached its max depth
				tree.reset();
			}

			timestampOfPeriodBegin = timestampOfPeriodEnd;
			timestampOfPeriodEnd = timestampOfPeriodBegin + period;
		}

		tree.processPacket(packet);

		return true;
	}

	@Override
	public void setNumOfCounters(Integer numOfCounters) throws Exception {
		super.reset();
		this.numOfCounters = numOfCounters;
		optimizeConfig(numOfCounters);

		if (numOfBranches < 1) {
			throw new Exception("Number of branches is below 1. Need more counters");
		}
		
		// init state
		timestampOfPeriodBegin = 0.0;
		timestampOfPeriodEnd = timestampOfPeriodBegin + period;
		tree = new BucketListTree(maxDepth,
				fanout,
				numOfBranches,
				burst,
				resDb);
		if (splitByRelativeValue) {
		    tree.splitByRelativeValue();
		}
	}

	@Override
	public void logConfig(Logger logger) throws IOException {
		super.logConfig(logger);
		logger.logConfigMsg("Large Flow Rate Threshold: " + getGamma()
				+ " Byte / sec\n");
		logger.logConfigMsg("Burst Allowed: " + getBurst() + " Byte\n");
		logger.logConfigMsg("Link Capacity: " + getLinkCapacity()
				+ " Byte / sec\n");
		logger.logConfigMsg("Period: " + getPeriod() + " sec\n");
		logger.logConfigMsg("Num of Counter: " + getNumOfCounters() + "\n");
		logger.logConfigMsg("Fanout(Size) of each Bucketlist: " + getFanout()
				+ "\n");
		logger.logConfigMsg("Max Depth of Bucketlist Tree: " + getMaxDepth()
				+ "\n");
		logger.logConfigMsg("Number of Branches in the BucketList Tree: "
				+ getNumOfBranches() + "\n");
	}

	public void setEstimatedNumOfFlows(int numOfFlows) throws Exception {
	    this.numOfFlows = numOfFlows;
	    initTree();
	}
	
	private Integer estimateNumOfFlows() throws Exception {
		// assume the reservation for all flows is gamma (large flow rate)
		// so there are supposed to be (linkCapacity / gamma) flows.
		// TODO: modify this
	    if (numOfFlows <= 0) {
	        throw new Exception("Num of estimated flows has not been set!");
	    }
//		return linkCapacity / gamma;
	    return numOfFlows;
	}

	private Double calculateMinPeriod(Integer burst,
			Integer gamma) {
		// if the time period is less than this minPeriod,
		// the flow is going to be caught even it just sent allowed bursts.
		return (double) burst / (double) gamma;
	}

	private void optimizeConfig(Integer numOfCounters) throws Exception {
        int optf = -1;
        int optd = -1;
        int optk = -1;
        int N = estimateNumOfFlows();
        double maxOptTarget = 0.0;
        for (int f = numOfCounters; f <= numOfCounters; f++) {
//        for (int f = 2; f <= numOfCounters; f++) {
            int k = calculateNumOfBranches(numOfCounters, f);
            int d = calculateMaxDepth(N, f, k);

            if (k <= 0 || k > numOfCounters) {
                continue;
            }

            double optTarget = (double) k / (double) d;
            if (debug) {
                System.out.println("optTarget = " + optTarget + ", f = " + f +
                    ", d = " + d + ", k = " + k);
            }
            if (optTarget > maxOptTarget) {
                optf = f;
                optd = d;
                optk = k;
                maxOptTarget = optTarget;
            } else if (optTarget == maxOptTarget && f > optf) {
                optf = f;
                optd = d;
                optk = k;
            }
        }
        
        // if number of counters is not enough
        if (optk <= 0) {
            optf = 2;
            optd = numOfCounters / optf;
            optk = 1;
        }
        
        fanout = optf;
        maxDepth = optd;
        numOfBranches = optk;
        if (debug) {
            System.out.println("N = " + N + ", f = " + optf + ", d = " + optd + ", k = " + optk);
        }
    }
	
    private Integer calculateMaxDepth(Integer numOfFlows,
            Integer fanout,
            Integer numOfBranches) {
        return (int) (Math.log((double) numOfFlows / (double) numOfBranches)
                / Math.log((double) fanout) * 1.2) + 1;
    }

    private Integer calculateNumOfBranches(Integer numOfCounters,
            Integer fanout) {
        return numOfCounters / fanout;
    }
	
	public void enableDebug() {
	    debug = true;
	}
	
	public void disableDebug() {
	    debug = false;
	}

}
