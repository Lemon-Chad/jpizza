package lemon.jpizza.objects;

import lemon.jpizza.*;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.objects.primitives.Bytes;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.primitives.Bool;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.results.RTResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
public class Value extends Obj {
    public Object value = null;
    public Position pos_start;
    public Position pos_end;
    public Context context;

    public Value(Object value) {
        this.value = value; jptype = JPType.Generic;
        set_pos(); set_context();
    }
    public Value() {
        jptype = JPType.Generic;
        set_pos(); set_context();
    }

    public Obj set_pos(@NotNull Position ps, @NotNull Position pe) {
        pos_start = ps; pos_end = pe;
        return this;
    }
    public Obj set_pos() {
        this.pos_start = null;
        this.pos_end = null;
        return this;
    }
    public Obj set_pos(@NotNull Position start_pos) { return set_pos(start_pos, start_pos.copy().advance()); }
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
    public Obj astring() { return new Str(toString()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj function() { return this; }
    public Obj bytes() {
        return new Bytes(Constants.objToBytes(Constants.toObject(this)))
                .set_context(context)
                .set_pos(pos_start, pos_end);
    }

    // Dictionary defaults

    public Obj delete(Obj other) { return dictionary().delete(other); }
    public Pair<Obj, RTError> get(Obj other) { return dictionary().get(other); }

    // Number defaults

    public Pair<Obj, RTError> add(Obj other) { return number().add(other); }
    public Pair<Obj, RTError> sub(Obj other) { return number().sub(other); }
    public Pair<Obj, RTError> mul(Obj other) { return number().mul(other); }
    public Pair<Obj, RTError> div(Obj other) { return number().div(other); }
    public Pair<Obj, RTError> fastpow(Obj other) { return number().fastpow(other); }
    public Pair<Obj, RTError> mod(Obj other) { return number().mod(other); }
    public Pair<Obj, RTError> lte(Obj other) { return number().lte(other); }
    public Pair<Obj, RTError> lt(Obj other) { return number().lt(other); }

    // String defaults

    // Boolean defaults

    public Pair<Obj, RTError> also(Obj other) { return (Pair<Obj, RTError>) bool().also(other); }
    public Pair<Obj, RTError> including(Obj other) { return (Pair<Obj, RTError>) bool().including(other); }
    public Pair<Obj, RTError> invert() { return (Pair<Obj, RTError>) bool().invert(); }


    // List defaults

    public Pair<Obj, RTError> append(Obj other) { return (Pair<Obj, RTError>) alist().append(other); }
    public Pair<Obj, RTError> extend(Obj other) { return (Pair<Obj, RTError>) alist().extend(other); }
    public Pair<Obj, RTError> pop(Obj other) { return (Pair<Obj, RTError>) alist().pop(other); }
    public Pair<Obj, RTError> remove(Obj other) { return (Pair<Obj, RTError>) alist().remove(other); }
    public Pair<Obj, RTError> bracket(Obj other) { return (Pair<Obj, RTError>) alist().bracket(other); }

    // Function defaults

    //public RTResult execute(List<Obj> args) { return execute(args, new Interpreter()); }
    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        return (RTResult) function().execute(args, generics, kwargs, parent);
    }

    // Other

    public Pair<Obj, RTError> eq(Obj obj) {
        if (this.value == null)
            return new Pair<>(new Bool(this.value == obj.value), null);
        return new Pair<>(new Bool(this.value.equals(obj.value)), null);
    }
    public Pair<Obj, RTError> ne(Obj obj) { return new Pair<>(new Bool(!equals(obj)), null); }

    public String toString() { return value.toString(); }
    public Obj copy() { return new Value(value).set_pos(pos_start, pos_end).set_context(context); }
    public Obj type() { return new Str("generic-value").set_pos(pos_start, pos_end).set_context(context); }

}
