package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JClosure;

public class BoundMethod {
    public final JClosure closure;
    public final Value receiver;

    public BoundMethod(JClosure closure, Value receiver) {
        this.closure = closure;
        this.receiver = receiver;
    }

    public String toString() {
        return closure.toString();
    }
}
