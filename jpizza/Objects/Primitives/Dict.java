package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Double;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.DictNode;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Dict extends Value {
    public Dict(Map<Obj, Obj> value) {super(value);}
    public Map<Obj, Obj> trueValue() { return (Map<Obj, Obj>) value; }

    // Functions

    public boolean contains(Obj other) {
        Obj[] keySet = (Obj[]) trueValue().keySet().toArray();
        int length = keySet.length;
        for (int i = 0; i < length; i++)
            if (((Bool)((Double<Obj, RTError>) keySet[i].getattr("eq", other)).a).trueValue()) return true;
        return false;
    }
    public Obj delete(Obj other) {
        Obj key = null;
        Obj[] entrySet = (Obj[]) trueValue().keySet().toArray();
        int length = entrySet.length;
        for (int i = 0; i < length; i++) {
            Obj k = entrySet[i];
            if (((Bool)((Double<Obj, RTError>) k.getattr("eq", other)).a).trueValue()) {
                key = k; break;
            }
        }
        if (key != null) {
            Map<Obj, Obj> v = trueValue();
            v.remove(key);
            value = v;
        }
        return new Null();
    }
    public Obj set(Obj a, Obj b) {
        Map<Obj, Obj> v = trueValue();
        v.put(a, b);
        value = v;
        return new Null();
    }

    // Methods

    public Double<Obj, RTError> add(Obj o) {
        Dict other = (Dict) o.dictionary();
        Map<Obj, Obj> combo = new HashMap<>(trueValue());
        other.trueValue().forEach(
                (key, value) -> combo.merge(key, value, (v1, v2) -> v1)
        );
        return new Double<>(new Dict(combo).set_context(context).set_pos(pos_start, pos_end), null);
    }
    public Double<Obj, RTError> get(Obj o) {
        ArrayList<Obj> lx = new ArrayList<>();
        trueValue().forEach(
                (key, value) -> {
                    if (((Bool)((Double<Obj, RTError>) key.getattr("eq", o)).a).trueValue()) lx.add(value);
                }
        );
        return new Double<>(
                lx.size() > 0 ? lx.get(0) : new Null(),
                null
        );
    }

    public Double<Obj, RTError> eq(Obj o) {
        if (!(o instanceof Dict)) return new Double<>(new Bool(false), null);
        return new Double<>(new Bool(this.trueValue().equals(((Dict) o).trueValue())), null);
    }

    // Conversions

    public Value alist() { return new PList(new ArrayList<>(trueValue().keySet()))
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value number() { return new Num(trueValue().size()).set_context(context).set_pos(pos_start, pos_end); }
    public Value function() { return new Function(null, new DictNode(new HashMap<>(),
            pos_start, pos_end), null).set_context(context).set_pos(pos_start, pos_end); }
    public Value dictionary() { return new Dict(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Value bool() { return new Bool(trueValue().size() > 0)
            .set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public String toString() {
        StringBuilder result = new StringBuilder("{");
        trueValue().forEach((k, v) -> result.append(k).append(": ").append(v).append(", "));
        if (result.length() > 1) {
            result.setLength(result.length() - 2);
        } result.append("}");
        return result.toString();
    }
    public Value copy() { return new Dict(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Value type() { return new Str("dict").set_context(context).set_pos(pos_start, pos_end); }

}
