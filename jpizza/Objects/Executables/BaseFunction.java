package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Double;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Values.StringNode;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static lemon.jpizza.Tokens.TT_STRING;

public class BaseFunction extends Value {
    public String name;
    public BaseFunction(String name) {
        super();
        this.name = name != null ? name : "<anonymous>";
    }

    // Functions

    public Context newContext() {
        Context newContext = new Context(name, context, pos_start);
        newContext.symbolTable = new SymbolTable(newContext.parent.symbolTable);
        return newContext;
    }

    public RTResult checkArgs(List<String> argNames, List<Obj> args) {
        RTResult res = new RTResult();

        if (args.size() > argNames.size()) return res.failure(new RTError(
                pos_start, pos_end,
                String.format("%s too many args passed into '%s'", args.size() - argNames.size(), name),
                context
        ));
        if (args.size() < argNames.size()) return res.failure(new RTError(
                pos_start, pos_end,
                String.format("%s too few args passed into '%s'", argNames.size() - args.size(), name),
                context
        ));

        return res.success(null);
    }

    public void populateArgs(List<String> argNames, List<Obj> args, Context execCtx) {
        int size = args.size();
        for (int i = 0; i < size; i++) {
            String argName = argNames.get(i);
            Obj argValue = args.get(i);
            argValue.set_context(execCtx);
            execCtx.symbolTable.set(argName, argValue);
        }
    }

    public RTResult checkPopArgs(List<String> argNames, List<Obj> args, Context execCtx) {
        RTResult res = new RTResult();
        res.register(checkArgs(argNames, args));
        if (res.shouldReturn())
            return res;
        populateArgs(argNames, args, execCtx);
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
    public RTResult execute(List<Obj> args) { return new RTResult().success(new Null()); }
}
