package lemon.jpizza.objects.executables;

import lemon.jpizza.Constants;
import lemon.jpizza.Token;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.Pair;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.Value;
import lemon.jpizza.objects.primitives.*;
import lemon.jpizza.results.RTResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EnumJChild extends Value {
    public EnumJ parent;
    public final int val;
    final List<String> params;
    final List<List<String>> types;
    final List<String> generics;

    public EnumJChild(int val, List<String> params, List<List<String>> types, List<String> generics) {
        this.val = val;
        this.params = params;
        this.types = types;
        this.generics = generics;

        jptype = Constants.JPType.EnumChild;
    }

    // Functions

    public RTResult instance(Context parent, List<Obj> args, List<Token> gens) {
        Context ctx = new Context(this.parent.name, parent, pos_start);
        ctx.symbolTable = new SymbolTable();

        HashMap<String, String> genericKey = new HashMap<>();
        BaseFunction.inferGenerics(args, types, generics, genericKey, pos_start, pos_end, ctx);

        if (args.size() != params.size()) return new RTResult().failure(RTError.ArgumentCount(
                pos_start, pos_end,
                String.format("Expected %s args, got %s", params.size(), args.size()),
                parent
        ));

        if (gens.size() + genericKey.size() < generics.size() || gens.size() > generics.size())
            return new RTResult().failure(RTError.ArgumentCount(
                pos_start, pos_end,
                String.format("Expected %s generics, got %s", generics.size(), gens.size()),
                parent
        ));

        for (int i = genericKey.size(); i < generics.size(); i++)
            genericKey.put(generics.get(i), gens.get(i).value.toString());

        int tSize = types.size();
        for (int i = 0; i < tSize; i++) {
            String type = ctx.symbolTable.getType(types.get(i));
            if (genericKey.containsKey(type))
                type = genericKey.get(type);
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

        for (int i = 0; i < args.size(); i++)
            ctx.symbolTable.define(params.get(i), args.get(i));

        StringBuilder genericAddition = new StringBuilder();
        if (genericKey.size() > 0) {
            genericAddition.append("(");
            for (String key : generics) {
                genericAddition.append(genericKey.get(key)).append(",");
            }
            genericAddition.setLength(genericAddition.length() - 1);
            genericAddition.append(")");
        }

        ctx.displayName = this.parent.name + genericAddition;
        return new RTResult().success(new ClassInstance(ctx, this.parent.name));
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
    public Obj copy() { return new EnumJChild(val, params, types, generics).setParent(parent)
                                    .set_context(context).set_pos(pos_start, pos_end); }

}
