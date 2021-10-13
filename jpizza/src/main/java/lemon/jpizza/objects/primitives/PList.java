package lemon.jpizza.objects.primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.values.ListNode;
import lemon.jpizza.objects.executables.Function;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.Value;

import java.util.*;
import lemon.jpizza.Pair;

@SuppressWarnings("unused")
public class PList extends Value {
    public PList(List<Obj> value) {
        super(value);
        jptype = Constants.JPType.List;
        list = value;
    }

    // Functions

    public Obj len() { return number(); }
    public Obj contains(Obj other) {
        return new Bool(list.contains(other));
    }

    // Methods

    public Pair<Obj, RTError> get(Obj o) {
        Pair<Obj, RTError> dble = inRange(o);
        if (dble.b != null) return new Pair<>(null, dble.b);
        Obj other = dble.a;
        return new Pair<>(list.get(Math.toIntExact(Math.round(other.number))), null);
    }
    public Pair<Obj, RTError> div(Obj other) {
        return remove(other);
    }
    public Pair<Obj, RTError> sub(Obj other) {
        return get(other);
    }
    public Pair<Obj, RTError> bracket(Obj other) { return get(other); }
    public Pair<Obj, RTError> bite(Obj other) {
        return pop(other);
    }
    public Pair<Obj, RTError> mul(Obj other) {
        if (other.jptype != Constants.JPType.Number) return new Pair<>(null, RTError.Type(
                pos_start, pos_end,
                "List index must be a number",
                context
        ));
        if (other.floating)
            return new Pair<>(null, RTError.Type(
                    pos_start, pos_end,
                    "Multiplier must be long, not double",
                    context
            ));
        List<Obj> finalValues = new ArrayList<>(list);
        for (int i = 0; i < other.number; i++) {
            finalValues.addAll(list);
        } return new Pair<>(
                new PList(finalValues).set_context(context).set_pos(pos_start, pos_end),
                null
        );
    }
    public Pair<Obj, RTError> append(Obj other) {
        list.add(other);
        return new Pair<>(new PList(list).set_context(context).set_pos(pos_start, pos_end), null);
    }
    public Pair<Obj, RTError> extend(Obj o) {
        List<Obj> otherval = o.alist().list;
        list.addAll(otherval);
        return new Pair<>(
                new PList(list).set_context(context).set_pos(pos_start, pos_end),
                null
        );
    }
    public Pair<Obj, RTError> inRange(Obj other) {
        if (other.jptype != Constants.JPType.Number) return new Pair<>(null, RTError.Type(
                pos_start, pos_end,
                "List index must be a number",
                context
        ));
        if (other.floating)
            return new Pair<>(null, RTError.Type(
                    pos_start, pos_end,
                    "List index must be long, not double",
                    context
            ));
        if (other.number >= list.size() || other.number < -list.size()) return new Pair<>(null, RTError.OutOfBounds(
                pos_start, pos_end,
                "List index out of range",
                context
        ));
        return new Pair<>(new Num((other.number + list.size()) % list.size()), null);
    }
    public Pair<Obj, RTError> pop(Obj o) {
        Pair<Obj, RTError> dble = inRange(o);
        if (dble.b != null) return new Pair<>(null, dble.b);
        Obj other = dble.a;
        list.remove(Math.toIntExact(Math.round(other.number)));
        return new Pair<>(
                new PList(list).set_context(context).set_pos(pos_start, pos_end),
                null
        );
    }
    public Pair<Obj, RTError> add(Obj o) { return extend(o); }
    public Pair<Obj, RTError> mod(Obj o) { return append(o); }
    public Pair<Obj, RTError> remove(Obj o) {
        ArrayList<Obj> updated = new ArrayList<>(list);
        updated.remove(o);
        return new Pair<>(new PList(updated).set_context(context).set_pos(pos_start, pos_end), null);
    }

    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != Constants.JPType.List) return new Pair<>(new Bool(false), null);
        return new Pair<>(new Bool(this.list.equals(o.list)), null);
    }


    // Conversions

    public Obj dictionary() { return new Dict(new HashMap<>(){{
        int size = list.size();
        for (int i = 0; i < size; i++) {
            Obj x = list.get(i);
            put(x, x);
        }
    }}).set_context(context).set_pos(pos_start, pos_end); }
    public Obj number() { return new Num(list.size())
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj function() { return new Function(null, new ListNode((List<Node>) value, pos_start, pos_end), null)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj alist() { return new PList(list).set_context(context).set_pos(pos_start, pos_end); }
    public Obj bool() { return new Bool(list.size() > 0)
            .set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public Obj copy() { return new PList(list).set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("list").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return Arrays.deepToString(list.toArray()); }

}
