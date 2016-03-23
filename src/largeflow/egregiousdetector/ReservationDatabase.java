package largeflow.egregiousdetector;

import largeflow.datatype.FlowId;

public interface ReservationDatabase {

	public Integer getReservation(FlowId flowId);
	
}
