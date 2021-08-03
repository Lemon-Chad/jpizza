package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.Pair;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.NullNode;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens;

import java.util.ArrayList;
import java.util.HashMap;

public class EnumJChild extends Value {
    EnumJ parent;
    int val;

    public EnumJChild(int val) {
        this.val = val;

        jptype = Constants.JPType.EnumChild;
    }

    // Functions

    public EnumJChild setParent(EnumJ parent) {
        this.parent = parent;
        return this;
    }

    // Methods

    public Pair<Obj, RTError> eq(Obj o) {
        if (o.jptype != Constants.JPType.EnumChild) return new Pair<>(new Bool(false), null);
        EnumJChild other = (EnumJChild) o;
        if (!((Bool)other.parent.eq(parent).a).trueValue()) return new Pair<>(new Bool(false), null);
        return new Pair<>(new Bool(other.val == val), null);
    }

    // Conversions

    public Obj dictionary() {
        Value thisaround = this;
        return new Dict(new HashMap<>(){{ put(thisaround, thisaround); }})
                .set_context(context).set_pos(pos_start, pos_end);
    }

    public Obj alist() {
        Value thisaround = this;
        return new PList(new ArrayList<>() {{ add(thisaround); }}).set_context(context).set_pos(pos_start, pos_end);
    }

    public Obj number() {
        return new Num(val)
                .set_context(context).set_pos(pos_start, pos_end);
    }

    public Obj bool() { return new Bool(val > 0).set_context(context).set_pos(pos_start, pos_end); }

    public Obj function() {
        return new Function(null, new NullNode(new Token(Tokens.TT.KEYWORD, "null")), null)
                .set_context(context).set_pos(pos_start, pos_end);
    }

    // Defaults

    public String toString() { return parent.name + "::" + val; }
    public Obj type() { return new Str(parent.name).set_context(context).set_pos(pos_start, pos_end); }
    public Obj copy() { return new EnumJChild(val).setParent(parent).set_context(context).set_pos(pos_start, pos_end); }

}
