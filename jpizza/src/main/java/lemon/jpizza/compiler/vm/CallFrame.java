package lemon.jpizza.compiler.vm;

import lemon.jpizza.compiler.values.JFunc;
import lemon.jpizza.compiler.values.Value;

public class CallFrame {
    public JFunc function;
    public int ip;
    public Value[] slots;

    public CallFrame(JFunc function, int ip, Value[] slots) {
        this.function = function;
        this.ip = ip;
        this.slots = slots;
    }
}
