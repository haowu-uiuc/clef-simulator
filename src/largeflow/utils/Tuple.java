package largeflow.utils;

public class Tuple<T, V> {
    public T first;
    public V second;
    
    public Tuple(T first, V second) {
        this.first = first;
        this.second = second;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o.getClass() != this.getClass()) {
            return false;
        }
        
        if (o == this) {
            return true;
        }
        
        @SuppressWarnings("unchecked")
        Tuple<T, V> t = (Tuple<T, V>) o;
        if (t.first.equals(first) && t.second.equals(second)) {
            return true;
        }
            
        return false;
    }
    
    @Override
    public String toString() {
        return "(" + first + "," + second + ")";
    }
}
