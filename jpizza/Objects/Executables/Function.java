package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Memo;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Results.RTResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Function extends BaseFunction {
    Node bodyNode;
    List<String> argNames;
    boolean async;
    boolean autoreturn;
    public Function(String name, Node bodyNode, List<String> argNames, boolean async, boolean autoreturn) {
        super(name);
        this.bodyNode = bodyNode;
        this.argNames = argNames != null ? argNames : new ArrayList<>();
        this.async = async; this.autoreturn = autoreturn;
    }

    public Function(String name, Node bodyNode, List<String> argNames) {
        super(name);
        this.bodyNode = bodyNode;
        this.argNames = argNames != null ? argNames : new ArrayList<>();
        this.async = false; this.autoreturn = true;
    }

    // Functions

    public RTResult execute(List<Obj> args, Interpreter parent) {
        RTResult res = new RTResult();
        Interpreter interpreter = new Interpreter(parent.memo, parent.memoize);
        Context execCtx = newContext();

        res.register(checkPopArgs(argNames, args, execCtx));
        if (res.shouldReturn()) {
            if (async && res.error != null)
                System.out.printf("Async function %s:\n%s%n", name, res.error.asString());
            return res;
        }

        Obj value = (Obj) res.register(interpreter.visit(bodyNode, execCtx));
        if (res.shouldReturn() && res.funcReturn == null) {
            if (async && res.error != null)
                System.out.printf("Async function %s:\n%s%n", name, res.error.asString());
            return res;
        }

        Obj retValue = autoreturn ? value : (
                    res.funcReturn != null ? (Obj) res.funcReturn : new Null()
                );
        return res.success(retValue);
    }

    // Methods

    // Conversions

    public Value alist() {
        List<Obj> argNames = new ArrayList<>();
        int size = this.argNames.size();
        for (int i = 0; i < size; i++)
            argNames.add(new Str(this.argNames.get(i)).set_pos(pos_start).set_context(context));
        return new PList(argNames);
    }
    public Value astring() { return new Str(name).set_pos(pos_start, pos_end).set_context(context); }
    public Value dictionary() {
        Map<Obj, Obj> map = new HashMap<>();
        int size = this.argNames.size();
        for (int i = 0; i < size; i++) {
            Obj s = new Str(this.argNames.get(i)).set_pos(pos_start).set_context(context);
            map.put(s, s);
        }
        return new Dict(map).set_pos(pos_start, pos_end).set_context(context);
    }
    public Value number() { return new Num(argNames.size()).set_pos(pos_start, pos_end).set_context(context); }
    public Value bool() { return new Bool(true).set_pos(pos_start, pos_end).set_context(context); }

    // Defaults

    public Obj copy() { return new Function(name, bodyNode, argNames, async, autoreturn)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("<function>").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "<function-"+name+">"; }
    public boolean isAsync() { return async; }

}
