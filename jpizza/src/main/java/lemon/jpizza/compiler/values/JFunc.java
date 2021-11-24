package lemon.jpizza.compiler.values;

import lemon.jpizza.compiler.Chunk;

import java.io.Serializable;

public class JFunc implements Serializable {
    public Value obj;
    public int arity;
    public Chunk chunk;
    public String name;

    public int upvalueCount;

    public JFunc(String source) {
        arity = 0;
        name = "";
        chunk = new Chunk(source);

        upvalueCount = 0;
    }

    public String toString() {
        return "<function-" + name + ">";
    }

}
