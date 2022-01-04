package lemon.jpizza.compiler.values.functions;

import lemon.jpizza.compiler.Chunk;
import lemon.jpizza.compiler.ChunkCode;
import lemon.jpizza.compiler.values.Value;
import org.checkerframework.checker.units.qual.A;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JFunc {
    public int arity;
    public int genericArity;
    public int totarity;
    public Chunk chunk;
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

    public int[] dump() {
        List<Integer> list = new ArrayList<>(List.of(ChunkCode.Func, arity, genericArity, totarity));
        if (name != null)
            Value.addAllString(list, name);
        else
            list.add(0);

        if (this.returnType != null) {
            list.add(ChunkCode.Type);
            list.add(returnType.size());
            for (String s : returnType)
                Value.addAllString(list, s);
        }
        else {
            list.add(0);
        }

        list.add(genericSlots.size());
        list.addAll(genericSlots);

        list.add(upvalueCount);

        list.add(async ? 1 : 0);
        list.add(catcher ? 1 : 0);

        if (args != null)
            Value.addAllString(list, args);
        else
            list.add(0);

        if (kwargs != null)
            Value.addAllString(list, kwargs);
        else
            list.add(0);

        int[] chunk = this.chunk.dump();
        for (int i : chunk)
            list.add(i);

        return list.stream().mapToInt(i -> i).toArray();
    }

    public byte[] dumpBytes() {
        int[] dump = dump();
        byte[] bytes = new byte[dump.length * 4];
        for (int i = 0; i < dump.length; i++) {
            int v = dump[i];
            bytes[i * 4    ] = (byte) (v >>> 24);
            bytes[i * 4 + 1] = (byte) (v >>> 16);
            bytes[i * 4 + 2] = (byte) (v >>>  8);
            bytes[i * 4 + 3] = (byte) (v       );
        }
        return bytes;
    }
}
