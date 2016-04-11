package largeflow.emulator;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import largeflow.datatype.Packet;

/**
 * Using RED algorithm to adjust the timing of traffic from link A to link B,
 * the link capacity of two links could be different. If capacity_B <
 * capacity_A, there could be packet drop when some burst come from link A.
 * 
 * @author HaoWu
 *
 */
public class QueueDiscipline {

	private double queueWeight; // queue weight
	private int min_th; // minimum threshold for queue length
	private int max_th; // maximum threshold for queue length
	private double max_p; // maximum value for proabability of dropping packet
							// (if max_p = 0,
							// it will not drop packet until queue reaches full)
	private int maxPacketSize; // max packet size
	private int minPacketSize; // min packet size
	private int linkCapacity; // the link capacity
	private double avgQueueSize; // average queue size
	private double q_time; // start of the queue idle time
	private int count; // packets since last marked pkt (marking is dropping
						// here)
	private double lastPacketTimeEnd; // the time of the end of transmission of
										// the last packet

	private int rate; // num of small packet per second in idle link

	private Queue<Packet> inputQueue;
	private Queue<Packet> outputQueue;
	
	private Random randGenerator;

	public QueueDiscipline(double queueWeight, int min_th, int max_th,
			double max_p, int linkCapacity, int minPacketSize, int maxPacketSize) {
		this.queueWeight = queueWeight;
		this.min_th = min_th;
		this.max_th = max_th;
		this.max_p = max_p;
		this.linkCapacity = linkCapacity;
		this.maxPacketSize = maxPacketSize;
		this.minPacketSize = minPacketSize;

		rate = linkCapacity / minPacketSize;
		avgQueueSize = 0;
		q_time = 0;
		count = -1;
		lastPacketTimeEnd = 0;

		inputQueue = new LinkedList<>();
		outputQueue = new LinkedList<>();
		
		randGenerator = new Random(System.currentTimeMillis());
	}

	public QueueDiscipline(int linkCapacity) {
		this.queueWeight = 0.002;
		this.min_th = 5;
		this.max_th = 20;
		this.max_p = 0.06;
		this.linkCapacity = linkCapacity;
		this.maxPacketSize = 1518;
		this.minPacketSize = 60;

		rate = linkCapacity / minPacketSize;
		avgQueueSize = 0;
		q_time = 0;
		count = -1;
		lastPacketTimeEnd = 0;

		inputQueue = new LinkedList<>();
		outputQueue = new LinkedList<>();
		
		randGenerator = new Random(System.currentTimeMillis());
	}

	public void reset() {
		avgQueueSize = 0;
		q_time = 0;
		count = -1;
		lastPacketTimeEnd = 0;

		inputQueue = new LinkedList<>();
		outputQueue = new LinkedList<>();
		
		randGenerator = new Random(System.currentTimeMillis()); // reset random seed
	}

	/**
	 * process an incoming packet from input port, and run RED to tell whether
	 * we should flush packets in buffer to output queue. Also based on RED, we
	 * decide whether to enqueue this new packet.
	 * 
	 * @param packet
	 */
	public void processPacket(Packet packet) {
		boolean isEnqueuable = true;
		double currentTime = packet.time;

		// dequeue the pkts from inputQueue
		while (!inputQueue.isEmpty()) {
			Packet nextPacket = inputQueue.peek();
			Packet adjustedNextPacket = new Packet(nextPacket.flowId,
					nextPacket.size, nextPacket.time);
			if (lastPacketTimeEnd > nextPacket.time) {
				adjustedNextPacket.time = lastPacketTimeEnd;
			}

			double adjustedNextPacketTimeEnd = adjustedNextPacket.time
					+ (double) adjustedNextPacket.size / (double) linkCapacity;

			if (adjustedNextPacketTimeEnd > currentTime + 0.000001) {
				break;
			}

			inputQueue.poll();
			outputQueue.add(adjustedNextPacket);
			lastPacketTimeEnd = adjustedNextPacketTimeEnd;
			q_time = lastPacketTimeEnd;
		}

		// calculate new average queue size
		if (!inputQueue.isEmpty()) {
			avgQueueSize = (1 - queueWeight) * avgQueueSize + queueWeight
					* inputQueue.size();
		} else {
			int m = (int) (rate * (packet.time - q_time));
			for (int i = 0; i < m; i++) {
				avgQueueSize *= (1 - queueWeight);
			}
		}

		// calculate the p_a
		if (avgQueueSize >= min_th && avgQueueSize < max_th) {
			count++;
			double p_b = max_p * (avgQueueSize - min_th) / (max_th - min_th);
			p_b = p_b * packet.size / maxPacketSize;

			double p_a;
			if ((1 - count * p_b) > 0) {
				p_a = p_b / (1 - count * p_b);
			} else {
				p_a = 1;
			}

			if (p_a > 1) {
				p_a = 1;
			}

			// mark the packet with probability of p_a
			double random = randGenerator.nextDouble();
			if (random < p_a) {
				isEnqueuable = false;
				count = 0;
			}
		} else if (avgQueueSize >= max_th) {
			isEnqueuable = false;
			count = 0;
		} else {
			count = -1;
		}

		if (isEnqueuable == true) {
			inputQueue.add(packet);
		}
		
	}

	/**
	 * at the end of simulation, we run this to flush the buffer to put all
	 * packets in the input queue to output queue.
	 */
	public void processEnd() {

		// dequeue the pkts from inputQueue
		while (!inputQueue.isEmpty()) {
			Packet nextPacket = inputQueue.peek();
			Packet adjustedNextPacket = new Packet(nextPacket.flowId,
					nextPacket.size, nextPacket.time);
			if (lastPacketTimeEnd > nextPacket.time) {
				adjustedNextPacket.time = lastPacketTimeEnd;
			}

			double adjustedNextPacketTimeEnd = adjustedNextPacket.time
					+ (double) adjustedNextPacket.size / (double) linkCapacity;

			inputQueue.poll();
			outputQueue.add(adjustedNextPacket);
			lastPacketTimeEnd = adjustedNextPacketTimeEnd;
		}

	}

	/**
	 * Get the next packet from output queue, if output queue is not empty.
	 * Return null if the output queue is empty.
	 * 
	 * @return
	 */
	public Packet getNextPacket() {
		if (outputQueue.isEmpty()) {
			return null;
		} else {
			return outputQueue.poll();
		}
	}

}
