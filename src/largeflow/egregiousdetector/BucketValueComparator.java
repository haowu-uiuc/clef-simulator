package largeflow.egregiousdetector;

import java.util.Comparator;

/**
 * make the priority queue to put the larger one in the front of smaller one.
 * @author HaoWu
 *
 */
public class BucketValueComparator implements Comparator<Bucket> {

	@Override
	public int compare(Bucket bucket1, Bucket bucket2) {
		if (bucket1.getValue() < bucket2.getValue()) {
			return 1;
		} else if (bucket1.getValue() > bucket2.getValue()) {
			return -1;
		}
		return 0;
	}

}
