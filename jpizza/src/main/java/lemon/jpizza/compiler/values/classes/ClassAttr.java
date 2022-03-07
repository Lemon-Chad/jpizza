package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.values.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassAttr {
    public Value val;
    public final boolean isStatic;
    public final boolean isPrivate;

    public ClassAttr(Value val, boolean isStatic, boolean isPrivate) {
        this.val = val;
        this.isStatic = isStatic;
        this.isPrivate = isPrivate;
    }

    public ClassAttr(Value val) {
        this(val, false, false);
    }

    public void set(Value val) {
        this.val = val;
    }

}
