package largeflow.datatype;

public class FlowId implements Comparable<FlowId>{

    private Integer value;
    private boolean isVirtualFlowId;

    public static Integer MAX_VALUE = Integer.MAX_VALUE;

    public FlowId(String valueStr) {
        this.value = Integer.valueOf(valueStr);
        isVirtualFlowId = false;
    }

    public FlowId(Integer valueInt) {
        this.value = valueInt;
        isVirtualFlowId = false;
    }

    public FlowId(String valueStr,
            boolean isVirtualFlowId) {
        this.value = Integer.valueOf(valueStr);
        this.isVirtualFlowId = isVirtualFlowId;
    }

    public FlowId(Integer valueInt,
            boolean isVirtualFlowId) {
        this.value = valueInt;
        this.isVirtualFlowId = isVirtualFlowId;
    }

    public void set(String valueStr) {
        this.value = Integer.valueOf(valueStr);
    }

    public void setAsVirtualFlowId() {
        isVirtualFlowId = true;
    }

    public void setAsNotVirtualFlowId() {
        isVirtualFlowId = false;
    }

    public String getStringValue() {
        return value.toString();
    }

    public Integer getIntegerValue() {
        return value;
    }

    public boolean isVirtualFlowId() {
        return isVirtualFlowId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof FlowId)) {
            return false;
        }

        FlowId f = (FlowId) o;
        if (f.isVirtualFlowId() != this.isVirtualFlowId()) {
            return false;
        }

        if (!f.getIntegerValue().equals(this.getIntegerValue())) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(FlowId fid) {
        if (this.equals(fid)) {
            return 0;
        } else if (this.getIntegerValue().equals(fid.getIntegerValue())) {
            if (this.isVirtualFlowId()) {
                return -1;
            } else {
                return 1;
            }
        } else if (this.getIntegerValue()
                .compareTo(fid.getIntegerValue()) < 0) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public int hashCode() {
        if (isVirtualFlowId) {
            return (value.hashCode() << 1) + 1;
        } else {
            return value.hashCode() << 1;
        }
    }

    @Override
    public String toString() {
        if (isVirtualFlowId) {
            return "v" + value;
        }

        return value.toString();
    }
}
