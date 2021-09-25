package lemon.jpizza;

public class Pair3<T, U, V> {
    public final T first;
    public final U second;
    public final V third;
    public Pair3(T a, U b, V c) {
        first = a;
        second = b;
        third = c;
    }

    public String toString() {
        return String.format("%s, %s, %s", first, second, third);
    }

}
