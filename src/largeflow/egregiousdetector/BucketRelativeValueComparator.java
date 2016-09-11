package largeflow.egregiousdetector;

import java.util.Comparator;

/**
 * make the priority queue to put the larger one in the front of smaller one.
 * @author HaoWu
 *
 */
public class BucketRelativeValueComparator implements Comparator<Bucket> {

	@Override
	public int compare(Bucket bucket1, Bucket bucket2) {
		double relValue1 = (double) bucket1.getValue()
		        / (double) bucket1.getReservation();
        double relValue2 = (double) bucket1.getValue()
                / (double) bucket2.getReservation();
		        
	    if (relValue1 < relValue2) {
			return 1;
		} else if (relValue1 > relValue2) {
			return -1;
		}
		return 0;
	}

}
