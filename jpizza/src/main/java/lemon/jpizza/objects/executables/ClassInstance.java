package lemon.jpizza.objects.executables;

import lemon.jpizza.*;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.*;
import lemon.jpizza.objects.Value;
import lemon.jpizza.results.RTResult;

import java.util.*;

public class ClassInstance extends Value {
    Position pos_start; Position pos_end;
    Context context;
    public final Context value;

    public ClassInstance(Context value) {
        this.value = value;
        value.symbolTable.define("this", this);

        set_pos(); set_context();
        jptype = Constants.JPType.ClassInstance;
    }

    public Object access(Obj o) {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("access");
        if (func == null)
            return _access(o);
        Obj x = res.register(func.execute(Collections.singletonList(o), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null) return res.error;
        return x;
    }

    public Object _access(Obj o) {
        if (o.jptype != Constants.JPType.String) return RTError.Type(
                o.get_start(), o.get_end(),
                "Expected string",
                o.get_ctx()
        );
        String other = o.string;
        Object c = value.symbolTable.get(other);
        Object x = value.symbolTable.getattr(other);
        if (x != null) {
            if (value.symbolTable.isprivate(other))
                return RTError.Publicity(
                        o.get_start(), o.get_end(),
                        "Attribute is private",
                        o.get_ctx()
                );
            return x;
        }
        else if (c != null) return c;
        else return RTError.Scope(
                    o.get_start(), o.get_end(),
                    "Attribute does not exist",
                    o.get_ctx()
            );
    }

    public Obj set_pos(Position pos_start, Position pos_end) {
        this.pos_start = pos_start; this.pos_end = pos_end;
        return this;
    }
    public Obj set_pos(Position pos_start) { return set_pos(pos_start, pos_start.copy().advance()); }
    public Obj set_pos() { return set_pos(null, null); }

    public Position get_start() { return pos_start; }
    public Position get_end() { return pos_end; }
    public Context get_ctx() { return context; }

    public ClassInstance set_context(Context context) { this.context = context; return this; }
    public ClassInstance set_context() { return set_context(null); }

    interface SuperBin {
        Pair<Obj, RTError> run(Obj other);
    }

    interface SuperUn {
        Pair<Obj, RTError> run();
    }

    public Pair<Obj, RTError> binOp(Obj other, String methodName, SuperBin superMethod) {
        RTResult res;
        CMethod method = value.symbolTable.getbin(methodName);
        if (method == null)
            return superMethod.run(other);
        res = method.execute(Collections.singletonList(other), new ArrayList<>(), new HashMap<>(),
                new Interpreter());
        if (checkType(methodName, res)) return new Pair<>(res.value, (RTError) res.error);
        return superMethod.run(other);
    }

    private boolean checkType(String methodName, RTResult res) {
        boolean typeMatch = res.value != null && Constants.methTypes.containsKey(methodName)
                && res.value.jptype == Constants.methTypes.get(methodName);
        if (typeMatch || !Constants.methTypes.containsKey(methodName))
            return true;
        else Shell.logger.warn(RTError.Type(
                pos_start, pos_end,
                String.format("Bin method should have return type %s, got %s",
                        Constants.methTypes.get(methodName), res.value.jptype),
                context
        ).asString());
        return false;
    }

    public Pair<Obj, RTError> unOp(String methodName, SuperUn superMethod) {
        RTResult res = new RTResult();
        CMethod method = value.symbolTable.getbin(methodName);
        if (method == null)
            return superMethod.run();
        res.register(method.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(),
                new Interpreter()));
        if (checkType(methodName, res)) return new Pair<>(res.value, (RTError) res.error);
        return superMethod.run();
    }

    public Pair<Obj, RTError> eq(Obj other) { return binOp(other, "eq", super::eq); }
    public Pair<Obj, RTError> ne(Obj other) { return binOp(other, "ne", super::ne); }
    public Pair<Obj, RTError> lt(Obj other) { return binOp(other, "lt", super::lt); }
    public Pair<Obj, RTError> lte(Obj other) { return binOp(other, "lte", super::lte); }
    public Pair<Obj, RTError> also(Obj other) { return binOp(other, "also", super::also); }
    public Pair<Obj, RTError> including(Obj other) { return binOp(other, "including", super::including); }
    public Pair<Obj, RTError> invert() { return unOp("invert", super::invert); }

    public Pair<Obj, RTError> append(Obj other) { return binOp(other, "append", super::append); }
    public Pair<Obj, RTError> extend(Obj other) { return binOp(other, "extend", super::extend); }
    public Pair<Obj, RTError> pop(Obj other) { return binOp(other, "pop", super::pop); }
    public Pair<Obj, RTError> remove(Obj other) { return binOp(other, "remove", super::remove); }
    public Pair<Obj, RTError> bracket(Obj other) { return binOp(other, "bracket", super::bracket); }

