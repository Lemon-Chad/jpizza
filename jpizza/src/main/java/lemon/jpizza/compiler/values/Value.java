package lemon.jpizza.compiler.values;

import lemon.jpizza.Constants;
import lemon.jpizza.compiler.ChunkCode;
import lemon.jpizza.compiler.values.classes.BoundMethod;
import lemon.jpizza.compiler.values.classes.Instance;
import lemon.jpizza.compiler.values.classes.JClass;
import lemon.jpizza.compiler.values.classes.Namespace;
import lemon.jpizza.compiler.values.enums.JEnum;
import lemon.jpizza.compiler.values.enums.JEnumChild;
import lemon.jpizza.compiler.values.functions.*;
import lemon.jpizza.compiler.vm.VMResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Value {
    protected double number;
    protected String string;
    protected boolean bool;
    protected List<Value> list;
    protected Map<Value, Value> map;
    protected JFunc func;
    protected JNative nativeFunc;
    protected Var var;
    protected JClosure closure;
    protected JClass jClass;
    protected Instance instance;
    protected List<String> type;
    protected BoundMethod boundMethod;
    protected Namespace namespace;
    protected JEnum enumParent;
    protected JEnumChild enumChild;
    protected Spread spread;
    protected Value ref;
    protected byte[] bytes;
    protected Result res;
    protected String patternBinding;
    protected Pattern pattern;

    public boolean isNull = false;
    public boolean isNumber = false;
    public boolean isString = false;
    public boolean isList = false;
    public boolean isMap = false;
    public boolean isBool = false;
    public boolean isFunc = false;
    public boolean isNativeFunc = false;
    public boolean isVar = false;
    public boolean isClosure = false;
    public boolean isClass = false;
    public boolean isInstance = false;
    public boolean isType = false;
    public boolean isBoundMethod = false;
    public boolean isNamespace = false;
    public boolean isEnumParent = false;
    public boolean isEnumChild = false;
    public boolean isSpread = false;
    public boolean isRef = false;
    public boolean isBytes = false;
    public boolean isRes = false;
    public boolean isPatternBinding = false;
    public boolean isPattern = false;

    public Value() {
        this.isNull = true;
    }

    public Value(Pattern pattern) {
        this.pattern = pattern;
        this.isPattern = true;
    }

    public Value(Result res) {
        this.res = res;
        this.isRes = true;
    }

    public Value(byte[] bytes) {
        this.bytes = bytes;
        this.isBytes = true;
    }

    public Value(Value value) {
        this.ref = value;
        this.isRef = true;
    }

    public static Value patternBinding(String patternBinding) {
        Value value = new Value();
        value.patternBinding = patternBinding;
        value.isPatternBinding = true;
        value.isNull = false;
        return value;
    }

    public Value(Spread spread) {
        this.spread = spread;
        this.isSpread = true;
    }

    public Value(JEnum enumParent) {
        this.enumParent = enumParent;
        this.isEnumParent = true;
    }

    public Value(JEnumChild enumChild) {
        this.enumChild = enumChild;
        this.isEnumChild = true;
    }

    public Value(Namespace namespace) {
        this.namespace = namespace;
        this.isNamespace = true;
    }

    public Value(BoundMethod boundMethod) {
        this.boundMethod = boundMethod;
        this.isBoundMethod = true;
    }

    public Value(JClosure closure) {
        this.closure = closure;
        this.isClosure = true;
    }

    public Value(JClass jClass) {
        this.jClass = jClass;
        this.isClass = true;
    }

    public static Value fromType(List<String> type) {
        Value value = new Value();
        value.type = type;
        value.isType = true;
        value.isNull = false;
        return value;
    }

    public Value(Instance instance) {
        this.instance = instance;
        this.isInstance = true;
    }

    public Value(Var var) {
        this.var = var;
        this.isVar = true;
    }

    public Value(double number) {
        this.number = number;
        this.isNumber = true;
    }

    public Value(String string) {
        this.string = string;
        this.isString = true;
    }

    public Value(boolean bool) {
        this.bool = bool;
        this.isBool = true;
    }

    public Value(List<Value> list) {
        this.list = list;
        this.isList = true;
    }

    public Value(Map<Value, Value> map) {
        this.map = map;
        this.isMap = true;
    }

    public Value(JFunc func) {
        this.func = func;
        this.isFunc = true;
    }

    public Value(JNative nativeFunc) {
        this.nativeFunc = nativeFunc;
        this.isNativeFunc = true;
    }

    public Double asNumber() {
        if (isNumber) {
            return number;
        }
        else if (isString) {
            return (double) string.length();
        }
        else if (isBool) {
            return bool ? 1.0 : 0.0;
        }
        else if (isList) {
            return (double) list.size();
        }
        else if (isMap) {
            return (double) map.size();
        }
        else if (isInstance) {
            return instance.asNumber();
        }
        else if (isRef) {
            return ref.asNumber();
        }
        else if (isRes) {
            return res.isError() ? 0.0 : 1.0;
        }
        return 0.0;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean asBool() {
        if (isBool) {
            return bool;
        }
        else if (this.isNull) {
            return false;
        }
        else if (isNumber) {
            return number != 0.0;
        }
        else if (isString) {
            return !string.isEmpty();
        }
        else if (isList) {
            return !list.isEmpty();
        }
        else if (isMap) {
            return !map.isEmpty();
        }
        else if (isInstance) {
            return instance.asBool();
        }
        else if (isRef) {
            return ref.asBool();
        }
        else if (isRes) {
            return !res.isError();
        }
        return false;
    }

    @SuppressWarnings("DuplicatedCode")
    public String asString() {
        if (isString) {
            return string;
        }
        else if (this.isNull) {
            return "";
        }
        else if (isNumber) {
            if (number == Double.MAX_VALUE) {
                return "Infinity";
            }
            else if (number == Double.MIN_VALUE) {
                return "-Infinity";
            }

            if (Math.floor(number) == number && number < Long.MAX_VALUE && number > Long.MIN_VALUE) {
                return String.valueOf((long) number);
            }
            return String.valueOf(number);
        }
        else if (isBool) {
            return String.valueOf(bool);
        }
        else if (isList) {
            StringBuilder result = new StringBuilder("[");
            list.forEach(k -> {
                if (k.isString) {
                    result.append('"').append(k.string).append('"');
                }
                else {
                    result.append(k.asString());
                }
                result.append(", ");
            });
            if (result.length() > 1) {
                result.setLength(result.length() - 2);
            } result.append("]");
            return result.toString();
        }
        else if (isMap) {
            StringBuilder result = new StringBuilder("{");
            map.forEach((k, v) -> {
                if (k.isString) {
                    result.append('"').append(k.string).append('"');
                }
                else {
                    result.append(k.asString());
                }
                result.append(": ");
                if (v.isString) {
                    result.append('"').append(v.string).append('"');
                }
                else {
                    result.append(v.asString());
                }
                result.append(", ");
            });
            if (result.length() > 1) {
                result.setLength(result.length() - 2);
            } result.append("}");
            return result.toString();
        }
        else if (isFunc) {
            return func.toString();
        }
        else if (isClosure) {
            return closure.function.toString();
        }
        else if (isVar) {
            return var.toString();
        }
        else if (isNativeFunc) {
            return nativeFunc.toString();
        }
        else if (isClass) {
            return jClass.toString();
        }
        else if (isType) {
            return String.join("", type);
        }
        else if (isInstance) {
            return instance.toString();
        }
        else if (isBoundMethod) {
            return boundMethod.toString();
        }
        else if (isNamespace) {
            return namespace.name();
        }
        else if (isEnumParent) {
            return enumParent.name();
        }
        else if (isEnumChild) {
            return enumChild.type() + "::" + enumChild.getValue();
        }
        else if (isRef) {
            return ref.asString();
        }
        else if (isBytes) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; i++)
                sb.append(bytes[i]).append(", ");

            return "{ " + sb + "len=" + bytes.length + " }";
        }
        else if (isRes) {
            if (res.isError()) {
                return String.format("(\"%s\" : \"%s\")", res.getErrorMessage(), res.getErrorReason());
            }
            else {
                return String.format("(%s)", res.getValue());
            }
        }
        else if (isPatternBinding) {
            return "{ pattern: " + patternBinding + " }";
        }
        else if (isPattern) {
            StringBuilder sb = new StringBuilder(pattern.value.toString() + " { ");
            for (Map.Entry<String, Value> entry : pattern.cases.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue().asString()).append(", ");
            }
            for (Map.Entry<String, String> entry : pattern.matches.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
            }
            return sb + "}";
        }
        return "";
    }

    public String toString() {
        return asString();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Value))
            return false;

        Value o = (Value) obj;
        if (this.isNull)
            return o.isNull;

        else if (this.isNumber)
            return o.isNumber && this.number == o.number;

        else if (this.isString)
            return o.isString && this.string.equals(o.string);

        else if (this.isBool)
            return o.isBool && this.bool == o.bool;

        else if (this.isList)
            return o.isList && this.list.equals(o.list);

        else if (this.isMap)
            return o.isMap && this.map.equals(o.map);

        else if (this.isEnumChild)
            return o.isEnumChild && this.enumChild.equals(o.enumChild);

        return this == o;
    }

    public List<Value> asList() {
        if (isList) {
            return list;
        }
        else if (isString) {
            String[] lis = string.split("");
            List<Value> list = new ArrayList<>();
            for (String s : lis) {
                list.add(new Value(s));
            }
            return list;
        }
        else if (this.isNull) {
            return new ArrayList<>();
        }
        else if (isInstance) {
            return instance.asList();
        }
        else if (isRef) {
            return ref.asList();
        }
        else if (isRes) {
            if (res.isError()) {
                return List.of(new Value(res.getErrorMessage()), new Value(res.getErrorReason()));
            }
            else {
                return List.of(new Value(res.getValue()));
            }
        }
        return new ArrayList<>(List.of(this));
    }

    public Var asVar() {
        if (isRef) {
            return ref.asVar();
        }
        return var;
    }

    public Map<Value, Value> asMap() {
        if (isMap) {
            return map;
        }
        else if (this.isNull) {
            return Map.of();
        }
        else if (isInstance) {
            return instance.asMap();
        }
        else if (isRef) {
            return ref.asMap();
        }
        else if (isRes) {
            return Map.of(new Value("success"), new Value(res.getValue()), new Value("error"),
                    new Value(res.isError() ? List.of(new Value(res.getErrorMessage()),
                                                      new Value(res.getErrorReason()))
                                            : new ArrayList<>()));
        }
        return Map.of(this, this);
    }

    public JFunc asFunc() {
        if (isFunc) {
            return func;
        }
        else if (this.isClosure) {
            return closure.function;
        }
        else if (isRef) {
            return ref.asFunc();
        }
        return null;
    }

    public JClosure asClosure() {
        if (isClosure) {
            return closure;
        }
        else if (isRef) {
            return ref.asClosure();
        }
        return null;
    }

    public JClass asClass() {
        if (isClass) {
            return jClass;
        }
        else if (isRef) {
            return ref.asClass();
        }
        return null;
    }

    public JNative asNative() {
        return nativeFunc;
    }

    public BoundMethod asBoundMethod() {
        if (isBoundMethod) {
            return boundMethod;
        }
        else if (isRef) {
            return ref.asBoundMethod();
        }
        return null;
    }

    public String type() {
        if (isNumber) {
            return "num";
        }
        else if (isString) {
            return "String";
        }
        else if (isBool) {
            return "bool";
        }
        else if (isList) {
            return "list";
        }
        else if (isMap) {
            return "dict";
        }
        else if (isFunc || isNativeFunc || isClosure) {
            return "function";
        }
        else if (isClass) {
            return "recipe";
        }
        else if (isInstance) {
            return instance.type();
        }
        else if (isNamespace) {
            return "namespace";
        }
        else if (isEnumParent) {
            return "Enum";
        }
        else if (isEnumChild) {
            return enumChild.type();
        }
        else if (isRef) {
            return "[" + ref.type() + "]";
        }
        else if (isBytes) {
            return "bytearray";
        }
        else if (isRes) {
            return "catcher";
        }
        return "void";
    }

    // Mutative Addition
    public VMResult add(Value other) {
        if (isNumber) {
            number += other.asNumber();
            return VMResult.OK;
        }
        else if (isList) {
            list.addAll(other.asList());
            return VMResult.OK;
        }

        return VMResult.ERROR;
    }

    // List Mutators
    public void append(Value value) {
        list.add(value);
    }

    public Value pop(Double index) {
        int i = index.intValue();
        Value value = list.get(i);
        list.remove(i);
        return value;
    }

    public void insert(Double index, Value value) {
        list.add(index.intValue(), value);
    }

    public void set(Double index, Value value) {
        list.set(index.intValue(), value);
    }

    public void remove(Value value) {
        list.remove(value);
    }

    // Map Mutators
    public void set(Value key, Value value) {
        map.put(key, value);
    }

    public void delete(Value key) {
        map.remove(key);
    }

    public Instance asInstance() {
        if (isRef) {
            return ref.asInstance();
        }
        return instance;
    }

    public Result asRes() {
        return res;
    }

    public Namespace asNamespace() {
        if (isRef) {
            return ref.asNamespace();
        }
        return namespace;
    }

    public List<String> asType() {
        if (isRef) {
            List<String> type = ref.asType();
            type.add(0, "[");
            type.add("]");
            return type;
        }
        return type;
    }

    public String toSafeString() {
        if (isInstance) {
            return instance.clazz.name;
        }
        else if (isVar) {
            return var.toSafeString();
        }
        return toString();
    }

    public Value copy() {
        if (isNumber) {
            return new Value(number);
        }
        else if (isString) {
            return new Value(string);
        }
        else if (isBool) {
            return new Value(bool);
        }
        else if (isList) {
            List<Value> list = new ArrayList<>();
            for (Value value : this.list) {
                list.add(value.copy());
            }
            return new Value(list);
        }
        else if (isMap) {
            Map<Value, Value> map = new HashMap<>();
            for (Map.Entry<Value, Value> entry : this.map.entrySet()) {
                map.put(entry.getKey().copy(), entry.getValue().copy());
            }
            return new Value(map);
        }
        else if (isClass) {
            return new Value(jClass.copy());
        }
        else if (isInstance) {
            return new Value(instance.copy());
        }
        return this;
    }

    public JEnumChild asEnumChild() {
        if (isRef) {
            return ref.asEnumChild();
        }
        return enumChild;
    }

    public JEnum asEnum() {
        if (isRef) return ref.asEnum();
        return enumParent;
    }

    public Spread asSpread() {
        if (isRef) return ref.asSpread();
        return spread;
    }

    public Value asRef() {
        return ref;
    }

    public Value setRef(Value value) {
        ref = value;
        return this;
    }

    public byte[] asBytes() {
        if (isInstance)
            return instance.asBytes();
        else if (isBytes)
            return bytes;
        return Constants.objToBytes(asObject());
    }

    public static Value fromObject(Object object) {
        if (object instanceof Double ||
                object instanceof Float ||
                object instanceof Integer ||
                object instanceof Long ||
                object instanceof Short ||
                object instanceof Byte) {
            return new Value(Double.parseDouble(object.toString()));
        }
        else if (object instanceof String) {
            return new Value((String) object);
        }
        else if (object instanceof Boolean) {
            return new Value((Boolean) object);
        }
        else if (object instanceof List) {
            List<Value> list = new ArrayList<>();
            for (Object o : (List<Object>) object) {
                list.add(fromObject(o));
            }
            return new Value(list);
        }
        else if (object instanceof Map) {
            Map<Value, Value> map = new HashMap<>();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) object).entrySet()) {
                map.put(fromObject(entry.getKey()), fromObject(entry.getValue()));
            }
            return new Value(map);
        }
        else if (object instanceof JClass) {
            return new Value((JClass) object);
        }
        else if (object instanceof Instance) {
            return new Value((Instance) object);
        }
        else if (object instanceof JEnumChild) {
            return new Value((JEnumChild) object);
        }
        else if (object instanceof JEnum) {
            return new Value((JEnum) object);
        }
        else if (object instanceof Spread) {
            return new Value((Spread) object);
        }
        else if (object instanceof Value) {
            return (Value) object;
        }
        else if (object instanceof byte[]) {
            return new Value((byte[]) object);
        }
        return new Value();
    }

    public static NativeResult fromByte(byte[] bytes) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream is = new ObjectInputStream(in);
            Object obj = is.readObject();
            return NativeResult.Ok(Value.fromObject(obj));
        } catch (IOException | ClassNotFoundException e) {
            return NativeResult.Err("Internal", "Could not read bytes (" + e.getMessage() + ")");
        }
    }

    public Object asObject() {
        if (isInstance)
            return instance;
        else if (isClass)
            return jClass;
        else if (isEnumParent)
            return enumParent;
        else if (isEnumChild)
            return enumChild;
        else if (isSpread)
            return spread;
        else if (isRef)
            return ref.asObject();
        else if (isBytes)
            return bytes;
        else if (isString)
            return string;
        else if (isNumber)
            return number;
        else if (isBool)
            return bool;
        else if (isList) {
            List<Object> list = new ArrayList<>();
            for (Value value : this.list) {
                list.add(value.asObject());
            }
            return list;
        }
        else if (isMap) {
            Map<Object, Object> map = new HashMap<>();
            for (Map.Entry<Value, Value> entry : this.map.entrySet()) {
                map.put(entry.getKey().asObject(), entry.getValue().asObject());
            }
            return map;
        }
        return this;
    }

    public String asPatternBinding() {
        return patternBinding;
    }

    public Pattern asPattern() {
        return pattern;
    }

    public Value shallowCopy() {
        if (isNumber) {
            return new Value(number);
        }
        else if (isString) {
            return new Value(string);
        }
        else if (isBool) {
            return new Value(bool);
        }
        else if (isList) {
            return new Value(new ArrayList<>(list));
        }
        else if (isMap) {
            return new Value(new HashMap<>(map));
        }
        else if (isClass) {
            return new Value(jClass.copy());
        }
        else if (isInstance) {
            return new Value(instance.copy());
        }
        return this;
    }

    public static int[] dumpString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int[] result = new int[bytes.length + 2];
        result[0] = ChunkCode.String;
        result[1] = bytes.length;
        for (int i = 0; i < bytes.length; i++) {
            result[i + 2] = ((int) bytes[i]) << 2;
        }
        return result;
    }

    public static void addAllString(List<Integer> list, String s) {
        for (int i : dumpString(s)) {
            list.add(i);
        }
    }

    public int[] dump() {
        if (isBool) {
            return new int[] { ChunkCode.Boolean, bool ? 1 : 0 };
        }
        else if (isNumber) {
            long longValue = Double.doubleToRawLongBits(number);
            return new int[] { ChunkCode.Number, (int) (longValue >>> 32), (int) longValue };
        }
        else if (isString) {
            return dumpString(string);
        }
        else if (isEnumParent) {
            return enumParent.dump();
        }
        else if (isFunc) {
            return func.dump();
        }
        else if (isType) {
            List<Integer> result = new ArrayList<>();
            result.add(ChunkCode.Type);
            result.add(type.size());
            for (String type : this.type) {
                addAllString(result, type);
            }
            return result.stream().mapToInt(i -> i).toArray();
        }
        return null;
    }
}
