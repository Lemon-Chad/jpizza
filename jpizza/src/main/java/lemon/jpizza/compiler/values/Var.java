package lemon.jpizza.compiler.values;

import java.io.Serializable;

public class Var implements Serializable {
    public String type;
    public Value val;
    public boolean constant;

    public Var(String type, Value val, boolean constant) {
        this.type = type;
        this.val = val;
        this.constant = constant;
    }

    public void val(Value v) {
        val = v;
    }

    public String toString() {
        return type + ":" + val;
    }
}
