package lemon.jpizza.objects.executables;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.*;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Shell;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Function extends BaseFunction {
    Node bodyNode;
    public List<String> argNames;
    List<String> argTypes;
    List<Token> generics;
    List<Function> preprocessors;
    List<Function> postprocessors;
    List<Obj> defaults;
    String argname;
    String kwargname;
    int defaultCount;
    boolean async;
    boolean autoreturn;
    String returnType;
    boolean catcher = false;

    public Function(String name, Node bodyNode, List<String> argNames, List<String> argTypes,
                    boolean async, boolean autoreturn, String returnType, List<Obj> defaults, int defaultCount,
                    List<Token> generics) {
        super(name);
        this.generics = generics;
        this.defaultCount = defaultCount;
        this.preprocessors = new ArrayList<>();
        this.postprocessors = new ArrayList<>();
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
        this.generics = new ArrayList<>();
        this.preprocessors = new ArrayList<>();
        this.postprocessors = new ArrayList<>();
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
        this.generics = new ArrayList<>();
        this.defaultCount = 0;
        this.preprocessors = new ArrayList<>();
        this.postprocessors = new ArrayList<>();
        for (int i = 0; i < this.argNames.size(); i++) {
            this.argTypes.add("any");
            this.defaults.add(null);
        }
        this.returnType = "any";
        this.async = false; this.autoreturn = true;
        jptype = Constants.JPType.Function;
    }

    // Functions

    public Function processors(List<Function> pre, List<Function> post) {
        this.preprocessors = pre;
        this.postprocessors = post;
        return this;
    }

    public Function addPostProcessor(Function postProcessor) {
        this.postprocessors.add(postProcessor);
        return this;
    }

    public Function addPreProcessor(Function preProcessor) {
        this.preprocessors.add(preProcessor);
        return this;
    }

    public Function setCatch(boolean c) {
        this.catcher = c;
        return this;
    }

    public Function setIterative(String it) {
        argname = it;
        return this;
    }

    public Function setKwargs(String kwargs) {
        kwargname = kwargs;
        return this;
    }

    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        return ifCatcher(args, generics, kwargs, parent);
    }

    public RTResult ifCatcher(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        RTResult res = run(args, generics, kwargs, parent);
        if (catcher) {
            if (res.error != null)
                return res.success(new Result(res.error.error_name, res.error.details));
            else
                return res.success(new Result(res.value));
        } return res;
    }

    public RTResult run(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        RTResult res = new RTResult();
        Interpreter interpreter = new Interpreter(parent.memo);
        Context execCtx = newContext();

        if (generics.size() > this.generics.size()) {
            return res.failure(RTError.GenericCount(
                    generics.get(this.generics.size()).pos_start, generics.get(generics.size() - 1).pos_end,
                    String.format("Got %s too many generic types", generics.size() - this.generics.size()),
                    context
            ));
        } else if (generics.size() < this.generics.size()) {
            return res.failure(RTError.GenericCount(
                    generics.get(0).pos_start, generics.get(generics.size() - 1).pos_end,
                    String.format("Got %s too few generic types", this.generics.size() - generics.size()),
                    context
            ));
        }

        HashMap<String, String> genericKey = new HashMap<>();
        int genericSize = generics.size();
        for (int i = 0; i < genericSize; i++)
            genericKey.put(this.generics.get(i).value.toString(), generics.get(i).value.toString());

        if (argname != null && args.size() > argNames.size()) {
            execCtx.symbolTable.define(argname, new PList(new ArrayList<>(args.subList(argNames.size(), args.size()))));
            args = new ArrayList<>(args.subList(0, argNames.size()));
        }

        if (kwargname != null) {
            Map<Obj, Obj> mp = new HashMap<>();
            for (Map.Entry<String, Obj> entry : kwargs.entrySet()) {
                mp.put(new Str(entry.getKey()), entry.getValue());
            }
            execCtx.symbolTable.define(kwargname, new Dict(mp));
        }

        res.register(checkPopArgs(argNames, argTypes, args, execCtx, defaults,
                argNames.size() - defaultCount, argNames.size(), genericKey));
        if (res.shouldReturn()) {
            if (async && res.error != null)
                Shell.logger.warn(String.format("Async function %s:\n%s", name, res.error.asString()));
            return res;
        }

        for (Map.Entry<String, String> entry : genericKey.entrySet())
            execCtx.symbolTable.define(entry.getKey(), new Str(entry.getValue()));

        for (Function f: this.preprocessors) {
            res.register(((Function) f.copy().set_context(execCtx)).execute(args, generics, kwargs, interpreter));
            if (res.error != null) return res;
        }

        Obj value = res.register(interpreter.visit(bodyNode, execCtx));
        if (res.shouldReturn() && res.funcReturn == null) {
            if (async && res.error != null)
                Shell.logger.warn(String.format("Async function %s:\n%s", name, res.error.asString()));
            return res;
        }

        Obj retValue = autoreturn ? value : (
                    res.funcReturn != null ? res.funcReturn : new Null()
                );

        if (!returnType.equals("any")) {
            Obj type = retValue.type().astring();
            if (type.jptype != Constants.JPType.String) return res.failure(RTError.Type(
                    pos_start, pos_end,
                    "Return value type is not a String",
                    context
            ));

            String rtype = ((Str) type).trueValue();
            if (!rtype.equals(returnType)) return res.failure(RTError.Type(
                    pos_start, pos_end,
                    "Return value has mismatched type",
                    context
            ));
        }

        ArrayList<Obj> newArgs = new ArrayList<>(args);
        newArgs.add(retValue);
        for (Function f: this.postprocessors) {
            retValue = res.register(((Function) f.copy().set_context(execCtx)).execute(newArgs, generics, kwargs, interpreter));
            if (res.error != null) return res;
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
                                            defaultCount, generics).setCatch(catcher).processors(preprocessors, postprocessors)
            .setIterative(argname).setKwargs(kwargname)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("function").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "<function-"+name+">"; }
    public boolean isAsync() { return async; }

}
