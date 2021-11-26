package lemon.jpizza.compiler.values.functions;

import lemon.jpizza.compiler.values.Var;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JClosure implements Serializable {
    public final JFunc function;

    public final List<Var> upvalues;
    public final int upvalueCount;

    public JClosure(JFunc function) {
        this.function = function;
        this.upvalueCount = function.upvalueCount;
        this.upvalues = new ArrayList<>();
        for (int i = 0; i < upvalueCount; i++) {
            upvalues.add(null);
        }
    }

    public void asMethod(boolean isStatic, boolean isPrivate, String owner) {
        function.isStatic = isStatic;
        function.isPrivate = isPrivate;
        function.owner = owner;
    }

    public String toString() {
        return function.toString();
    }
}
