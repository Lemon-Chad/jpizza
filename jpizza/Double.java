package lemon.jpizza;

public class Double<T, X> {
    public T a;
    public X b;
    public Double(T a, X b) {
        this.a = a;
        this.b = b;
    }

    public String toString() {
        return String.format("%s, %s", a, b);
    }

}
