package lemon.jpizza.compiler.values;

public class Upvalue {
    public Value location;

    public Upvalue(Value location) {
        this.location = location;
    }

    public String toString() {
        return "Upvalue(" + location + ")";
    }
}
