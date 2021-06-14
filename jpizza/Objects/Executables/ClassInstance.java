package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Double;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ClassInstance extends Obj {
    Position pos_start; Position pos_end;
    Context context;
    public Context value;

    public ClassInstance(Context value) {
        this.value = value;

        set_pos(); set_context();
    }

    public Object access(String other) {
        Object c = value.symbolTable.get(other);
        Object x = value.symbolTable.getattr(other);
        return x != null ? x : (
                    c != null ? c : new Null()
                );
    }

    public Obj set_pos(Position pos_start, Position pos_end) {
        this.pos_start = pos_start; this.pos_end = pos_end;
        return this;
    }
    public Obj set_pos(Position pos_start) { return set_pos(pos_start, pos_start.copy().advance()); }
    public Obj set_pos() { return set_pos(null, null); }

    public ClassInstance set_context(Context context) { this.context = context; return this; }
    public ClassInstance set_context() { return set_context(null); }

    public Object getattr(String name, Object... argx) {
        CMethod bin = value.symbolTable.getbin(name);
        if (bin != null) {
            List<Obj> args = new ArrayList<>();
            int length = argx.length;
            for (int i = 0; i < length; i++) args.add((Obj) argx[i]);
            RTResult awesomePossum = bin.execute(args, new Interpreter());
            return new Double(awesomePossum.value, awesomePossum.error);
        }
        Method method;
        List<Object> args = Arrays.asList(argx.clone());
        List<Class<?>> typex = new ArrayList<>();
        args.forEach(x -> typex.add(x.getClass()));
        Class<?>[] types = (Class<?>[]) typex.toArray();
        try {
            method = this.getClass().getMethod(name, types);
            return method.invoke(this, argx);
        } catch (NoSuchMethodException e) {
            try {
                Method vclass = Value.class.getMethod(name, types);
                return vclass.invoke(this, argx);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException z) {
                return null;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    public Obj dictionary() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("dictionary");
        if (func == null)
            return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end);
        Obj x = (Obj) res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj alist() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("list");
        if (func == null)
            return new PList(new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end);
        Obj x = (Obj) res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new PList(new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj type() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("type");
        if (func == null)
            return new Str(value.displayName).set_context(context).set_pos(pos_start, pos_end);
        Obj x = (Obj) res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Str(value.displayName).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj astring() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("string");
        if (func == null)
            return new Str(value.displayName).set_context(context).set_pos(pos_start, pos_end);
        Obj x = (Obj) res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Str(value.displayName).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj number() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("number");
        if (func == null)
            return new Num(0).set_context(context).set_pos(pos_start, pos_end);
        Obj x = (Obj) res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Num(0).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj bool() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("boolean");
        if (func == null)
            return new Bool(true).set_context(context).set_pos(pos_start, pos_end);
        Obj x = (Obj) res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Bool(true).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj anull() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("null");
        if (func == null)
            return new Null().set_context(context).set_pos(pos_start, pos_end);
        Obj x = (Obj) res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Null().set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj copy() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("copy");
        if (func == null)
            return new ClassInstance(value).set_context(context).set_pos(pos_start, pos_end);
        Obj x = (Obj) res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new ClassInstance(value).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj function() {
        return this;
    }

    public String toString() { return (String) (astring().value); }







}
