package lemon.jpizza.compiler.vm;

import lemon.jpizza.compiler.values.JClosure;
import lemon.jpizza.compiler.values.JFunc;
import lemon.jpizza.compiler.values.Value;

public class CallFrame {
    public JClosure closure;
    public int ip;
    public int slots;

    public CallFrame(JClosure closure, int ip, int slots) {
        this.closure = closure;
        this.ip = ip;
        this.slots = slots;
    }
}
