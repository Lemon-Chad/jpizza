package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.values.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassAttr {
    public Value val;
    public String type;
    public final List<String> rawType;
    public final boolean isStatic;
    public final boolean isPrivate;

    public ClassAttr(Value val, String type, List<String> rawType, boolean isStatic, boolean isPrivate) {
        this.val = val;
        this.type = type;
        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
        this.rawType = rawType;
    }

    public ClassAttr(Value val, String type) {
        this(val, type, new ArrayList<>(Collections.singletonList(type)), false, false);
    }

    public ClassAttr(Value val) {
        this(val, "any");
    }

    public void set(Value val) {
        this.val = val;
    }
}
