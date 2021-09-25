package lemon.jpizza.objects.primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.objects.executables.ClassInstance;
import lemon.jpizza.Pair;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.Value;
import lemon.jpizza.results.RTResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EnumJChild extends Value {
    public EnumJ parent;
    public final int val;
    final List<String> params;
    final List<String> types;


    public EnumJChild(int val, List<String> params, List<String> types) {
        this.val = val;
        this.params = params;
        this.types = types;

        jptype = Constants.JPType.EnumChild;
    }

    // Functions

    public RTResult instance(Context parent, List<Obj> args) {
        Context ctx = new Context(this.parent.name, parent, pos_start);
        ctx.symbolTable = new SymbolTable();

        if (args.size() != params.size()) return new RTResult().failure(RTError.ArgumentCount(
                pos_start, pos_end,
                String.format("Expected %s args, got %s", params.size(), args.size()),
                parent
        ));

        int tSize = types.size();
        for (int i = 0; i < tSize; i++) {
            String type = types.get(i);
            if (type.equals("any")) continue;

            Obj arg = args.get(i);

            Obj oType = arg.type().astring();
            if (oType.jptype != Constants.JPType.String) return new RTResult().failure(RTError.Type(
                    arg.get_start(), arg.get_end(),
                    "Type is not a string",
                    arg.get_ctx()
            ));

            String oT = oType.string;
            if (!oT.equals(type)) return new RTResult().failure(RTError.Type(
                    arg.get_start(), arg.get_end(),
                    String.format("Expected type %s, got %s", type, oT),
                    arg.get_ctx()
            ));

        }

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
        if (other.parent != parent) return new Pair<>(new Bool(false), null);
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
    public Obj copy() { return new EnumJChild(val, params, types).setParent(parent)
                                    .set_context(context).set_pos(pos_start, pos_end); }

}
