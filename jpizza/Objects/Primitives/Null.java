package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Double;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.NullNode;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.HashMap;

import static lemon.jpizza.Tokens.TT_KEYWORD;

public class Null extends Value {
    public Null() {super(null);}

    // Functions

    // Methods

    // Conversions

    public Value dictionary() { return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end); }
    public Value number() { return new Num(0)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value function() { return new Function(
            null, new NullNode(new Token(TT_KEYWORD, "null", pos_start, pos_end)), null)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value bool() { return new Bool(false)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value alist() { return new PList(new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end); }

    public Double<Obj, RTError> eq(Obj o) {
        if (!(o instanceof Null)) return new Double<>(new Bool(false), null);
        return new Double<>(new Bool(true), null);
    }

    // Defaults

    public Value copy() { return new Null().set_context(context).set_pos(pos_start, pos_end); }
    public Value type() { return new Str("null").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "null"; }

}
