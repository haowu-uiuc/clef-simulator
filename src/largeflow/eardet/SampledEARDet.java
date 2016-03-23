package largeflow.eardet;

import java.util.Map;

import largeflow.datatype.FlowId;
import largeflow.datatype.Packet;
import largeflow.emulator.Detector;
import largeflow.emulator.QueueDiscipline;

public class SampledEARDet extends Detector {

	// parameter to set
	private Integer linkCapacity;
	private Integer gamma_th; // large flow threshold
	private Double sampleRate; // sample packets with probability of sample rate
	private Integer sampledLinkCapacity;

	private EARDet eardet;
	private QueueDiscipline gatewayRouter;
	private PacketSampler packetSampler;

	/**
	 * init SampledEARDet by parameters: maximum packet size (alpha), counter threshold
	 * (beta_th), number of counters, link capacity, and large flow threshold
	 * (gamma_th).
	 * 
	 * @param detectorName
	 * @param alpha
	 * @param beta_th
	 * @param numOfCounters
	 * @param linkCapacity
	 * @param gamma_th
	 */
	public SampledEARDet(String detectorName, Integer alpha, Integer beta_th,
			Integer numOfCounters, Integer linkCapacity, Integer gamma_th) {
		super(detectorName);

		this.linkCapacity = linkCapacity;
		this.gamma_th = gamma_th;
		sampleRate = (double) gamma_th
				/ ((double) linkCapacity / (double) (numOfCounters + 1));
		sampledLinkCapacity = (int) (linkCapacity * sampleRate);

		gatewayRouter = new QueueDiscipline(sampledLinkCapacity);
		packetSampler = new PacketSampler(sampleRate);
		eardet = new EARDet(detectorName + "_subEARDet", alpha, beta_th,
				numOfCounters, sampledLinkCapacity);
	}

	public void reset() {
		eardet.reset();
		gatewayRouter.reset();
		packetSampler = new PacketSampler(sampleRate); // reset the random seed
	}

	public Integer getAlpha() {
		return eardet.getAlpha();
	}

	public Integer getBetaTh() {
		return eardet.getBetaTh();
	}

	public Integer getNumOfCounters() {
		return eardet.getNumOfCounters();
	}

	@Override
	public Map<FlowId, Double> getBlackList() {
		return eardet.getBlackList();
	}

	@Override
	public Double getBlackListTime(FlowId flowId) {
		return eardet.getBlackListTime(flowId);
	}

	/**
	 * Because of gateway router, we may not be able to process the packet now,
	 * so the boolean return does not exactly shows whether the packet is
	 * blocked or not. False: when the flow ID is already in blacklist True:
	 * otherwise.
	 * @throws Exception 
	 */
	@Override
	public boolean processPacket(Packet packet) throws Exception {
		Packet sampledPacket = packetSampler.samplePacket(packet);
		if (sampledPacket == null) {
			return true;
		}

		gatewayRouter.processPacket(sampledPacket);

		Packet adjustedPacket;
		while ((adjustedPacket = gatewayRouter.getNextPacket()) != null) {
			eardet.processPacket(adjustedPacket);
		}

		if (getBlackList().containsKey(packet.flowId)) {
			return false;
		}
		return true;
	}

	@Override
	public void setNumOfCounters(Integer numOfCounters) {
		sampleRate = (double) gamma_th
				/ ((double) linkCapacity / (double) (numOfCounters + 1));
		sampledLinkCapacity = (int) (linkCapacity * sampleRate);

		eardet.setLinkCapacity(sampledLinkCapacity);
		eardet.setNumOfCounters(numOfCounters);

		gatewayRouter = new QueueDiscipline(sampledLinkCapacity);
		packetSampler = new PacketSampler(sampleRate);
	}

}
