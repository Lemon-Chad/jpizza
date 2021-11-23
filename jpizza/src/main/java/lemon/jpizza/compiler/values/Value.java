package lemon.jpizza.compiler.values;

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
    protected Var var;

    public boolean isNull = false;
    public boolean isNumber = false;
    public boolean isString = false;
    public boolean isList = false;
    public boolean isMap = false;
    public boolean isBool = false;
    public boolean isFunc = false;
    public boolean isVar = false;

    public Value() {
        this.isNull = true;
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

    public VMResult add(Value value) {
        if (isNumber && value.isNumber) {
            number += value.number;
            return VMResult.OK;
        }
        else if (isString) {
            string += value.asString();
        }
        else if (isList) {
            list.addAll(value.asList());
        }
        else if (isMap) {
            map.putAll(value.asMap());
        }
        return VMResult.ERROR;
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
        return "";
    }

    @Override
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
        else if (isFunc) {
            return "function";
        }
        return "void";
    }

}
