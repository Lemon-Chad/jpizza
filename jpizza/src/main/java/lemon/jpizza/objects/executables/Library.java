package lemon.jpizza.objects.executables;

import lemon.jpizza.*;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.*;
import lemon.jpizza.results.RTResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Library extends BaseFunction {
    public static final Map< String, Map< String, List<String> > > atrs = new HashMap<>();
    final String libname;
    public Library(String name, String libname) {
        super(name);
        this.libname = libname;
        jptype = JPType.Library;
    }

    public List<Obj> valList() {
        String methodName = "execute_"+name;
        List<Obj> argVals = new ArrayList<>();
        List<String> argNames = atrs.get(libname).get(methodName);
        if (argNames != null) {
            int size = argNames.size();
            for (int i = 0; i < size; i++)
                argVals.add(new Str(argNames.get(i)).set_context(context).set_pos(pos_start, pos_end));
        }
        return argVals;
    }

    // Functions

    public static RTResult checkType(Object obj, String expect, JPType type) {
        Obj o = (Obj) obj;
        if (o.jptype != type) return new RTResult().failure(RTError.Type(
                o.get_start(), o.get_end(),
                "Expected " + expect,
                o.get_ctx()
        ));
        return new RTResult().success(o);
    }

    public static RTResult checkFunction(Object obj) {
        Obj o = (Obj) obj;
        if (o.jptype != JPType.Function && o.jptype != JPType.CMethod)
            return new RTResult().failure(RTError.Type(
                o.get_start(), o.get_end(),
                "Expected function",
                o.get_ctx()
            ));
        return new RTResult().success(o);
    }

    public static RTResult checkInt(Object obj) {
        Obj o = (Obj) obj;
        if (o.jptype != JPType.Number || o.floating()) return new RTResult().failure(RTError.Type(
                o.get_start(), o.get_end(),
                "Expected an integer",
                o.get_ctx()
        ));
        return new RTResult().success(o);
    }

    public static RTResult checkPosInt(Object obj) {
        Obj o = (Obj) obj;
        if (o.jptype != JPType.Number || o.floating() || o.number < 0)
            return new RTResult().failure(RTError.Type(
                o.get_start(), o.get_end(),
                "Expected a postive integer",
                o.get_ctx()
        ));
        return new RTResult().success(o);
    }

    public static void initialize(String libName, Class<? extends Library> cls, Map<String, List<String>> funcs) {
        Context libContext = new Context(libName, null, null);
        libContext.symbolTable = new SymbolTable(Shell.globalSymbolTable);
        initialize(libName, cls, funcs, libContext, true);
    }

    public static void initialize(String libName, Class<? extends Library> cls, Map<String, List<String>> funcs, SymbolTable table) {
        Context libContext = new Context(libName, null, null);
        libContext.symbolTable = table;
        initialize(libName, cls, funcs, libContext, false);
    }

    public static void initialize(String libName, Class<? extends Library> cls, Map<String, List<String>> funcs, Context libContext,
                                  boolean adlib) {
        SymbolTable libTable = libContext.symbolTable;

        Constructor<? extends Library> cons;
        try {
            cons = cls.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(); return;
        }

        atrs.put(libName, new HashMap<>());
        Map<String, List<String>> libAtrs = atrs.get(libName);

        funcs.forEach((k, v) -> {
            // Initialize here
            libAtrs.put(k, v);
            Library val;
            try {
                val = cons.newInstance(k);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
                return;
            }
            libTable.declareattr(new Token(TokenType.Identifier, k, libContext.parentEntryPos, libContext.parentEntryPos), null, val);
        });
        if (adlib) Constants.LIBRARIES.put(libName, libContext);
    }

    @Override
    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        RTResult res = new RTResult();
        Context execCtx = newContext();

        String methodName = "execute_" + name;
        List<String> argNames = atrs.get(libname).get(name);
        if (argNames == null)
            return res.failure(RTError.Scope(
                    pos_start, pos_end,
                    "Undefined method",
                    context
            ));

        Method method;
        try {
            method = this.getClass().getMethod(methodName, Context.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return res.failure(RTError.Scope(
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
            return res.failure(RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
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
