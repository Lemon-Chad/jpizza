package lemon.jpizza.compiler.values;

public class Var {
    public final String type;
    public Value val;
    public final boolean constant;
    public final int min;
    public final int max;

    public Var(String type, Value val, boolean constant) {
        this.type = type;
        this.val = val;
        this.constant = constant;
        min = Integer.MIN_VALUE;
        max = Integer.MAX_VALUE;
    }

    public Var(String type, Value val, boolean constant, int min, int max) {
        this.type = type;
        this.val = val;
        this.constant = constant;
        this.min = min;
        this.max = max;
    }

    public void val(Value v) {
        val = v;
    }

    public String toString() {
        return val + ":" + type;
    }

    public String toSafeString() {
        return val.toSafeString() + ":" + type;
    }
}
