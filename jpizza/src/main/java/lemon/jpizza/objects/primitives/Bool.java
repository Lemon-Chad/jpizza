package lemon.jpizza.objects.primitives;

import lemon.jpizza.JPType;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.nodes.values.BooleanNode;
import lemon.jpizza.objects.executables.Function;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.Value;
import lemon.jpizza.Pair;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.HashMap;

import static lemon.jpizza.Tokens.TT;

public class Bool extends Value {
    public Bool(boolean value) {
        super(value);
        jptype = JPType.Boolean;
        boolval = value;
    }

    // Methods

    @SuppressWarnings("unused")
    public Pair<Obj, RTError> invert() { return new Pair<>(new Bool(!(boolean)value), null);}
    @SuppressWarnings("unused")
    public Pair<Obj, RTError> also(Obj o) {
        if ((boolean) this.value) return new Pair<>(new Bool(true), null);
        return new Pair<>(o.bool(), null);
    }
    @SuppressWarnings("unused")
    public Pair<Obj, RTError> including(Obj o) {
        if (!(boolean) this.value) return new Pair<>(new Bool(false), null);
        return new Pair<>(o.bool(), null);
    }

    @SuppressWarnings("unused")
    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != JPType.Boolean) return new Pair<>(new Bool(false), null);
        return new Pair<>(new Bool(this.boolval == o.boolval), null);
    }

    // Conversions

    @SuppressWarnings("unused")
    public Obj dictionary() {
        Value thisaround = this;
        return new Dict(new HashMap<>(){{ put(thisaround, thisaround); }})
            .set_context(context).set_pos(pos_start, pos_end); }
    @SuppressWarnings("unused")
    public Obj alist() {
        Value thisaround = this;
        return new PList(new ArrayList<>() {{ add(thisaround); }}).set_context(context).set_pos(pos_start, pos_end); }
    @SuppressWarnings("unused")
    public Obj number() { return new Num((boolean) value ? 1 : 0)
            .set_context(context).set_pos(pos_start, pos_end); }
    @SuppressWarnings("unused")
    public Obj bool() { return new Bool(boolval).set_context(context).set_pos(pos_start, pos_end); }
    @SuppressWarnings("unused")
    public Obj function() { return new Function(null, new BooleanNode(new Token(TT.BOOL, value, pos_start, pos_end)), null)
            .set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    @SuppressWarnings("unused")
    public Obj type() { return new Str("bool").set_context(context).set_pos(pos_start, pos_end); }
    @SuppressWarnings("unused")
    public Obj copy() { return new Bool(boolval).set_context(context).set_pos(pos_start, pos_end); }

}
