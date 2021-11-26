package lemon.jpizza.compiler.values.functions;

import lemon.jpizza.compiler.Chunk;

import java.io.Serializable;
import java.util.List;

public class JFunc implements Serializable {
    public int arity;
    public final Chunk chunk;
    public String name;
    public List<String> returnType;

    // Only if the function is a method
    public boolean isPrivate;
    public boolean isStatic;
    public boolean isBin;
    public String owner;

    public int upvalueCount;

    public JFunc(String source) {
        arity = 0;
        name = "";
        chunk = new Chunk(source);

        upvalueCount = 0;
    }

    public String toString() {
        if (owner != null)
            return "<" + owner + "-method-" + name + ">";
        return "<function-" + name + ">";
    }

}
