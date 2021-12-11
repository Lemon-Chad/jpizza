package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.values.Value;

public class ClassAttr {
    public Value val;
    public final String type;
    public final boolean isStatic;
    public final boolean isPrivate;

    public ClassAttr(Value val, String type, boolean isStatic, boolean isPrivate) {
        this.val = val;
        this.type = type;
        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
    }

    public ClassAttr(Value val, String type) {
        this(val, type, false, false);
    }

    public ClassAttr(Value val) {
        this(val, "any");
    }

    public void set(Value val) {
        this.val = val;
    }
}
