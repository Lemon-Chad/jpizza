package lemon.jpizza.objects.primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.Value;
import lemon.jpizza.Pair;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.List;
import java.util.Map;

public class Ref extends Value {
    Obj inner;
    public Ref(Obj inner) {
        this.inner = inner;
        jptype = Constants.JPType.Ref;
        pos_start = inner.get_start(); pos_end = inner.get_end();
    }

    public Obj number() { return inner.number(); }
    public Obj alist() { return inner.alist(); }
    public Obj bool() { return inner.bool(); }
    public Obj anull() { return inner.anull(); }
    public Obj function() { return inner.function(); }
    public Obj dictionary() { return inner.dictionary(); }
    public Obj astring() { return inner.astring(); }
    public Obj bytes() { return inner.bytes(); }

    public Obj type() { return new Str("[" + inner.type().toString() + "]"); }
    public Obj copy() { return new Ref(inner).set_pos(pos_start, pos_end).set_context(context); }

    public Pair<Obj, RTError> append(Obj other) { return inner.append(other); }
    public Pair<Obj, RTError> extend(Obj other) { return inner.extend(other); }
    public Pair<Obj, RTError> pop(Obj other) { return inner.pop(other); }
    public Pair<Obj, RTError> remove(Obj other) { return inner.remove(other); }
    public Pair<Obj, RTError> bracket(Obj other) { return inner.bracket(other); }

    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) { return inner.execute(args, generics, kwargs, parent); }

    public Pair<Obj, RTError> eq(Obj obj) { return inner.eq(obj); }
    public Pair<Obj, RTError> ne(Obj obj) { return inner.ne(obj); }

    public Pair<Obj, RTError> also(Obj other) { return inner.also(other); }
    public Pair<Obj, RTError> including(Obj other) { return inner.including(other); }
    public Pair<Obj, RTError> invert() { return inner.invert(); }

    public Obj delete(Obj other) { return inner.delete(other); }
    public Pair<Obj, RTError> get(Obj other) { return inner.get(other); }

    public Pair<Obj, RTError> add(Obj other) { return inner.add(other); }
    public Pair<Obj, RTError> sub(Obj other) { return inner.sub(other); }
    public Pair<Obj, RTError> mul(Obj other) { return inner.mul(other); }
    public Pair<Obj, RTError> div(Obj other) { return inner.div(other); }
    public Pair<Obj, RTError> fastpow(Obj other) { return inner.fastpow(other); }
    public Pair<Obj, RTError> mod(Obj other) { return inner.mod(other); }
    public Pair<Obj, RTError> lte(Obj other) { return inner.lte(other); }
    public Pair<Obj, RTError> lt(Obj other) { return inner.lt(other); }

    @Override
    public Pair<Obj, RTError> deref() {
        return new Pair<>(inner, null);
    }

    @Override
    public Pair<Obj, RTError> mutate(Obj other) {
        inner = other;
        return new Pair<>(this, null);
    }

    public String toString() {
        return inner.toString();
    }

}
