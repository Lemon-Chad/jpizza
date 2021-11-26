package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.values.Value;

public class ClassAttr {
    public Value val;
    public String type;
    public boolean isStatic;
    public boolean isPrivate;

    public ClassAttr(Value val, String type, boolean isStatic, boolean isPrivate) {
        this.val = val;
        this.type = type;
        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
    }

    public void set(Value val) {
        this.val = val;
    }
}
