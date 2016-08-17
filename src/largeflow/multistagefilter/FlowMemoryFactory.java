package largeflow.multistagefilter;

public class FlowMemoryFactory {

    private int threshold;
    private int drainRate;
    private int linkCapacity;
    private boolean noSizeLimit = false;
    
    public FlowMemoryFactory(int threshold, int drainRate, int linkCapacity) {
        this.threshold = threshold;
        this.drainRate = drainRate;
        this.linkCapacity = linkCapacity;
    }
    
    public void setNoSizeLimit() {
        noSizeLimit = true;
    }
    
    public FlowMemory createFlowMemory(int size) {
        FlowMemory fm = new FlowMemory(size, threshold, drainRate, linkCapacity);
        if (noSizeLimit) {
            fm.setNoSizeLimit();
        }
        return fm;
    }
    
}
