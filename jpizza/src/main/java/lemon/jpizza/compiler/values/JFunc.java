package lemon.jpizza.compiler.values;

import lemon.jpizza.compiler.Chunk;

import java.io.Serializable;

public class JFunc implements Serializable {
    Value obj;
    int arity;
    Chunk chunk;
    String name;

    public JFunc() {
        arity = 0;
        name = "";
        chunk = new Chunk();
    }

    public String toString() {
        return "<function-" + name + ">";
    }

}
