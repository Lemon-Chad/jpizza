package lemon.jpizza.objects.executables;

import lemon.jpizza.Constants;
import lemon.jpizza.Position;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.*;
import lemon.jpizza.objects.Value;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
public class BaseFunction extends Value {
    public final String name;
    public BaseFunction(String name) {
        super();
        this.name = name != null ? name : "<anonymous>";
        jptype = Constants.JPType.BaseFunction;
    }

    // Functions

    public Context newContext() {
        Context newContext = new Context(name, context, pos_start);
        newContext.symbolTable = new SymbolTable(context != null ? context.symbolTable : null);
        return newContext;
    }

    public RTResult checkArgs(List<Obj> defaults, List<List<String>> argTypes, HashMap<String, String> genericKey,
                              List<Obj> args, int minArgs, int maxArgs) {
        RTResult res = new RTResult();

        int size = args.size();
        if (size > maxArgs) return res.failure(RTError.ArgumentCount(
                pos_start, pos_end,
                String.format("%s too many args passed into '%s'", args.size() - maxArgs, name),
                context
        ));
        if (size < minArgs) return res.failure(RTError.ArgumentCount(
                pos_start, pos_end,
                String.format("%s too few args passed into '%s'", minArgs - args.size(), name),
                context
        ));

        int tSize = argTypes.size();
        for (int i = 0; i < tSize; i++) {
            String type = context.symbolTable.getType(argTypes.get(i));
            String generictype = genericKey.get(type);
            if (type.equals("any") || (generictype != null && generictype.equals("any"))) continue;

            Obj arg;
            if (i >= size) {
                arg = defaults.get(i - size);
            }
            else {
                arg = args.get(i);
            }

            Obj oType = arg.type().astring();
            if (oType.jptype != Constants.JPType.String) return res.failure(RTError.Type(
                    arg.get_start(), arg.get_end(),
                    "Type is not a string",
                    arg.get_ctx()
            ));

            String oT = oType.string;
            if (!oT.equals(type) && (generictype == null || !generictype.equals(oT))) return res.failure(RTError.Type(
                    arg.get_start(), arg.get_end(),
                    String.format("Expected type %s, got %s", generictype != null ? generictype: type, oT),
                    arg.get_ctx()
            ));

        }

        return res.success(null);
    }

    public static RTResult inferGenerics(List<Obj> args, List<List<String>> types, List<String> generics, HashMap<String, String> genericKey,
                                         @SuppressWarnings("unused") @NotNull Position pos_start,
                                         @SuppressWarnings("unused") @NotNull Position pos_end, Context ctx) {
        RTResult res = new RTResult();

        int len = Math.min(args.size(), types.size());
        for (int i = 0; i < len; i++) {
            String expect = ctx.symbolTable.getType(types.get(i));
            if (generics.contains(expect) && !genericKey.containsKey(expect)) {
                String actual = args.get(i).type().toString();
                genericKey.put(expect, actual);
            }
        }

        return res.success(null);
    }

    public void populateArgs(List<String> argNames, List<Obj> args, List<Obj> defaults, Context execCtx) {
        int size = argNames.size();
        int aSize = args.size();
        for (int i = 0; i < size; i++) {
            Obj argValue;
            if (i >= aSize)
                argValue = defaults.get(i - aSize);
            else
                argValue = args.get(i);
            argValue.set_context(execCtx);
            String argName = argNames.get(i);
            execCtx.symbolTable.define(argName, argValue);
        }
    }

    public RTResult checkPopArgs(List<String> argNames, List<List<String>> argTypes, List<Obj> args, Context execCtx,
                                 List<Obj> defaults, int minArgs, int maxArgs, HashMap<String, String> genericKey) {
        RTResult res = new RTResult();
        res.register(checkArgs(defaults, argTypes, genericKey, args, minArgs, maxArgs));
        if (res.shouldReturn())
            return res;
        populateArgs(argNames, args, defaults, execCtx);
        if (res.shouldReturn())
            return res;
        return res.success(null);
    }


    // Methods

    // Conversions

    // Defaults

    public Obj copy() { return new BaseFunction(name).set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("<base-function>").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "<base-function>"; }
    public boolean isAsync() { return false; }
    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        return new RTResult().success(new Null());
    }
}
