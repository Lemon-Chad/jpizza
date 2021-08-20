package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Objects.Executables.ClassInstance;
import lemon.jpizza.Pair;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.NullNode;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnumJChild extends Value {
    public EnumJ parent;
    public int val;
    List<String> params;


    public EnumJChild(int val, List<String> params) {
        this.val = val;
        this.params = params;

        jptype = Constants.JPType.EnumChild;
    }

    // Functions

    public RTResult instance(Context parent, List<Obj> args) {
        Context ctx = new Context(this.parent.name, parent, pos_start);
        ctx.symbolTable = new SymbolTable();
        if (args.size() != params.size()) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                String.format("Expected %s args, got %s", params.size(), args.size()),
                parent
        ));
        ctx.symbolTable.define("$child", val);
        ctx.symbolTable.define("$parent", this.parent.name);
        for (int i = 0; i < args.size(); i++)
            ctx.symbolTable.define(params.get(i), args.get(i));
        return new RTResult().success(new ClassInstance(ctx));
    }

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

    // Defaults

    public String toString() { return parent.name + "::" + val; }
    public Obj type() { return new Str(parent.name).set_context(context).set_pos(pos_start, pos_end); }
    public Obj copy() { return new EnumJChild(val, params).setParent(parent)
                                    .set_context(context).set_pos(pos_start, pos_end); }

}
