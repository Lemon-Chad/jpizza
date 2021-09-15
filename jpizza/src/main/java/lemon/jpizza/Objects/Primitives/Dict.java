package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.Pair;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.DictNode;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lemon.jpizza.Operations.OP;

public class Dict extends Value {
    public Dict(Map<Obj, Obj> value) { super(value); jptype = Constants.JPType.Dict; }
    public Map<Obj, Obj> trueValue() { return (Map<Obj, Obj>) value; }

    // Functions

    public boolean contains(Obj other) {
        for (Obj key : trueValue().keySet())
            if (((Bool)((Pair<Obj, RTError>) key.getattr(OP.EQ, other)).a).trueValue()) return true;
        return false;
    }
    public Obj delete(Obj other) {
        Obj key = null;
        for (Obj k : trueValue().keySet()) {
            if (((Bool)((Pair<Obj, RTError>) k.getattr(OP.EQ, other)).a).trueValue()) {
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

    public Pair<Obj, RTError> add(Obj o) {
        Dict other = (Dict) o.dictionary();
        Map<Obj, Obj> combo = new HashMap<>(trueValue());
        other.trueValue().forEach(
                (key, value) -> combo.merge(key, value, (v1, v2) -> v1)
        );
        return new Pair<>(new Dict(combo).set_context(context).set_pos(pos_start, pos_end), null);
    }
    public Pair<Obj, RTError> get(Obj o) {
        ArrayList<Obj> lx = new ArrayList<>();
        trueValue().forEach(
                (key, value) -> {
                    if (((Bool)((Pair<Obj, RTError>) key.getattr(OP.EQ, o)).a).trueValue()) lx.add(value);
                }
        );
        return new Pair<>(
                lx.size() > 0 ? lx.get(0) : new Null(),
                null
        );
    }

    public Pair<Obj, RTError> bracket(Obj o) {
        return get(o);
    }

    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != Constants.JPType.Dict) return new Pair<>(new Bool(false), null);
        return new Pair<>(new Bool(this.trueValue().equals(((Dict) o).trueValue())), null);
    }

    // Conversions

    public Obj alist() { return new PList(new ArrayList<>(trueValue().keySet()))
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj number() { return new Num(trueValue().size()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj function() { return new Function(null, new DictNode(new HashMap<>(),
            pos_start, pos_end), null).set_context(context).set_pos(pos_start, pos_end); }
    public Obj dictionary() { return new Dict(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj bool() { return new Bool(trueValue().size() > 0)
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
    public Obj copy() { return new Dict(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("dict").set_context(context).set_pos(pos_start, pos_end); }

}
