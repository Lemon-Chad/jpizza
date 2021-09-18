package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Library extends BaseFunction {
    public static Map<String, List<String>> atrs = new HashMap<>();
    public Library(String name) {
        super(name);
        jptype = Constants.JPType.Library;
    }

    public List<Obj> valList() {
        String methodName = "execute_"+name;
        List<Obj> argVals = new ArrayList<>();
        List<String> argNames = atrs.get(methodName);
        if (argNames != null) {
            int size = argNames.size();
            for (int i = 0; i < size; i++)
                argVals.add(new Str(argNames.get(i)).set_context(context).set_pos(pos_start, pos_end));
        }
        return argVals;
    }

    // Functions

    public static RTResult checkType(Object obj, String expect, Constants.JPType type) {
        if (obj == null)
            return new RTResult().failure(new RTError(
                    null, null,
                    "Expected " + expect,
                    null
            ));
        Obj o = (Obj) obj;
        if (o.jptype != type) return new RTResult().failure(new RTError(
                o.get_start(), o.get_end(),
                "Expected " + expect,
                o.get_ctx()
        ));
        return new RTResult().success(o);
    }

    public static RTResult checkFunction(Object obj) {
        if (obj == null)
            return new RTResult().failure(new RTError(
                    null, null,
                    "Expected function",
                    null
            ));
        Obj o = (Obj) obj;
        if (o.jptype != Constants.JPType.Function && o.jptype != Constants.JPType.CMethod)
            return new RTResult().failure(new RTError(
                o.get_start(), o.get_end(),
                "Expected function",
                o.get_ctx()
            ));
        return new RTResult().success(o);
    }

    public static RTResult checkInt(Object obj) {
        if (obj == null)
            return new RTResult().failure(new RTError(
                    null, null,
                    "Expected an integer",
                    null
            ));
        Obj o = (Obj) obj;
        if (o.jptype != Constants.JPType.Number || ((Num) o).floating) return new RTResult().failure(new RTError(
                o.get_start(), o.get_end(),
                "Expected an integer",
                o.get_ctx()
        ));
        return new RTResult().success(o);
    }

    public static void initialize() {}

    public static RTResult checkPosInt(Object obj) {
        if (obj == null)
            return new RTResult().failure(new RTError(
                    null, null,
                    "Expected a postive integer",
                    null
            ));
        Obj o = (Obj) obj;
        if (o.jptype != Constants.JPType.Number || ((Num) o).floating || ((Num) o).trueValue() < 0)
            return new RTResult().failure(new RTError(
                o.get_start(), o.get_end(),
                "Expected a postiive integer",
                o.get_ctx()
        ));
        return new RTResult().success(o);
    }

    public static void initialize(String libName, Class<?> cls, Map<String, List<String>> funcs) {
        Context libContext = new Context(libName, null, null);
        libContext.symbolTable = new SymbolTable();
        initialize(libName, cls, funcs, libContext, true);
    }

    public static void initialize(String libName, Class<?> cls, Map<String, List<String>> funcs, SymbolTable table) {
        Context libContext = new Context(libName, null, null);
        libContext.symbolTable = table;
        initialize(libName, cls, funcs, libContext, false);
    }

    @SuppressWarnings("DuplicatedCode")
    public static void initialize(String libName, Class<?> cls, Map<String, List<String>> funcs, Context libContext,
                                  boolean adlib) {
        SymbolTable libTable = libContext.symbolTable;

        Constructor<?> cons;
        try {
            cons = cls.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(); return;
        }

        funcs.forEach((k, v) -> {
            // Initialize here
            atrs.put(k, v);
            Library val;
            try {
                val = (Library) cons.newInstance(k);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
                return;
            }
            libTable.define(k, val);
        });
        if (adlib) Constants.LIBRARIES.put(libName, libContext);
    }

    @Override
    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        RTResult res = new RTResult();
        Context execCtx = newContext();

        String methodName = "execute_" + name;
        List<String> argNames = atrs.get(name);
        if (argNames == null)
            return res.failure(new RTError(
                    pos_start, pos_end,
                    "Undefined method",
                    context
            ));

        Method method;
        try {
            method = this.getClass().getMethod(methodName, Context.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return res.failure(new RTError(
                    pos_start, pos_end,
                    "Undefined method",
                    context
            ));
        }
        res.register(checkPopArgs(argNames, new ArrayList<>(), args, execCtx, new ArrayList<>(),
                argNames.size(), argNames.size(), new HashMap<>()));
        if (res.shouldReturn()) return res;

        Obj returnValue;
        try {
            returnValue = res.register((RTResult) method.invoke(this, execCtx));
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return res.failure(new RTError(
                    pos_start, pos_end,
                    "LibError",
                    context
            ));
        }
        if (res.shouldReturn()) return res;

        return res.success(returnValue.set_context(context));
    }

    // Methods

    // Conversions

    public Obj alist() {
        return new PList(valList()).set_context(context).set_pos(pos_start, pos_end);
    }
    public Obj number() { return new Num(valList().size()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj bool() { return new Bool(true).set_context(context).set_pos(pos_start, pos_end); }
    public Obj string() { return new Str(toString()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj dictionary() { return new Dict(new HashMap<>(){{
        for (Obj i : valList())
            add(i);
    }}).set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public Obj type() { return new Str("<lib>").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "<lib-"+name+">"; }
    public Obj copy() {
        Constructor<?> cons;
        try {
            cons = this.getClass().getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(); return new Null();
        }
        try {
            return ((Obj) cons.newInstance(name)).set_context(context).set_pos(pos_start, pos_end);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace(); return new Null();
        }
    }

}
