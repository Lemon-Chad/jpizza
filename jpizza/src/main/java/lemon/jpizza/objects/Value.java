package lemon.jpizza.objects;

import lemon.jpizza.*;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.objects.primitives.Bytes;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.primitives.Bool;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.results.RTResult;

import static lemon.jpizza.Operations.*;

import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
public class Value extends Obj {
    public Object value = null;
    public Position pos_start;
    public Position pos_end;
    public Context context;

    public Value(Object value) {
        this.value = value; jptype = Constants.JPType.Generic;
        set_pos(); set_context();
    }
    public Value() {
        jptype = Constants.JPType.Generic;
        set_pos(); set_context();
    }

    public Obj set_pos(Position ps, Position pe) {
        pos_start = ps; pos_end = pe;
        return this;
    }
    public Obj set_pos() {
        this.pos_start = null;
        this.pos_end = null;
        return this;
    }
    public Obj set_pos(Position start_pos) { return set_pos(start_pos, start_pos.copy().advance()); }
    public Obj set_context(Context ctx) {
        context = ctx;
        return this;
    }
    public Obj set_context() { return set_context(null); }

    public Position get_start() { return pos_start; }
    public Position get_end() { return pos_end; }
    public Context get_ctx() { return context; }

    // Conversions

    public Obj number() { return this; }
    public Obj dictionary() { return this; }
    public Obj alist() { return this; }
    public Obj bool() { return this; }
    public Obj anull() { return new Null().set_context(context).set_pos(pos_start, pos_end); }
    public Obj astring() { return new Str(toString()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj function() { return this; }
    public Obj bytes() {
        return new Bytes(Constants.objToBytes(Constants.toObject(this)))
                .set_context(context)
                .set_pos(pos_start, pos_end);
    }

    // Dictionary defaults

    public Obj delete(Obj other) { return (Obj)    dictionary().getattr(OP.DELETE, other); }
    public Pair<Obj, RTError> get(Obj other) { return (Pair<Obj, RTError>) dictionary().getattr(OP.GET, other); }

    // Number defaults

    public Pair<Obj, RTError> add(Obj other) { return (Pair<Obj, RTError>) number().getattr(OP.ADD, other); }
    public Pair<Obj, RTError> sub(Obj other) { return (Pair<Obj, RTError>) number().getattr(OP.SUB, other); }
    public Pair<Obj, RTError> mul(Obj other) { return (Pair<Obj, RTError>) number().getattr(OP.MUL, other); }
    public Pair<Obj, RTError> div(Obj other) { return (Pair<Obj, RTError>) number().getattr(OP.DIV, other); }
    public Pair<Obj, RTError> fastpow(Obj other) { return (Pair<Obj, RTError>) number().getattr(OP.FASTPOW, other); }
    public Pair<Obj, RTError> mod(Obj other) { return (Pair<Obj, RTError>) number().getattr(OP.MOD, other); }
    public Pair<Obj, RTError> lte(Obj other) { return (Pair<Obj, RTError>) number().getattr(OP.LTE, other); }
    public Pair<Obj, RTError> lt(Obj other) { return (Pair<Obj, RTError>) number().getattr(OP.LT, other); }

    // String defaults

    // Boolean defaults

    public Pair<Obj, RTError> also(Obj other) { return (Pair<Obj, RTError>) bool().getattr(OP.ALSO, other); }
    public Pair<Obj, RTError> including(Obj other) { return (Pair<Obj, RTError>) bool().getattr(OP.INCLUDING, other); }
    public Pair<Obj, RTError> invert() { return (Pair<Obj, RTError>) bool().getattr(OP.INVERT); }


    // List defaults

    public Pair<Obj, RTError> append(Obj other) { return (Pair<Obj, RTError>) alist().getattr(OP.APPEND, other); }
    public Pair<Obj, RTError> extend(Obj other) { return (Pair<Obj, RTError>) alist().getattr(OP.EXTEND, other); }
    public Pair<Obj, RTError> pop(Obj other) { return (Pair<Obj, RTError>) alist().getattr(OP.POP, other); }
    public Pair<Obj, RTError> remove(Obj other) { return (Pair<Obj, RTError>) alist().getattr(OP.REMOVE, other); }
    public Pair<Obj, RTError> bracket(Obj other) { return (Pair<Obj, RTError>) alist().getattr(OP.BRACKET, other); }

    // Function defaults

    //public RTResult execute(List<Obj> args) { return execute(args, new Interpreter()); }
    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        return (RTResult) function().getattr(OP.EXECUTE, args, generics, kwargs, parent);
    }

    // Other

    public Pair<Obj, RTError> eq(Obj obj) { return new Pair<>(new Bool(this.value.equals(obj.value)), null); }
    public Pair<Obj, RTError> ne(Obj obj) { return new Pair<>(((Bool) eq(obj).a).invert().a, null); }

    public String toString() { return value.toString(); }
    public Obj copy() { return new Value(value).set_pos(pos_start, pos_end).set_context(context); }
    public Obj type() { return new Str("generic-value").set_pos(pos_start, pos_end).set_context(context); }

    public Object getattr(OP name, Object... argx) {
        return switch (name) {
            case NUMBER     -> number    ();
            case DICTIONARY -> dictionary();
            case ALIST      -> alist     ();
            case BOOL       -> bool      ();
            case ANULL      -> anull     ();
            case ASTRING    -> astring   ();
            case FUNCTION   -> function  ();
            case DELETE     -> delete    ((Obj) argx[0]);
            case GET        -> get       ((Obj) argx[0]);
            case ADD        -> add       ((Obj) argx[0]);
            case SUB        -> sub       ((Obj) argx[0]);
            case MUL        -> mul       ((Obj) argx[0]);
            case DIV        -> div       ((Obj) argx[0]);
            case MOD        -> mod       ((Obj) argx[0]);
            case FASTPOW    -> fastpow   ((Obj) argx[0]);
            case LTE        -> lte       ((Obj) argx[0]);
            case LT         -> lt        ((Obj) argx[0]);
            case ALSO       -> also      ((Obj) argx[0]);
            case INCLUDING  -> including ((Obj) argx[0]);
            case INVERT     -> invert    ();
            case APPEND     -> append    ((Obj) argx[0]);
            case EXTEND     -> extend    ((Obj) argx[0]);
            case POP        -> pop       ((Obj) argx[0]);
            case REMOVE     -> remove    ((Obj) argx[0]);
            case EXECUTE    -> execute   ((List<Obj>) argx[0], (List<Token>) argx[1], (Map<String, Obj>) argx[2],
                    (Interpreter) argx[3]);
            case EQ         -> eq        ((Obj) argx[0]);
            case NE         -> ne        ((Obj) argx[0]);
            case TOSTRING   -> toString  ();
            case COPY       -> copy      ();
            case TYPE       -> type      ();
            case BRACKET    -> bracket   ((Obj) argx[0]);

            default         -> {
                Shell.logger.outln("Attribute " + name + " not found");
                yield null;
            }
        };
        /* Method method;
        List<Object> args = Arrays.asList(argx.clone());
        List<Class<?>> typex = new ArrayList<>();
        int size = args.size();
        for (int i = 0; i < size; i++) {
            Class<?> y = args.get(i).getClass();
            while (y.getSuperclass() != Object.class) y = y.getSuperclass();
            typex.add(y);
        };
        Class<?>[] types = new Class<?>[typex.size()];
        types = typex.toArray(types);
        try {
            method = this.getClass().getMethod(name, types);
            return method.invoke(this, argx);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        } */
    }

}
