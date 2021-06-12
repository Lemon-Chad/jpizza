package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.NumberNode;
import lemon.jpizza.Objects.Executables.BaseFunction;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Token;
import lemon.jpizza.Double;

import java.util.Collections;
import java.util.HashMap;

import static lemon.jpizza.Tokens.*;

public class Num extends Value {
    public Num(float value) {super(value);}
    public float trueValue() { return (float) value; }

    // Functions

    public boolean floating() {
        return Math.round((float) value) != (float) value;
    }

    // Methods

    public Double add(Obj o) {
        Num other = (Num) o.number();
        return new Double(new Num(other.trueValue() + trueValue()).set_context(context), null);
    }
    public Double mod(Obj o) {
        Num other = (Num) o.number();
        return new Double(new Num(other.trueValue() % trueValue()).set_context(context), null);
    }
    public Double sub(Obj o) {
        Num other = (Num) o.number();
        return new Double(new Num(trueValue() - other.trueValue()).set_context(context), null);
    }
    public Double mul(Obj o) {
        Num other = (Num) o.number();
        return new Double(new Num(other.trueValue() * trueValue()).set_context(context), null);
    }
    public Double div(Obj o) {
        Num other = (Num) o.number();
        if (other.trueValue() == 0)
            return new Double(null, new RTError(
                    pos_start, pos_end,
                    "Division by 0",
                    context
            ));
        return new Double(new Num(trueValue() / other.trueValue()).set_context(context), null);
    }
    public Double fastpow(Obj o) {
        Num other = (Num) o.number();
        return new Double(new Num((float) Math.pow(trueValue(), other.trueValue())).set_context(context), null);
    }
    public Double lt(Obj o) {
        Num other = (Num) o.number();
        return new Double(new Bool(trueValue() < other.trueValue()).set_context(context), null);
    }
    public Double lte(Obj o) {
        Num other = (Num) o.number();
        return new Double(new Bool(trueValue() <= other.trueValue()).set_context(context), null);
    }
    public Double invert() {
        return new Double(new Num(-trueValue()).set_context(context), null);
    }

    public Double eq(Obj o) {
        if (!(o instanceof Num)) return new Double(new Bool(false), null);
        return new Double(new Bool(this.trueValue() == ((Num) o).trueValue()), null);
    }

    // Conversions

    public Value dictionary() {
        Value thisaround = this;
        return new Dict(new HashMap<>(){{ put(thisaround, thisaround); }}).set_context(context)
                .set_pos(pos_start, pos_end);
    }
    public Value function() { return new Function(
            null, new NumberNode(new Token(floating() ? TT_FLOAT : TT_INT, trueValue(), pos_start, pos_end)), null)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value number() { return new Num(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Value bool() { return new Bool(trueValue() > 0)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value alist() { return new PList(Collections.singletonList(this)).set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public Value copy() { return new Num(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Value type() { return new Str("num").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return String.valueOf(trueValue()); }

}
