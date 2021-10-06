package lemon.jpizza.objects.primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.Pair;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.nodes.values.NullNode;
import lemon.jpizza.objects.executables.Function;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.Value;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens.TT;

import java.util.ArrayList;
import java.util.HashMap;

public class Null extends Value {
    public Null() { super(null); jptype = Constants.JPType.Null; }

    // Functions

    // Methods

    // Conversions

    public Obj dictionary() { return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj number() { return new Num(0).set_context(context).set_pos(pos_start, pos_end); }
    public Obj function() { return new Function(
            null, new NullNode(new Token(TT.KEYWORD, "null", pos_start, pos_end)), null)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj bool() { return new Bool(false)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj alist() { return new PList(new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end); }

    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != Constants.JPType.Null) return new Pair<>(new Bool(false), null);
        return new Pair<>(new Bool(true), null);
    }

    // Defaults

    public Obj copy() { return new Null().set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("null").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "null"; }

}