package lemon.jpizza.compiler.values;

import lemon.jpizza.compiler.values.classes.BoundMethod;
import lemon.jpizza.compiler.values.classes.Instance;
import lemon.jpizza.compiler.values.classes.JClass;
import lemon.jpizza.compiler.values.functions.JClosure;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.compiler.values.functions.JNative;
import lemon.jpizza.compiler.vm.VMResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Value implements Serializable {
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

    public Value() {
        this.isNull = true;
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
            if (Math.floor(number) == number) {
                return String.valueOf((long) number);
            }
            return String.valueOf(number);
        }
        else if (isBool) {
            return String.valueOf(bool);
        }
        else if (isList) {
            return Arrays.deepToString(list.toArray());
        }
        else if (isMap) {
            StringBuilder result = new StringBuilder("{");
            map.forEach((k, v) -> result.append(k).append(": ").append(v).append(", "));
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

        return false;
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
        return new ArrayList<>(List.of(this));
    }

    public Var asVar() {
        return var;
    }

    public Map<Value, Value> asMap() {
        if (isMap) {
            return map;
        }
        else if (this.isNull) {
            return Map.of();
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
        return null;
    }

    public JClosure asClosure() {
        if (isClosure) {
            return closure;
        }
        return null;
    }

    public JClass asClass() {
        if (isClass) {
            return jClass;
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
        return instance;
    }

    public List<String> asType() {
        return type;
    }
}
