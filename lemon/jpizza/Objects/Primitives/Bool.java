package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.BooleanNode;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Pair;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.HashMap;

import static lemon.jpizza.Tokens.TT;

public class Bool extends Value {
    public Bool(boolean value) { super(value); jptype = Constants.JPType.Boolean; }

    public boolean trueValue() { return (boolean) value; }

    // Methods

    public Pair<Obj, RTError> invert() { return new Pair<>(new Bool(!(boolean)value), null);}
    public Pair<Obj, RTError> also(Obj o) {
        Value other = (Value) o.bool();
        return new Pair<>(new Bool((boolean) this.value || (boolean) other.value), null);
    }
    public Pair<Obj, RTError> including(Obj o) {
        Value other = (Value) o.bool();
        return new Pair<>(new Bool((boolean) this.value && (boolean) other.value), null);
    }

    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != Constants.JPType.Boolean) return new Pair<>(new Bool(false), null);
        return new Pair<>(new Bool(this.trueValue() == ((Bool) o).trueValue()), null);
    }

    // Conversions

    public Obj dictionary() {
        Value thisaround = this;
        return new Dict(new HashMap<>(){{ put(thisaround, thisaround); }})
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj alist() {
        Value thisaround = this;
        return new PList(new ArrayList<>() {{ add(thisaround); }}).set_context(context).set_pos(pos_start, pos_end); }
    public Obj number() { return new Num((boolean) value ? 1 : 0)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj bool() { return new Bool(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj function() { return new Function(null, new BooleanNode(new Token(TT.BOOL, value, pos_start, pos_end)), null)
            .set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public Obj type() { return new Str("bool").set_context(context).set_pos(pos_start, pos_end); }
    public Obj copy() { return new Bool(trueValue()).set_context(context).set_pos(pos_start, pos_end); }

}
