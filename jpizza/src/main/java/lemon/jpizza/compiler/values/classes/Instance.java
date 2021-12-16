package lemon.jpizza.compiler.values.classes;

import lemon.jpizza.Constants;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JClosure;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.compiler.vm.VMResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Instance {
    public final JClass clazz;
    public final Map<String, ClassAttr> fields;
    public final Map<String, Value> methods;
    public final Map<String, Value> binMethods;
    public Value self;
    final VM vm;
    final Map<String, String> generics;

    public Instance(JClass clazz, VM vm) {
        this.clazz = clazz;
        methods = clazz.methods;
        binMethods = clazz.binMethods;
        generics = new HashMap<>();

        this.vm = vm;

        fields = new HashMap<>();
        copyAttributes(clazz.attributes, fields);
    }

    public Instance(String name, Map<String, ClassAttr> attrs, VM vm) {
        this(new JClass(name, attrs, null), vm);
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

    public void putGeneric(String key, Value val) {
        generics.put(key, val.asString());
    }

    public String getGeneric(String key) {
        return generics.get(key);
    }

    private String stringOp(String opName) {
        String res = unfailableOp(opName, clazz.name, "String");
        if (res == null)
            res = vm.pop().asString();
        return res;
    }

    public boolean instanceOf(Value value) {
        if (value.isEnumChild && hasField("$child") && hasField("$child")) {
            return value.asEnumChild().getValue() == fields.get("$child").val.asNumber().intValue() &&
                    value.asEnumChild().getParent() == fields.get("$parent").val.asEnum();
        }
        else if (value.isClass) {
            return clazz == value.asClass();
        }
        return false;
    }

    public String type() {
        return stringOp("type");
    }

    @Override
    public String toString() {
        return stringOp("string");
    }

    private <T> T _unfailableOp(String opName, T def, String type) {
        Value val = binMethods.get(opName);
        if (val != null) {
            vm.push(val);
            boolean worked = vm.call(val.asClosure(), self, new Value[0], new HashMap<>(), new String[0]);
            if (!worked)
                return def;

            vm.frame = vm.frames.peek();
            vm.frame.returnType = type;

            VMResult res = vm.run();
            if (res == VMResult.ERROR) {
                return def;
            }
            else {
                return null;
            }
        }
        return def;
    }

    private <T> T unfailableOp(String opName, T def, String type) {
        vm.safe = true;
        T res = _unfailableOp(opName, def, type);
        vm.safe = false;
        return res;
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

    public Double asNumber() {
        Double res = unfailableOp("number", 0.0, "num");
        if (res == null)
            res = vm.pop().asNumber();
        return res;
    }

    public boolean asBool() {
        Boolean res = unfailableOp("boolean", true, "bool");
        if (res == null)
            res = vm.pop().asBool();
        return res;
    }

    public List<Value> asList() {
        List<Value> res = unfailableOp("list", List.of(self), "list");
        if (res == null)
            res = vm.pop().asList();
        return res;
    }

    public Map<Value, Value> asMap() {
        Map<Value, Value> res = unfailableOp("map", Map.of(
                self, self
        ), "map");
        if (res == null)
            res = vm.pop().asMap();
        return res;
    }

    public Instance copy() {
        return new Instance(clazz, vm);
    }

    public byte[] asBytes() {
        byte[] res = unfailableOp("bytes", Constants.objToBytes(self.asObject()), "bytes");
        if (res == null)
            res = vm.pop().asBytes();
        return res;
    }

    public boolean hasField(String key) {
        return fields.containsKey(key);
    }
}
