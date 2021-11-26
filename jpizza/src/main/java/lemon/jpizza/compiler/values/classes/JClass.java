package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JClosure;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.compiler.vm.VMResult;

import java.util.HashMap;
import java.util.Map;

public class JClass {
    public final String name;
    public final Map<String, ClassAttr> attributes;
    public Map<String, Value> methods;

    public Value constructor;

    public JClass(String name, Map<String, ClassAttr> attributes) {
        this.name = name;
        this.attributes = attributes;
        this.methods = new HashMap<>();
    }

    public String toString() {
        return name;
    }

    public void addMethod(String name, Value value) {
        if (name.equals("<make>"))
            constructor = value;
        else
            methods.put(name, value);
    }

    public Value getField(String name, boolean internal) {
        ClassAttr attr = attributes.get(name);
        if (attr != null && attr.isStatic && (!attr.isPrivate || internal))
            return attr.val;

        Value val = methods.get(name);
        JClosure method = val != null ? val.asClosure() : null;
        if (method != null && method.function.isStatic && (!method.function.isPrivate || internal))
            return val;

        return null;
    }

    public VMResult setField(String name, Value value, boolean internal) {
        ClassAttr attr = attributes.get(name);
        if (attr != null && attr.isStatic && (!attr.isPrivate || internal)) {
            attr.set(value);
            return VMResult.OK;
        }
        return VMResult.ERROR;
    }

    public boolean hasField(String name) {
        return attributes.containsKey(name);
    }

    public boolean has(String name) {
        return attributes.containsKey(name) || methods.containsKey(name);
    }
}
