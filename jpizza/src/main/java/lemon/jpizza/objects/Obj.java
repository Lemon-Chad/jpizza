package lemon.jpizza.objects;

import lemon.jpizza.*;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.results.RTResult;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Obj implements Serializable {
    public Object value;

    public String string;

    public double number;
    public boolean hex;

    public ConcurrentHashMap<Obj, Obj> map;

    public boolean boolval;

    public List<Obj> list;

    public byte[] arr;

    public Position pos_start;
    public Position pos_end;
    public Context context;
    public Constants.JPType jptype;

    public Object getValue() { return value; }

    public abstract Obj set_pos(Position pos_start, Position pos_end);
    public abstract Obj set_pos(Position pos_start);
    public abstract Obj set_pos();

    public abstract Position get_start();
    public abstract Position get_end();
    public abstract Context get_ctx();

    public abstract Obj number();
    public abstract Obj alist();
    public abstract Obj bool();
    public abstract Obj anull();
    public abstract Obj function();
    public abstract Obj dictionary();
    public abstract Obj astring();
    public abstract Obj bytes();

    public abstract Obj type();
    public abstract Obj copy();

    public Object access(Obj name) { return null; }

    public abstract Obj set_context(Context value);
    public abstract Obj set_context();

    public abstract Pair<Obj, RTError> append(Obj other);
    public abstract Pair<Obj, RTError> extend(Obj other);
    public abstract Pair<Obj, RTError> pop(Obj other);
    public abstract Pair<Obj, RTError> remove(Obj other);
    public abstract Pair<Obj, RTError> bracket(Obj other);

    public boolean floating() {
        return Math.floor(number) == number;
    }

    public abstract RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent);

    public abstract Pair<Obj, RTError> eq(Obj obj);
    public abstract Pair<Obj, RTError> ne(Obj obj);

    public abstract Pair<Obj, RTError> also(Obj other);
    public abstract Pair<Obj, RTError> including(Obj other);
    public abstract Pair<Obj, RTError> invert();

    public abstract Obj delete(Obj other);
    public abstract Pair<Obj, RTError> get(Obj other);

    public abstract Pair<Obj, RTError> add(Obj other);
    public abstract Pair<Obj, RTError> sub(Obj other);
    public abstract Pair<Obj, RTError> mul(Obj other);
    public abstract Pair<Obj, RTError> div(Obj other);
    public abstract Pair<Obj, RTError> fastpow(Obj other);
    public abstract Pair<Obj, RTError> mod(Obj other);
    public abstract Pair<Obj, RTError> lte(Obj other);
    public abstract Pair<Obj, RTError> lt(Obj other);

    public Pair<Obj, RTError> deref() {
        return new Pair<>(null, RTError.Type(
            get_start(), get_end(),
            "Cannot be dereferenced",
            get_ctx()
        ));
    }

    public Pair<Obj, RTError> mutate(Obj other) {
        return new Pair<>(null, RTError.Type(
            get_start(), get_end(),
            "Cannot be mutated",
            get_ctx()
        ));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Obj)) return false;

        Obj other = (Obj) o;
        while (other.jptype == Constants.JPType.Ref)
            other = other.deref().a;

        if (jptype != other.jptype) return false;

        Pair<Obj, RTError> val = this.eq(other);
        if (val.b != null) return false;
        return val.a.boolval;
    }

}
