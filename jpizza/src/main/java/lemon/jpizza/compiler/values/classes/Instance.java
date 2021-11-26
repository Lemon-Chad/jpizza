package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JClosure;
import lemon.jpizza.compiler.vm.VMResult;

import java.util.HashMap;
import java.util.Map;

public class Instance {
    public final JClass clazz;
    public Map<String, ClassAttr> fields;
    public Map<String, Value> methods;

    public Instance(JClass clazz) {
        this.clazz = clazz;
        methods = clazz.methods;

        fields = new HashMap<>();
        for (Map.Entry<String, ClassAttr> entry : clazz.attributes.entrySet()) {
            ClassAttr value = entry.getValue();
            fields.put(entry.getKey(), new ClassAttr(
                    value.val,
                    value.type,
                    value.isStatic,
                    value.isPrivate
            ));
        }

    }

    public Instance(JClass clazz, Map<String, ClassAttr> fields, Map<String, Value> methods) {
        this.clazz = clazz;
        this.fields = fields;
        this.methods = methods;
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

    public VMResult setField(String name, Value value, boolean internal) {
        ClassAttr attr = fields.get(name);
        if (attr != null && (!attr.isPrivate || internal)) {
            attr.set(value);
            return VMResult.OK;
        }
        return VMResult.ERROR;
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    public boolean has(String name) {
        return fields.containsKey(name) || methods.containsKey(name);
    }
}
