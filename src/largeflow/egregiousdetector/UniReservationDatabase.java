package largeflow.egregiousdetector;

import largeflow.datatype.FlowId;

public class UniReservationDatabase implements ReservationDatabase{

	private Integer reservationPerFlow;
	
	public UniReservationDatabase(Integer reservationPerFlow) {
		this.reservationPerFlow = reservationPerFlow;
	}
	
	@Override
	public Integer getReservation(FlowId flowId) {
		return reservationPerFlow;
	}
}
