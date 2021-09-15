package lemon.jpizza;

public class Pair3<T, U, V> {
    public T first;
    public U second;
    public V third;
    public Pair3(T a, U b, V c) {
        first = a;
        second = b;
        third = c;
    }

    public String toString() {
        return String.format("%s, %s, %s", first, second, third);
    }

}
