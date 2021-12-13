package lemon.jpizza.compiler.values.functions;

import lemon.jpizza.compiler.Chunk;
import lemon.jpizza.compiler.values.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JFunc implements Serializable {
    public int arity;
    public int genericArity;
    public int totarity;
    public final Chunk chunk;
    public String name;
    public List<String> returnType;

    public List<Integer> genericSlots;

    public String args;
    public String kwargs;

    public boolean catcher;

    // Only if the function is a method
    public boolean isPrivate;
    public boolean isStatic;
    public boolean isBin;
    public String owner;

    public int upvalueCount;
    public boolean async;
    public List<Value> defaults;
    public int defaultCount;

    public JFunc(String source) {
        arity = 0;
        totarity = 0;
        genericArity = 0;
        defaultCount = 0;
        name = "";
        chunk = new Chunk(source);

        upvalueCount = 0;
        genericSlots = new ArrayList<>();
    }

    public String toString() {
        if (owner != null)
            return "<" + owner + "-method-" + name + ">";
        return "<function-" + name + ">";
    }

    public JFunc copy() {
        JFunc copy = new JFunc(chunk.source());

        copy.chunk.constants(chunk.constants().copy());
        copy.chunk.codeArray = chunk.codeArray;
        copy.chunk.positions = chunk.positions;

        copy.arity = arity;
        copy.defaults = defaults;
        copy.totarity = totarity;
        copy.genericArity = genericArity;
        copy.name = name;
        copy.returnType = returnType;
        copy.isPrivate = isPrivate;
        copy.isStatic = isStatic;
        copy.isBin = isBin;
        copy.owner = owner;
        copy.upvalueCount = upvalueCount;
        copy.async = async;
        copy.args = args;
        copy.kwargs = kwargs;
        copy.catcher = catcher;
        copy.genericSlots = genericSlots;
        return copy;
    }

}
