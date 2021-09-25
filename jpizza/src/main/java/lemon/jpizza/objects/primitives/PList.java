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

import static lemon.jpizza.Operations.OP;

@SuppressWarnings("unused")
public class PList extends Value {
    public PList(List<Obj> value) { super(value); jptype = Constants.JPType.List; }
    public List<Obj> trueValue() { return (List<Obj>) value; }

    // Functions

    public Obj len() { return number(); }
    public Obj contains(Obj other) {
        List<Obj> trueval = trueValue();
        int size = trueval.size();
        for (int i = 0; i < size; i++) {
            if (((Bool)((Pair<Obj, RTError>) trueval.get(i).getattr(OP.EQ, other)).a).trueValue())
                return new Bool(true);
        }
        return new Bool(false);
    }

    // Methods

    public Pair<Obj, RTError> get(Obj o) {
        Pair<Num, RTError> dble = inRange(o);
        if (dble.b != null) return new Pair<>(null, dble.b);
        Num other = dble.a;
        return new Pair<>(trueValue().get(Math.toIntExact(Math.round(other.trueValue()))), null);
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
    public Pair<Obj, RTError> mul(Obj o) {
        Obj n = o.number();
        if (n.jptype != Constants.JPType.Number) return new Pair<>(null, RTError.Type(
                pos_start, pos_end,
                "List index must be a number",
                context
        ));
        Num other = (Num) n;
        if (other.floating)
            return new Pair<>(null, RTError.Type(
                    pos_start, pos_end,
                    "Multiplier must be long, not double",
                    context
            ));
        List<Obj> finalValues = trueValue();
        for (int i = 0; i < other.trueValue(); i++) {
            finalValues.addAll(trueValue());
        } return new Pair<>(
                new PList(finalValues).set_context(context).set_pos(pos_start, pos_end),
                null
        );
    }
    public Pair<Obj, RTError> append(Obj other) {
        List<Obj> trueval = trueValue();
        trueval.add(other);
        return new Pair<>(new PList(trueval).set_context(context).set_pos(pos_start, pos_end), null);
    }
    public Pair<Obj, RTError> extend(Obj o) {
        List<Obj> otherval = ((PList) o.alist()).trueValue();
        List<Obj> trueval = trueValue();
        trueval.addAll(otherval);
        return new Pair<>(
                new PList(trueval).set_context(context).set_pos(pos_start, pos_end),
                null
        );
    }
    public Pair<Num, RTError> inRange(Obj o) {
        Obj n = o.number();
        if (n.jptype != Constants.JPType.Number) return new Pair<>(null, RTError.Type(
                pos_start, pos_end,
                "List index must be a number",
                context
        ));
        Num other = (Num) n;
        if (other.floating)
            return new Pair<>(null, RTError.Type(
                    pos_start, pos_end,
                    "List index must be long, not double",
                    context
            ));
        if (other.trueValue() + 1 > trueValue().size()) return new Pair<>(null, RTError.OutOfBounds(
                pos_start, pos_end,
                "List index out of range",
                context
        ));
        return new Pair<>(other, null);
    }
    public Pair<Obj, RTError> pop(Obj o) {
        Pair<Num, RTError> dble = inRange(o);
        if (dble.b != null) return new Pair<>(null, dble.b);
        Num other = dble.a;
        List<Obj> x = trueValue();
        x.remove(Math.toIntExact(Math.round(other.trueValue())));
        value = x;
        return new Pair<>(
                new PList(x).set_context(context).set_pos(pos_start, pos_end),
                null
        );
    }
    public Pair<Obj, RTError> add(Obj o) { return extend(o); }
    public Pair<Obj, RTError> mod(Obj o) { return append(o); }
    public Pair<Obj, RTError> remove(Obj o) {
        ArrayList<Obj> updated = new ArrayList<>(trueValue());
        updated.removeIf(value -> ((Bool)((Pair<Obj, RTError>) value.getattr(OP.EQ, o)).a).trueValue());
        return new Pair<>(new PList(updated).set_context(context).set_pos(pos_start, pos_end), null);
    }

    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != Constants.JPType.List) return new Pair<>(new Bool(false), null);
        return new Pair<>(new Bool(this.trueValue().equals(((PList) o).trueValue())), null);
    }


    // Conversions

    public Obj dictionary() { return new Dict(new HashMap<>(){{
        List<Obj> trueval = trueValue();
        int size = trueval.size();
        for (int i = 0; i < size; i++) {
            Obj x = trueval.get(i);
            put(x, x);
        }
    }}).set_context(context).set_pos(pos_start, pos_end); }
    public Obj number() { return new Num(trueValue().size())
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj function() { return new Function(null, new ListNode((List<Node>) value, pos_start, pos_end), null)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj alist() { return new PList(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj bool() { return new Bool(trueValue().size() > 0)
            .set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public Obj copy() { return new PList(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("list").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return Arrays.deepToString(trueValue().toArray()); }

}
