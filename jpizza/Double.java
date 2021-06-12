package lemon.jpizza;

public class Double {
    Object a; Object b;
    public Double(Object a, Object b) {
        this.a = a;
        this.b = b;
    }

    public Object get(int i) {
        if (i == 0)
            return a;
        else if (i == 1)
            return b;
        return null;
    }

    public String toString() {
        return String.format("%s, %s", a, b);
    }

}
