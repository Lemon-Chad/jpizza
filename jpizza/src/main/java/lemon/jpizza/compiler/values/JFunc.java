package lemon.jpizza.compiler.values;

import lemon.jpizza.compiler.Chunk;

import java.io.Serializable;

public class JFunc implements Serializable {
    public Value obj;
    public int arity;
    public Chunk chunk;
    public String name;

    public JFunc(String source) {
        arity = 0;
        name = "";
        chunk = new Chunk(source);
    }

    public String toString() {
        return "<function-" + name + ">";
    }

}
