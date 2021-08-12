package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.Pair;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.StringNode;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.HashMap;

import static lemon.jpizza.Tokens.TT;
import lemon.jpizza.Objects.Executables.Function;

public class Str extends Value {
    public Str(String value) { super(value); jptype = Constants.JPType.String; }
    public String trueValue() { return value.toString(); }

    // Functions

    // Methods

    public Pair<Obj, RTError> mul(Obj o) {
        Num other = (Num) o.number();
        if (other.floating)
            return new Pair<>(null, new RTError(
                    pos_start, pos_end,
                    "Expected long",
                    context
            ));
        return new Pair<>(new Str(trueValue().repeat(Math.toIntExact(Math.round(other.trueValue())))).set_context(context), null);
    }
    public Pair<Obj, RTError> add(Obj o) {
        Str other = (Str) o.astring();
        return new Pair<>(
                new Str(String.format("%s%s", trueValue(), other.trueValue())).set_context(context), null
        );
    }

    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != Constants.JPType.String) return new Pair<>(new Bool(false), null);
        return new Pair<>(new Bool(this.trueValue().equals(((Str) o).trueValue())), null);
    }

    // Conversions

    public Obj dictionary() {
        Value thisaround = this;
        return new Dict(new HashMap<>(){{
            put(thisaround, thisaround);
        }}).set_context(context).set_pos(pos_start, pos_end);
    }
    public Obj number() { return new Num(trueValue().length())
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj function() { return new Function(null, new StringNode(
            new Token(TT.STRING, trueValue(), pos_start, pos_end)), null)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj bool() { return new Bool(trueValue().length() > 0)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj alist() {
        ArrayList<Obj> chars = new ArrayList<>();
        char[] charArray = trueValue().toCharArray();
        int length = charArray.length;
        for (int i = 0; i < length; i++) {
            chars.add(new Str(String.valueOf(charArray[i])).set_context(context).set_pos(pos_start, pos_end));
        } return new PList(chars).set_context(context).set_pos(pos_start, pos_end);
    }

    // Defaults

    public Obj copy() { return new Str(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("String").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return value.toString(); }

}
