package lemon.jpizza.objects.primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.Pair;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.nodes.values.DictNode;
import lemon.jpizza.objects.executables.Function;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Dict extends Value {
    public Dict(Map<Obj, Obj> value) {
        super(new ConcurrentHashMap<>(value));
        map = (ConcurrentHashMap<Obj, Obj>) this.value;
        jptype = Constants.JPType.Dict;
    }
    public Dict(ConcurrentHashMap<Obj, Obj> value) {
        super(value);
        jptype = Constants.JPType.Dict;
        map = value;
    }

    // Functions

    public boolean contains(Obj other) {
        return map.containsKey(other);
    }
    public Obj delete(Obj other) {
        if (other != null)
            map.remove(other);
        return new Null();
    }
    public void set(Obj a, Obj b) {
        delete(a);
        map.put(a, b);
        new Null();
    }

    // Methods

    public Pair<Obj, RTError> add(Obj o) {
        Obj other = o.dictionary();
        ConcurrentHashMap<Obj, Obj> combo = new ConcurrentHashMap<>(map);
        other.map.forEach(
                (key, value) -> combo.merge(key, value, (v1, v2) -> v1)
        );
        return new Pair<>(new Dict(combo).set_context(context).set_pos(pos_start, pos_end), null);
    }
    public Pair<Obj, RTError> get(Obj o) {
        return new Pair<>(map.getOrDefault(o, new Null()), null);
    }

    public Pair<Obj, RTError> bracket(Obj o) {
        return get(o);
    }

    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != Constants.JPType.Dict) return new Pair<>(new Bool(false), null);
        return new Pair<>(new Bool(map.equals(o.map)), null);
    }

    // Conversions

    public Obj alist() { return new PList(new ArrayList<>(map.keySet()))
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj number() { return new Num(map.size()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj function() { return new Function(null, new DictNode(new HashMap<>(),
            pos_start, pos_end), null).set_context(context).set_pos(pos_start, pos_end); }
    public Obj dictionary() { return new Dict(map).set_context(context).set_pos(pos_start, pos_end); }
    public Obj bool() { return new Bool(map.size() > 0)
            .set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public String toString() {
        StringBuilder result = new StringBuilder("{");
        map.forEach((k, v) -> result.append(k).append(": ").append(v).append(", "));
        if (result.length() > 1) {
            result.setLength(result.length() - 2);
        } result.append("}");
        return result.toString();
    }
    public Obj copy() { return new Dict(map).set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("dict").set_context(context).set_pos(pos_start, pos_end); }

}