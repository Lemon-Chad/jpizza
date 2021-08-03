package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.NumberNode;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Token;
import lemon.jpizza.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static lemon.jpizza.Tokens.*;

public class Num extends Value {
    public Num(double v) {
        value = v;
        jptype = Constants.JPType.Number;
    }
    public double trueValue() { return (double) value; }

    // Functions

    public boolean floating() {
        return Math.round((double) value) != (double) value;
    }

    // Methods

    public Pair<Obj, RTError> add(Obj o) {
        Num other = (Num) o.number();
        double v = other.trueValue() + trueValue();
        return new Pair<>(new Num(v).set_context(context), null);
    }
    public Pair<Obj, RTError> add(Num o) {
        return new Pair<>(new Num(o.trueValue() + trueValue()).set_context(context), null);
    }
    public Pair<Obj, RTError> mod(Obj o) {
        Num other = (Num) o.number();
        return new Pair<>(new Num(other.trueValue() % trueValue()).set_context(context), null);
    }
    public Pair<Obj, RTError> sub(Obj o) {
        Num other = (Num) o.number();
        return new Pair<>(new Num(trueValue() - other.trueValue()).set_context(context), null);
    }
    public Pair<Obj, RTError> mul(Obj o) {
        Num other = (Num) o.number();
        return new Pair<>(new Num(other.trueValue() * trueValue()).set_context(context), null);
    }
    public Pair<Obj, RTError> div(Obj o) {
        Num other = (Num) o.number();
        if (other.trueValue() == 0)
            return new Pair<>(null, new RTError(
                    pos_start, pos_end,
                    "Division by 0",
                    context
            ));
        return new Pair<>(new Num(trueValue() / other.trueValue()).set_context(context), null);
    }
    public Pair<Obj, RTError> fastpow(Obj o) {
        Num other = (Num) o.number();
        return new Pair<>(new Num(Math.pow(trueValue(), other.trueValue())).set_context(context), null);
    }
    public Pair<Obj, RTError> lt(Obj o) {
        Num other = (Num) o.number();
        return new Pair<>(new Bool(trueValue() < other.trueValue()).set_context(context), null);
    }
    public Pair<Obj, RTError> lte(Obj o) {
        Num other = (Num) o.number();
        return new Pair<>(new Bool(trueValue() <= other.trueValue()).set_context(context), null);
    }
    public Pair<Obj, RTError> invert() {
        return new Pair<>(new Num(-trueValue()).set_context(context), null);
    }

    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != Constants.JPType.Number) return new Pair<>(new Bool(false), null);
        return new Pair<>(new Bool(this.trueValue() == ((Num) o).trueValue()), null);
    }

    // Conversions

    public Obj dictionary() {
        Value thisaround = this;
        return new Dict(new HashMap<>(){{ put(thisaround, thisaround); }}).set_context(context)
                .set_pos(pos_start, pos_end);
    }
    public Obj function() { return new Function(
            null, new NumberNode(new Token(floating() ? TT.FLOAT : TT.INT, trueValue(), pos_start, pos_end)), null)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj number() { return this; }
    public Obj bool() { return new Bool(trueValue() > 0)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj alist() { return new PList(new ArrayList<>(Collections.singletonList(this))).set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public Obj copy() { return new Num(trueValue())
                                        .set_context(context)
                                        .set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("num").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() {
        if (!floating()) return String.valueOf((long) trueValue());
        return String.valueOf(trueValue());
    }

}
