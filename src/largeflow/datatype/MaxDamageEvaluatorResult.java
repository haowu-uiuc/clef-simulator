package largeflow.datatype;

public class MaxDamageEvaluatorResult {

	public int atkRate;		// bytes / second
	public int numOfCounter;	
	public double maxDamage;	// bytes
	
	public MaxDamageEvaluatorResult(){}
	
	public MaxDamageEvaluatorResult(int atkRate, int numOfCounter, double maxDamage){
		this.atkRate = atkRate;
		this.numOfCounter = numOfCounter;
		this.maxDamage = maxDamage;
	}
}
