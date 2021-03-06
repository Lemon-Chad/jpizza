package lemon.jpizza.compiler.vm;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JClosure;

public class CallFrame {
    public final JClosure closure;
    public int ip;
    public int slots;
    public String returnType;
    public Value bound;
    public int memoize = 0;
    public boolean catchError = false;
    public boolean addPeek = false;

    public CallFrame(JClosure closure, int ip, int slots, String returnType) {
        this(closure, ip, slots, returnType, null);
    }

    public CallFrame(JClosure closure, int ip, int slots, String returnType, Value binding) {
        this.closure = closure;
        this.ip = ip;
        this.slots = slots;
        this.returnType = returnType;
        this.bound = binding;
    }
}
