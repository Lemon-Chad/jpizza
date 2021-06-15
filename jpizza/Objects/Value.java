package lemon.jpizza.Objects;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Double;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Primitives.Bool;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Value extends Obj {
    public Object value;
    public Position pos_start; public Position pos_end;
    public Context context;

    public Value(Object value) { this.value = value; set_pos(); set_context(); }
    public Value() { this.value = null; }

    public Value set_pos(Position pos_start, Position pos_end) {
        this.pos_start = pos_start; this.pos_end = pos_end;
        return this;
    }
    public Value set_pos() { return set_pos(null, null); }
    public Value set_pos(Position start_pos) { return set_pos(start_pos, start_pos.copy().advance()); }
    public Value set_context(Context context) {
        this.context = context;
        return this;
    }
    public Value set_context() { return set_context(null); }

    // Conversions

    public Value number() { return this; }
    public Value dictionary() { return this; }
    public Value alist() { return this; }
    public Value bool() { return this; }
    public Value anull() { return new Null().set_context(context).set_pos(pos_start, pos_end); }
    public Obj astring() { return new Str(toString()).set_context(context).set_pos(pos_start, pos_end); }
    public Value function() { return this; }

    // Dictionary defaults

    public Obj delete(Obj other) { return (Obj)    dictionary().getattr("delete", other); }
    public Double<Obj, RTError> get(Obj other) { return (Double<Obj, RTError>) dictionary().getattr("get", other); }

    // Number defaults

    public Double<Obj, RTError> add(Obj other) { return (Double<Obj, RTError>) number().getattr("add", other); }
    public Double<Obj, RTError> sub(Obj other) { return (Double<Obj, RTError>) number().getattr("sub", other); }
    public Double<Obj, RTError> mul(Obj other) { return (Double<Obj, RTError>) number().getattr("mul", other); }
    public Double<Obj, RTError> div(Obj other) { return (Double<Obj, RTError>) number().getattr("div", other); }
    public Double<Obj, RTError> fastpow(Obj other) { return (Double<Obj, RTError>) number().getattr("fastpow", other); }
    public Double<Obj, RTError> mod(Obj other) { return (Double<Obj, RTError>) number().getattr("mod", other); }
    public Double<Obj, RTError> lte(Obj other) { return (Double<Obj, RTError>) number().getattr("lte", other); }
    public Double<Obj, RTError> lt(Obj other) { return (Double<Obj, RTError>) number().getattr("lt", other); }

    // String defaults

    // Boolean defaults

    public Double<Obj, RTError> also(Obj other) { return (Double<Obj, RTError>) bool().getattr("also", other); }
    public Double<Obj, RTError> including(Obj other) { return (Double<Obj, RTError>) bool().getattr("including", other); }
    public Double<Obj, RTError> invert() { return (Double<Obj, RTError>) bool().getattr("invert"); }


    // List defaults

    public Double<Obj, RTError> append(Obj other) { return (Double<Obj, RTError>) alist().getattr("append", other); }
    public Double<Obj, RTError> extend(Obj other) { return (Double<Obj, RTError>) alist().getattr("extend", other); }
    public Double<Obj, RTError> pop(Obj other) { return (Double<Obj, RTError>) alist().getattr("pop", other); }
    public Double<Obj, RTError> remove(Obj other) { return (Double<Obj, RTError>) alist().getattr("remove", other); }

    // Function defaults

    //public RTResult execute(List<Obj> args) { return execute(args, new Interpreter()); }
    public RTResult execute(List<Obj> args, Interpreter parent) {
        return (RTResult) function().getattr("execute", args, parent);
    }

    // Other

    public Double<Obj, RTError> eq(Obj obj) { return new Double<>(new Bool(this.value.equals(obj.value)), null); }
    public Double<Obj, RTError> ne(Obj obj) { return new Double<>(((Bool) eq(obj).a).invert().a, null); }

    public String toString() { return value.toString(); }
    public Obj copy() { return new Value(value).set_pos(pos_start, pos_end).set_context(context); }
    public Obj type() { return new Str("generic-value").set_pos(pos_start, pos_end).set_context(context); }

    public Object getattr(String name, Object... argx) {
        Method method;
        List<Object> args = Arrays.asList(argx.clone());
        List<Class<?>> typex = new ArrayList<>();
        args.forEach(x -> {
            Class<?> y = x.getClass();
            while (y.getSuperclass() != Object.class) y = y.getSuperclass();
            typex.add(y);
        });
        Class<?>[] types = new Class<?>[typex.size()];
        types = typex.toArray(types);
        try {
            method = this.getClass().getMethod(name, types);
            return method.invoke(this, argx);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

}
