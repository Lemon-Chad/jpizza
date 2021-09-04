package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Function extends BaseFunction {
    Node bodyNode;
    public List<String> argNames;
    List<String> argTypes;
    List<Obj> defaults;
    int defaultCount;
    boolean async;
    boolean autoreturn;
    String returnType;
    boolean catcher = false;

    public Function(String name, Node bodyNode, List<String> argNames, List<String> argTypes,
                    boolean async, boolean autoreturn, String returnType, List<Obj> defaults, int defaultCount) {
        super(name);
        this.defaultCount = defaultCount;
        this.bodyNode = bodyNode;
        this.argNames = argNames != null ? argNames : new ArrayList<>();
        this.async = async; this.autoreturn = autoreturn;
        this.argTypes = argTypes != null ? argTypes : new ArrayList<>();
        this.returnType = returnType;
        this.defaults = defaults;
        jptype = Constants.JPType.Function;
    }

    public Function(String name, Node bodyNode, List<String> argNames, List<String> argTypes, String returnType,
                    List<Obj> defaults, int defaultCount) {
        super(name);
        this.defaultCount = defaultCount;
        this.bodyNode = bodyNode;
        this.argNames = argNames != null ? argNames : new ArrayList<>();
        this.argTypes = argTypes != null ? argTypes : new ArrayList<>();
        this.async = false; this.autoreturn = true;
        this.returnType = returnType;
        this.defaults = defaults;
        jptype = Constants.JPType.Function;
    }

    public Function(String name, Node bodyNode, List<String> argNames) {
        super(name);
        this.bodyNode = bodyNode;
        this.argNames = argNames != null ? argNames : new ArrayList<>();
        this.argTypes = new ArrayList<>();
        this.defaultCount = 0;
        for (int i = 0; i < this.argNames.size(); i++) {
            this.argTypes.add("any");
            this.defaults.add(null);
        }
        this.returnType = "any";
        this.async = false; this.autoreturn = true;
        jptype = Constants.JPType.Function;
    }

    // Functions

    public Function setCatch(boolean c) {
        this.catcher = c;
        return this;
    }

    public RTResult execute(List<Obj> args, Interpreter parent) {
        return ifCatcher(args, parent);
    }

    public RTResult ifCatcher(List<Obj> args, Interpreter parent) {
        RTResult res = run(args, parent);
        if (catcher) {
            if (res.error != null)
                return res.success(new Result(res.error.details));
            else
                return res.success(new Result(res.value));
        } return res;
    }

    public RTResult run(List<Obj> args, Interpreter parent) {
        RTResult res = new RTResult();
        Interpreter interpreter = new Interpreter(parent.memo);
        Context execCtx = newContext();

        res.register(checkPopArgs(argNames, argTypes, args, execCtx, defaults,
                argNames.size() - defaultCount, argNames.size()));
        if (res.shouldReturn()) {
            if (async && res.error != null)
                Shell.logger.outln(String.format("Async function %s:\n%s", name, res.error.asString()));
            return res;
        }

        Obj value = res.register(interpreter.visit(bodyNode, execCtx));
        if (res.shouldReturn() && res.funcReturn == null) {
            if (async && res.error != null)
                Shell.logger.outln(String.format("Async function %s:\n%s", name, res.error.asString()));
            return res;
        }

        Obj retValue = autoreturn ? value : (
                    res.funcReturn != null ? res.funcReturn : new Null()
                );

        if (!returnType.equals("any")) {
            Obj type = retValue.type().astring();
            if (type.jptype != Constants.JPType.String) return res.failure(new RTError(
                    pos_start, pos_end,
                    "Return value type is not a string",
                    context
            ));

            String rtype = ((Str) type).trueValue();
            if (!rtype.equals(returnType)) return res.failure(new RTError(
                    pos_start, pos_end,
                    "Return value has mismatched type",
                    context
            ));
        }

        return res.success(retValue.set_context(context));
    }

    // Methods

    // Conversions

    public Obj alist() {
        List<Obj> argNames = new ArrayList<>();
        int size = this.argNames.size();
        for (int i = 0; i < size; i++)
            argNames.add(new Str(this.argNames.get(i)).set_pos(pos_start).set_context(context));
        return new PList(new ArrayList<>(argNames));
    }
    public Obj astring() { return new Str(name).set_pos(pos_start, pos_end).set_context(context); }
    public Obj dictionary() {
        Map<Obj, Obj> map = new HashMap<>();
        int size = this.argNames.size();
        for (int i = 0; i < size; i++) {
            Obj s = new Str(this.argNames.get(i)).set_pos(pos_start).set_context(context);
            map.put(s, s);
        }
        return new Dict(map).set_pos(pos_start, pos_end).set_context(context);
    }
    public Obj number() { return new Num(argNames.size()).set_pos(pos_start, pos_end).set_context(context); }
    public Obj bool() { return new Bool(true).set_pos(pos_start, pos_end).set_context(context); }

    // Defaults

    public Obj copy() { return new Function(name, bodyNode, argNames, argTypes, async, autoreturn, returnType, defaults,
                                            defaultCount).setCatch(catcher)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("function").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "<function-"+name+">"; }
    public boolean isAsync() { return async; }

}
