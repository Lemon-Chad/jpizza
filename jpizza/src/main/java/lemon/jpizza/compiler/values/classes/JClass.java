package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JClosure;
import lemon.jpizza.compiler.values.functions.NativeResult;

import java.util.HashMap;
import java.util.Map;

import static lemon.jpizza.compiler.values.classes.Instance.copyAttributes;

public class JClass {
    public final JClass superClass;

    public final String name;
    public final Map<String, ClassAttr> attributes;
    public Map<String, Value> methods;
    public Map<String, Value> binMethods;

    public Value constructor;

    public JClass(String name, Map<String, ClassAttr> attributes, JClass superClass) {
        this.name = name;
        this.attributes = new HashMap<>();
        this.methods = new HashMap<>();
        this.binMethods = new HashMap<>();

        this.superClass = superClass;
        if (superClass != null) {
            copyAttributes(superClass.attributes, this.attributes);
            this.methods = superClass.methods;
            this.binMethods = superClass.binMethods;
        }

        this.attributes.putAll(attributes);
    }

    public String toString() {
        return name;
    }

    public void addMethod(String name, Value value) {
        if (name.equals("<make>"))
            constructor = value;
        else if (value.asClosure().function.isBin)
            binMethods.put(name, value);
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

    public NativeResult setField(String name, Value value, boolean internal) {
        return Instance.setField(name, value, attributes, true, internal);
    }

    public boolean hasField(String name) {
        return attributes.containsKey(name);
    }

    public boolean has(String name) {
        return attributes.containsKey(name) || methods.containsKey(name);
    }
}
