package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Double;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.StringNode;
import lemon.jpizza.Objects.Executables.BaseFunction;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.HashMap;

import static lemon.jpizza.Tokens.TT_STRING;
import lemon.jpizza.Objects.Executables.Function;

public class Str extends Value {
    public Str(String value) {super(value);}
    public String trueValue() { return value.toString(); }

    // Functions

    // Methods

    public Double mul(Obj o) {
        Num other = (Num) o.number();
        if (other.floating())
            return new Double(null, new RTError(
                    pos_start, pos_end,
                    "Expected long",
                    context
            ));
        return new Double(new Str(trueValue().repeat(Math.toIntExact(Math.round(other.trueValue())))).set_context(context), null);
    }
    public Double add(Obj o) {
        Str other = (Str) o.astring();
        return new Double(
                new Str(String.format("%s%s", trueValue(), other.trueValue())).set_context(context), null
        );
    }

    public Double eq(Obj o) {
        if (!(o instanceof Str)) return new Double(new Bool(false), null);
        return new Double(new Bool(this.trueValue().equals(((Str) o).trueValue())), null);
    }

    // Conversions

    public Value dictionary() {
        Value thisaround = this;
        return new Dict(new HashMap<>(){{
            put(thisaround, thisaround);
        }}).set_context(context).set_pos(pos_start, pos_end);
    }
    public Value number() { return new Num(trueValue().length())
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value function() { return new Function(null, new StringNode(
            new Token(TT_STRING, trueValue(), pos_start, pos_end)), null)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value bool() { return new Bool(trueValue().length() > 0)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value alist() {
        ArrayList<Obj> chars = new ArrayList<>();
        char[] charArray = trueValue().toCharArray();
        int length = charArray.length;
        for (int i = 0; i < length; i++) {
            chars.add(new Str(String.valueOf(charArray[i])).set_context(context).set_pos(pos_start, pos_end));
        } return new PList(chars).set_context(context).set_pos(pos_start, pos_end);
    }

    // Defaults

    public Value copy() { return new Str(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Value type() { return new Str("String").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return value.toString(); }

}
