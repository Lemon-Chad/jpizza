package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Results.RTResult;

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

    public RTResult execute(List<Obj> args, Interpreter parent) {
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
        res.register(checkPopArgs(argNames, args, execCtx));
        if (res.shouldReturn()) return res;

        Obj returnValue;
        try {
            returnValue = (Obj) res.register((RTResult) method.invoke(this, execCtx));
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return res.failure(new RTError(
                    pos_start, pos_end,
                    "LibError",
                    context
            ));
        }
        if (res.shouldReturn()) return res;

        return res.success(returnValue);
    }

    // Methods

    // Conversions

    public Value alist() {
        return new PList(valList()).set_context(context).set_pos(pos_start, pos_end);
    }
    public Value number() { return new Num(valList().size()).set_context(context).set_pos(pos_start, pos_end); }
    public Value bool() { return new Bool(true).set_context(context).set_pos(pos_start, pos_end); }
    public Value string() { return new Str(toString()).set_context(context).set_pos(pos_start, pos_end); }
    public Value dictionary() { return new Dict(new HashMap<>(){{
        Obj[] vallist = (Obj[]) valList().toArray();
        int length = vallist.length;
        for (int i = 0; i < length; i++)
            add(vallist[i]);
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
