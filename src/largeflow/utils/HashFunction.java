package largeflow.utils;

public interface HashFunction<K, V> {

	public V getHashCode(K value);
	
}
