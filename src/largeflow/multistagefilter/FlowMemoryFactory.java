package largeflow.multistagefilter;

public class FlowMemoryFactory {

    private int threshold;
    private int drainRate;
    private int linkCapacity;
    
    public FlowMemoryFactory(int threshold, int drainRate, int linkCapacity) {
        this.threshold = threshold;
        this.drainRate = drainRate;
        this.linkCapacity = linkCapacity;
    }
    
    public FlowMemory createFlowMemory(int size) {
        return new FlowMemory(size, threshold, drainRate, linkCapacity);
    }
    
}
