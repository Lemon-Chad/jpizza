package lemon.jpizza.compiler.values.functions;

import lemon.jpizza.compiler.values.Var;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JClosure {
    public final JFunc function;

    public final Var[] upvalues;
    public int upvalueCount;

    public JClosure(JFunc function) {
        this.function = function;
        this.upvalueCount = function.upvalueCount;
        this.upvalues = new Var[upvalueCount];
    }

    public void asMethod(boolean isStatic, boolean isPrivate, boolean isBin, String owner) {
        function.isStatic = isStatic;
        function.isPrivate = isPrivate;
        function.isBin = isBin;
        function.owner = owner;
    }

    public String toString() {
        return function.toString();
    }
}
