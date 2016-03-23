package largeflow.datatype;

@Deprecated
public class StringFlowId implements Comparable<StringFlowId>{

	private String value;
	private boolean isVirtualFlowId;
	
	public static Integer MAX_VALUE = Integer.MAX_VALUE;
	
	public StringFlowId(String value) {
		this.value = new String(value);
		isVirtualFlowId = false;
	}
	
	public StringFlowId(Integer valueInt) {
		this.value = valueInt.toString();
		isVirtualFlowId = false;
	}
	
	public StringFlowId(String value, boolean isVirtualFlowId) {
		this.value = value;
		this.isVirtualFlowId = isVirtualFlowId;
	}
	
	public StringFlowId(Integer value, boolean isVirtualFlowId) {
		this.value = value.toString();
		this.isVirtualFlowId = isVirtualFlowId;
	}
	
	public void set(String value) {
		this.value = value;
	}
	
	public void setAsVirtualFlowId() {
		isVirtualFlowId = true;
	}
	
	public void setAsNotVirtualFlowId() {
		isVirtualFlowId = false;
	}
	
	public String getStringValue() {
		return value;
	}
	
	public Integer getIntegerValue() {
		return Integer.valueOf(value);
	}
	
	public boolean isVirtualFlowId() {
		return isVirtualFlowId;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		
		if (!(o instanceof StringFlowId)) {
			return false;
		}
		
		StringFlowId f = (StringFlowId) o;
		if (f.isVirtualFlowId() != this.isVirtualFlowId()) {
			return false;
		}
		
		if (!f.getStringValue().equals(this.getStringValue())) {
			return false;
		}
			
		return true;
	}

	@Override
	public int compareTo(StringFlowId fid) {
		if (this.equals(fid)) {
			return 0;
		} else if (this.getStringValue().equals(fid.getStringValue())) {
			if (this.isVirtualFlowId()) {
				return -1;
			} else {
				return 1;
			}
		} else if (this.getStringValue().compareTo(fid.getStringValue()) < 0) {
			return -1;
		} else {
			return 1;
		}
	}
	
	@Override
	public int hashCode() {
		if (isVirtualFlowId) {
			return (value.hashCode() << 1)  + 1;
		} else {
			return value.hashCode() << 1;
		}
	}
	
	@Override
	public String toString() {
		if (isVirtualFlowId) {
			return "v" + value;
		}
		
		return value;
	}
}
