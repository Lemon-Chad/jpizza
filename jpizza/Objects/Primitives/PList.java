package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Nodes.Values.ListNode;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;

import java.util.*;
import lemon.jpizza.Double;

@SuppressWarnings("unchecked")
public class PList extends Value {
    public PList(List<Obj> value) {super(value);}
    public List<Obj> trueValue() { return (ArrayList<Obj>) value; }

    // Functions

    public Obj len() { return number(); }
    public Obj contains(Obj other) {
        List<Obj> trueval = trueValue();
        int size = trueval.size();
        for (int i = 0; i < size; i++) {
            if (((Bool)((Double) trueval.get(i).getattr("eq", other)).get(0)).trueValue())
                return new Bool(true);
        }
        return new Bool(false);
    }

    // Methods

    public Double dot(Obj o) {
        if (!(o.number() instanceof Num)) return new Double(null, new RTError(
                pos_start, pos_end,
                "List index must be a number!",
                context
        ));
        Num other = (Num) o.number();
        if (other.floating())
            return new Double(null, new RTError(
                    pos_start, pos_end,
                    "List index must be int, not float",
                    context
            ));
        if (other.trueValue() + 1 > trueValue().size()) return new Double(null, new RTError(
                pos_start, pos_end,
                "List index out of range",
                context
        ));
        return new Double(trueValue().get(Math.round(other.trueValue())), null);
    }
    public Double div(Obj other) {
        return remove(other);
    }
    public Double sub(Obj other) {
        return dot(other);
    }
    public Double bite(Obj other) {
        return pop(other);
    }
    public Double mul(Obj o) {
        if (!(o.number() instanceof Num)) return new Double(null, new RTError(
                pos_start, pos_end,
                "List index must be a number!",
                context
        ));
        Num other = (Num) o.number();
        if (other.floating())
            return new Double(null, new RTError(
                    pos_start, pos_end,
                    "Multiplier must be int, not float",
                    context
            ));
        List<Obj> finalValues = trueValue();
        for (int i = 0; i < other.trueValue(); i++) {
            finalValues.addAll(trueValue());
        } return new Double(
                new PList(finalValues).set_context(context).set_pos(pos_start, pos_end),
                null
        );
    }
    public Double append(Obj other) {
        List<Obj> trueval = trueValue();
        trueval.add(other);
        return new Double(new PList(trueval).set_context(context).set_pos(pos_start, pos_end), null);
    }
    public Double extend(Obj o) {
        List<Obj> otherval = ((PList) o.alist()).trueValue();
        List<Obj> trueval = trueValue();
        trueval.addAll(otherval);
        return new Double(
                new PList(trueval).set_context(context).set_pos(pos_start, pos_end),
                null
        );
    }
    public Double pop(Obj o) {
        if (!(o.number() instanceof Num)) return new Double(null, new RTError(
                pos_start, pos_end,
                "List index must be a number!",
                context
        ));
        Num other = (Num) o.number();
        if (other.floating())
            return new Double(null, new RTError(
                    pos_start, pos_end,
                    "List index must be int, not float",
                    context
            ));
        if (other.trueValue() + 1 > trueValue().size()) return new Double(null, new RTError(
                pos_start, pos_end,
                "List index out of range",
                context
        ));
        ArrayList<Obj> x = new ArrayList<>(trueValue());
        x.remove(x.get(Math.round(other.trueValue())));
        return new Double(
                new PList(x).set_context(context).set_pos(pos_start, pos_end),
                null
        );
    }
    public Double add(Obj o) { return extend(o); }
    public Double mod(Obj o) { return append(o); }
    public Double remove(Obj o) {
        ArrayList<Obj> updated = new ArrayList<>(trueValue());
        updated.removeIf(value -> ((Bool)((Double) value.getattr("eq", o)).get(0)).trueValue());
        return new Double(new PList(updated).set_context(context).set_pos(pos_start, pos_end), null);
    }

    public Double eq(Obj o) {
        if (!(o instanceof PList)) return new Double(new Bool(false), null);
        return new Double(new Bool(this.trueValue().equals(((PList) o).trueValue())), null);
    }


    // Conversions

    public Value dictionary() { return new Dict(new HashMap<>(){{
        List<Obj> trueval = trueValue();
        int size = trueval.size();
        for (int i = 0; i < size; i++) {
            Obj x = trueval.get(i);
            put(x, x);
        }
    }}).set_context(context).set_pos(pos_start, pos_end); }
    public Value number() { return new Num(trueValue().size())
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value function() { return new Function(null, new ListNode((List<Node>) value, pos_start, pos_end), null)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value alist() { return new PList(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Value bool() { return new Bool(trueValue().size() > 0)
            .set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public Value copy() { return new PList(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Value type() { return new Str("list").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return Arrays.deepToString(((ArrayList<Object>) value).toArray()); }

}
