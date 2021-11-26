package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JClosure;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.compiler.vm.VMResult;

import java.util.HashMap;
import java.util.Map;

public class Instance {
    public final JClass clazz;
    public final Map<String, ClassAttr> fields;
    public final Map<String, Value> methods;

    public Instance(JClass clazz) {
        this.clazz = clazz;
        methods = clazz.methods;

        fields = new HashMap<>();
        copyAttributes(clazz.attributes, fields);
    }

    public static void copyAttributes(Map<String, ClassAttr> src, Map<String, ClassAttr> dst) {
        for (Map.Entry<String, ClassAttr> entry : src.entrySet()) {
            ClassAttr value = entry.getValue();
            dst.put(entry.getKey(), new ClassAttr(
                    value.val,
                    value.type,
                    value.isStatic,
                    value.isPrivate
            ));
        }
    }

    public String type() {
        return clazz.name;
    }

    @Override
    public String toString() {
        return clazz.name;
    }

    public Value getField(String name, boolean internal) {
        ClassAttr attr = fields.get(name);
        if (attr != null && (!attr.isPrivate || internal))
            return attr.val;

        Value val = methods.get(name);
        JClosure method = val != null ? val.asClosure() : null;
        if (method != null && (!method.function.isPrivate || internal))
            return val;

        return null;
    }

    public NativeResult setField(String name, Value value, boolean internal) {
        return setField(name, value, fields, false, internal);
    }

    public static NativeResult setField(String name, Value value, Map<String, ClassAttr> fields, boolean staticContext, boolean internal) {
        ClassAttr attr = fields.get(name);
        if (attr != null && attr.isStatic == staticContext && (!attr.isPrivate || internal)) {
            String type = value.type();
            if (!attr.type.equals("any") && !attr.type.equals(type))
                return NativeResult.Err("Type", "Expected " + attr.type + " but got " + type);
            attr.set(value);
            return NativeResult.Ok();
        }
        return NativeResult.Err("Scope", "Undefined attribute '" + name + "'");
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public boolean has(String name) {
        return fields.containsKey(name) || methods.containsKey(name);
    }
}
