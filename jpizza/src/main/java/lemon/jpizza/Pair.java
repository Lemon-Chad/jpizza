package lemon.jpizza;

import java.io.Serializable;

public class Pair<T, X> {
    public T a;
    public X b;
    public Pair(T a, X b) {
        this.a = a;
        this.b = b;
    }

    public String toString() {
        return String.format("%s, %s", a, b);
    }

}