    public Pair<Obj, RTError> add(Obj other) { return binOp(other, "add", super::add); }
    public Pair<Obj, RTError> sub(Obj other) { return binOp(other, "sub", super::sub); }
    public Pair<Obj, RTError> mul(Obj other) { return binOp(other, "mul", super::mul); }
    public Pair<Obj, RTError> div(Obj other) { return binOp(other, "div", super::div); }
    public Pair<Obj, RTError> fastpow(Obj other) { return binOp(other, "fastpow", super::fastpow); }
    public Pair<Obj, RTError> mod(Obj other) { return binOp(other, "mod", super::mod); }

    public Obj delete(Obj other) {
        RTResult res;
        CMethod method = value.symbolTable.getbin("delete");
        if (method == null)
            return super.delete(other);
        res = method.execute(Collections.singletonList(other), new ArrayList<>(), new HashMap<>(),
                new Interpreter());
        if (checkType("delete", res)) {
            if (res.error == null)
                return res.value;
            else Shell.logger.warn(res.error.asString());
        }
        return super.delete(other);
    }
    public Pair<Obj, RTError> get(Obj other) { return binOp(other, "get", super::get); }

    public Obj dictionary() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("dictionary");
        if (func == null)
            return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null || x.jptype != Constants.JPType.Dict) {
            if (res.error != null)
                Shell.logger.warn(res.error.asString());
            else
                Shell.logger.warn("Mismatched type, expected dict (" + value.displayName + "-" + func.name + ")");
            return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end);
        }
        return x;
    }
    public Obj alist() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("list");
        if (func == null)
            return new PList(new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null || x.jptype != Constants.JPType.List) {
            if (res.error != null)
                Shell.logger.warn(res.error.asString());
            else
                Shell.logger.warn("Mismatched type, expected list (" + value.displayName + "-" + func.name + ")");
            return new PList(new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end);
        }
        return x;
    }
    public Obj astring() {
        CMethod func = value.symbolTable.getbin("string");
        return tstr(func);
    }
    public Obj number() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("number");
        if (func == null)
            return new Num(0).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null || x.jptype != Constants.JPType.Number) {
            if (res.error != null)
                Shell.logger.warn(res.error.asString());
            else
                Shell.logger.warn("Mismatched type, expected num (" + value.displayName + "-" + func.name + ")");
            return new Num(0).set_context(context).set_pos(pos_start, pos_end);
        }
        return x;
    }
    public Obj bytes() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("bytes");
        if (func == null)
            return new Bytes(new byte[0]).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null || x.jptype != Constants.JPType.Bytes) {
            if (res.error != null)
                Shell.logger.warn(res.error.asString());
            else
                Shell.logger.warn("Mismatched type, expected bytes (" + value.displayName + "-" + func.name + ")");
            return new Bytes(new byte[0]).set_context(context).set_pos(pos_start, pos_end);
        }
        return x;
    }
    public Obj bool() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("boolean");
        if (func == null)
            return new Bool(true).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null || x.jptype != Constants.JPType.Boolean) {
            if (res.error != null)
                Shell.logger.warn(res.error.asString());
            else
                Shell.logger.warn("Mismatched type, expected bool (" + value.displayName + "-" + func.name + ")");
            return new Bool(true).set_context(context).set_pos(pos_start, pos_end);
        }
        return x;
    }
    public Obj anull() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("null");
        if (func == null)
            return new Null().set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null || x.jptype != Constants.JPType.Null) {
            if (res.error != null)
                Shell.logger.warn(res.error.asString());
            else
                Shell.logger.warn("Mismatched type, expected null (" + value.displayName + "-" + func.name + ")");
            return new Null().set_context(context).set_pos(pos_start, pos_end);
        }
        return x;
    }
    public Obj function() {
        return this;
    }
    public Obj tstr(CMethod func) {
        RTResult res = new RTResult();
        if (func == null)
            return new Str(value.displayName).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null || x.jptype != Constants.JPType.String) {
            if (res.error != null)
                Shell.logger.warn(res.error.asString());
            else
                Shell.logger.warn("Mismatched type, expected String (" + value.displayName + "-" + func.name + ")");
            return new Str(value.displayName).set_context(context).set_pos(pos_start, pos_end);
        }
        return x;
    }

    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        return super.execute(args, generics, kwargs, parent);
    }

    public Obj type() {
        CMethod func = value.symbolTable.getbin("type");
        return tstr(func);
    }
    public Obj copy() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("copy");
        if (func == null)
            return new ClassInstance(value).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null || x.jptype != Constants.JPType.ClassInstance) {
            if (res.error != null)
                Shell.logger.warn(res.error.asString());
            else
                Shell.logger.warn("Mismatched type, expected Instance (" + value.displayName + "-" + func.name + ")");
            return new ClassInstance(value).set_context(context).set_pos(pos_start, pos_end);
        }
        return x;
    }
    public String toString() { return (String) (astring().value); }

}
