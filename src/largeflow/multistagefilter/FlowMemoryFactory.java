package largeflow.multistagefilter;

public class FlowMemoryFactory {

    private boolean DEBUG = false;
    
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
    
    public void enableDebug() {
        DEBUG = true;
    }
    
    public void disableDebug() {
        DEBUG = false;
    }
    
    public FlowMemory createFlowMemory(int size) {
        FlowMemory fm = new FlowMemory(size, threshold, drainRate, linkCapacity);
        if (noSizeLimit) {
            fm.setNoSizeLimit();
        }
        if (DEBUG) {
            fm.enableDebug();
        }
        return fm;
    }
    
}
