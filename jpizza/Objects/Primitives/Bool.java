package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Nodes.Values.BooleanNode;
import lemon.jpizza.Objects.Executables.BaseFunction;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Double;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.HashMap;

import static lemon.jpizza.Tokens.TT_BOOL;

public class Bool extends Value {
    public Bool(boolean value) {super(value);}

    public boolean trueValue() { return (boolean) value; }

    // Methods

    public Double invert() { return new Double(new Bool(!(boolean)value), null);}
    public Double also(Obj o) {
        Value other = (Value) o.bool();
        return new Double(new Bool((boolean) this.value || (boolean) other.value), null);
    }
    public Double including(Obj o) {
        Value other = (Value) o.bool();
        return new Double(new Bool((boolean) this.value && (boolean) other.value), null);
    }

    public Double eq(Obj o) {
        if (!(o instanceof Bool)) return new Double(new Bool(false), null);
        return new Double(new Bool(this.trueValue() == ((Bool) o).trueValue()), null);
    }

    // Conversions

    public Value dictionary() {
        Value thisaround = this;
        return new Dict(new HashMap<>(){{ put(thisaround, thisaround); }})
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value alist() {
        Value thisaround = this;
        return new PList(new ArrayList<>() {{ add(thisaround); }}).set_context(context).set_pos(pos_start, pos_end); }
    public Value number() { return new Num((boolean) value ? 1 : 0)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value bool() { return new Bool(trueValue()).set_context(context).set_pos(pos_start, pos_end); }
    public Value function() { return new Function(null, new BooleanNode(new Token(TT_BOOL, value, pos_start, pos_end)), null)
            .set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public Value type() { return new Str("bool").set_context(context).set_pos(pos_start, pos_end); }
    public Value copy() { return new Bool(trueValue()).set_context(context).set_pos(pos_start, pos_end); }

}
