package lemon.jpizza.compiler.values;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JClosure implements Serializable {
    public JFunc function;

    public List<Var> upvalues;
    public int upvalueCount;

    public JClosure(JFunc function) {
        this.function = function;
        this.upvalueCount = function.upvalueCount;
        this.upvalues = new ArrayList<>();
        for (int i = 0; i < upvalueCount; i++) {
            upvalues.add(null);
        }
    }
}
