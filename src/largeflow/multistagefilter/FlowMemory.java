package largeflow.multistagefilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;

/**
 * flow memory using leaky bucket
 * 
 * @author HaoWu
 *
 */
public class FlowMemory {

    // when noSizeLimit == true, the size of buckets is MAX_SIZE
    private static int MAX_SIZE = 100000;
    private boolean DEBUG = false;
    
    private int size; // number of buckets
    // true for no flow size limit, thus no overflow in flow memory
    // but to prevent system memory overflow, we set a MAX_SIZE for flow memory
    private boolean noSizeLimit = false;
    private int bucket_threshold; // threshold of bucket
    private int bucket_drainRate; // drain rate of bucket
    private int linkCapacity;

    // least-bucket-value eviction
    private FlowMemoryEvictionType  evictionType = FlowMemoryEvictionType.RANDOM_EVICTION;
    
    // state
    private List<LeakyBucket> buckets;
    private Queue<Integer> idleBucketIdxs;
    private Map<FlowId, Integer> mapFlowToBucketIdx;
    private Map<Integer, FlowId> mapBucketIdxToFlow;

    public FlowMemory(int size,
            int threshold, // for bucket
            int drainRate, // for bucket
            int linkCapacity) {
        this.size = size;
        this.bucket_threshold = threshold;
        this.bucket_drainRate = drainRate;
        this.linkCapacity = linkCapacity;
        mapFlowToBucketIdx = new HashMap<>(size);
        mapBucketIdxToFlow = new HashMap<>(size);
        buckets = new ArrayList<LeakyBucket>(size);
        idleBucketIdxs = new LinkedList<>();
        reset();
    }

    public void reset() {
        mapFlowToBucketIdx.clear();
        mapBucketIdxToFlow.clear();
        buckets.clear();
        idleBucketIdxs.clear();
        for (int i = 0; i < size; i++) {
            idleBucketIdxs.add(i);
            buckets.add(new LeakyBucket(bucket_threshold,
                    bucket_drainRate,
                    linkCapacity));
        }
    }

    public void setEvictionType (FlowMemoryEvictionType type) {
        if (DEBUG) {
            System.out.println("Eviction type set as " + type.toString());
        }
        evictionType = type;
    }
    
    public void setSize(int size) {
        this.size = size;
        if (noSizeLimit) {
            this.size = MAX_SIZE;
        }
        reset();
    }
    
    public void setNoSizeLimit() {
        noSizeLimit = true;
        setSize(MAX_SIZE);
    }
    
    public void unsetNoSizeLimit() {
        noSizeLimit = false;
    }
    
    /**
     * Process a packet in the flow memory. When the memory is full, the flow of
     * the packet may not be added into the flow memory
     * 
     * @return true -> the flow of the packet is detected as a large flow; false
     *         -> the flow of the packet is not detected as a large flow yet.
     */
    public boolean processPacket(Packet packet) {
        // add flow in the flow memory if it is not in the memory
        // if the memory is full, then remove existing flow from the flow memory
        // according to some rule.
        boolean isLargeFlow = false;
        Integer bucketIdx = addFlow(packet.flowId);
        LeakyBucket bucket = null;
        if (bucketIdx >= 0) {
            bucket = buckets.get(bucketIdx);
            bucket.processPacket(packet);
            isLargeFlow = bucket.check();
        }

        if (bucket != null && isLargeFlow) {
            // if the flow is judged as a large flow,
            // then, remove the flow from the flow memory and reset its bucket
            mapBucketIdxToFlow.remove(bucketIdx);
            mapFlowToBucketIdx.remove(packet.flowId);
            bucket.reset();
            // put the bucket to empty bucket queue for future use
            idleBucketIdxs.add(bucketIdx);
        }

        return isLargeFlow;
    }

    /**
     * asscosiate a flow to the flow memory
     * 
     * @return bucket index (>=0) - flow is added into the flow memory
     *         successufully -1 - flow is not added into the flow memory
     */
    public Integer addFlow(FlowId flowId) {
        if (flowIsInFlowMemory(flowId)) {
            return getBucketIndex(flowId);
        }
        
        if (numOfFlows() == MAX_SIZE) {
            // the number of flows exceeds the MAX SIZE LIMIT
            System.out.println("Warning: the number of flows exceeds the MAX_SIZE of the flow memory");
        }
        
        if (DEBUG) {
            System.out.println("INFO: Number of flows: " + numOfFlows());
        }

        if (!idleBucketIdxs.isEmpty()) {
            int bucketIdx = idleBucketIdxs.poll();
            associateFlowAndBucket(flowId, bucketIdx);
            return bucketIdx;
        } else {
            if (DEBUG) {
                System.out.println("INFO: Flow Memory is full");
            }
            // The flow is not in the flow memory yet,
            // decide a flow to remove and add this flow into flow memory,
            // or keep previous flows in the flow memory and skip this flow
            FlowId flowIdToRemove = getFlowToRemove();
            if (flowIdToRemove != null) {
                int bucketIdx = getBucketIndex(flowIdToRemove);
                // remove old entries
                deassociateFlowAndBucket(flowIdToRemove, bucketIdx);
                // add new entries
                associateFlowAndBucket(flowId, bucketIdx);
                // reset bucket
                Bucket bucket = buckets.get(bucketIdx);
                bucket.reset();
            }
        }

        return -1;
    }

    /**
     * when the flow memory is full, get the flow ID to remove from the flow
     * memory Subclass can override this method to have other way to pick up
     * flow to remove
     * 
     * @return null means no need to remove flow from memory
     */
    protected FlowId getFlowToRemove() {
        if (mapBucketIdxToFlow.size() < size) {
            return null;
        }
        
        if (evictionType == FlowMemoryEvictionType.RANDOM_EVICTION) {
            // randomly pick up a bucket and
            // return the flow associated to the bucket
            int idx = (int) ((Math.random() - 0.000001) * size);
            FlowId flowId = mapBucketIdxToFlow.get(idx);
            return flowId;
        } else if (evictionType == FlowMemoryEvictionType.LEAST_BUCKET_VALUE_EVICTION) {
            // find min-value bucket to remove
            int minValue = buckets.get(0).getValue();
            int minIndex = 0;
            for (int i = 1; i < buckets.size(); i++) {
                int value = buckets.get(i).getValue();
                if (minValue > value) {
                    minValue = value;
                    minIndex = i;
                }
            }
            FlowId flowId = mapBucketIdxToFlow.get(minIndex);
            return flowId;
        }
        
        return null;
    }

    public boolean flowIsInFlowMemory(FlowId flowId) {
        return mapFlowToBucketIdx.containsKey(flowId);
    }

    public int numOfFlows() {
        return mapFlowToBucketIdx.size();
    }
    
    public Set<FlowId> getFlows() {
        return mapFlowToBucketIdx.keySet();
    }

    private int getBucketIndex(FlowId flowId) {
        if (mapFlowToBucketIdx.containsKey(flowId)) {
            return mapFlowToBucketIdx.get(flowId);
        }
        return -1;
    }

    private void associateFlowAndBucket(FlowId flowId, int bucketIdx) {
        mapFlowToBucketIdx.put(flowId, bucketIdx);
        mapBucketIdxToFlow.put(bucketIdx, flowId);
    }

    private void deassociateFlowAndBucket(FlowId flowId, int bucketIdx) {
        mapFlowToBucketIdx.remove(flowId);
        mapBucketIdxToFlow.remove(bucketIdx);
    }
    
    public void enableDebug() {
        DEBUG = true;
    }
    
    public void disableDebug() {
        DEBUG = false;
    }
}
