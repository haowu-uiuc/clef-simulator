package largeflow.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class RandomHashFunction<K> implements HashFunction<K, Integer> {

	private Map<K, Integer> keyToHashCodeMap;
	//map hashcode to flow IDs. In practice, this is provided by slow path
//	private Map<Integer, List<String>> hashCodeToStrsMap;
	private Map<Integer, List<K>> hashCodeToIdsMap;
	private Integer maxHashCodeNum;
	private Random randGenerator;
	
	/**
	 * Generate hash code from 0 to maxHashCodeNum - 1
	 * @param maxHashCodeNum is max number of hash code
	 */
	public RandomHashFunction(Integer maxHashCodeNum) {
		this.maxHashCodeNum = maxHashCodeNum;
		keyToHashCodeMap = new TreeMap<>();
		hashCodeToIdsMap = new TreeMap<>();
		for (int i = 0; i < maxHashCodeNum; i++){
			hashCodeToIdsMap.put(i, new ArrayList<K>());
		}
		
		randGenerator = new Random((long) (Math.random() * Long.MAX_VALUE));
	}

	public Boolean isFirstTimeSee(K key) {
		return !keyToHashCodeMap.containsKey(key);
	}
	
	@Override
	public Integer getHashCode(K key) {
		if (keyToHashCodeMap.containsKey(key)) {
			return keyToHashCodeMap.get(key);
		}
		
		int hashCode = (int) ((randGenerator.nextDouble() - 0.000001) * maxHashCodeNum);
		keyToHashCodeMap.put(key, hashCode);
		hashCodeToIdsMap.get(hashCode).add(key);
		
		return hashCode;
	}
	
	public List<K> getKeys(Integer hashCode) {
		List<K> list = new ArrayList<>();
		for (K key : hashCodeToIdsMap.get(hashCode)) {
			list.add(key);
		}
		
		return list;
	}

}
