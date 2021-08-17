package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Pair;

import java.util.*;

import static lemon.jpizza.Operations.*;

public class ClassInstance extends Obj {
    Position pos_start; Position pos_end;
    Context context;
    public Context value;

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
        Obj x = res.register(func.execute(Collections.singletonList(o), new Interpreter()));
        if (res.error != null) return res.error;
        return x;
    }

    public Object _access(Obj o) {
        if (o.jptype != Constants.JPType.String) return new RTError(
                o.get_start(), o.get_end(),
                "Expected string",
                o.get_ctx()
        );
        String other = ((Str) o).trueValue();
        Object c = value.symbolTable.get(other);
        Object x = value.symbolTable.getattr(other);
        if (x != null) return x;
        else if (c != null) return c;
        else return new RTError(
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

    public Object getattr(OP name, Object... argx) {
        CMethod bin = switch (name) {
            case NUMBER     -> value.symbolTable.getbin("number"    );
            case DICTIONARY -> value.symbolTable.getbin("dictionary");
            case ALIST      -> value.symbolTable.getbin("alist"     );
            case BOOL       -> value.symbolTable.getbin("bool"      );
            case ANULL      -> value.symbolTable.getbin("anull"     );
            case ASTRING    -> value.symbolTable.getbin("astring"   );
            case FUNCTION   -> value.symbolTable.getbin("function"  );
            case DELETE     -> value.symbolTable.getbin("delete"    );
            case GET        -> value.symbolTable.getbin("get"       );
            case ADD        -> value.symbolTable.getbin("add"       );
            case SUB        -> value.symbolTable.getbin("sub"       );
            case MUL        -> value.symbolTable.getbin("mul"       );
            case DIV        -> value.symbolTable.getbin("div"       );
            case MOD        -> value.symbolTable.getbin("mod"       );
            case FASTPOW    -> value.symbolTable.getbin("fastpow"   );
            case LTE        -> value.symbolTable.getbin("lte"       );
            case LT         -> value.symbolTable.getbin("lt"        );
            case ALSO       -> value.symbolTable.getbin("also"      );
            case INCLUDING  -> value.symbolTable.getbin("including" );
            case INVERT     -> value.symbolTable.getbin("invert"    );
            case APPEND     -> value.symbolTable.getbin("append"    );
            case EXTEND     -> value.symbolTable.getbin("extend"    );
            case POP        -> value.symbolTable.getbin("pop"       );
            case REMOVE     -> value.symbolTable.getbin("remove"    );
            case EXECUTE    -> value.symbolTable.getbin("execute"   );
            case EQ         -> value.symbolTable.getbin("eq"        );
            case NE         -> value.symbolTable.getbin("ne"        );
            case TOSTRING   -> value.symbolTable.getbin("toString"  );
            case COPY       -> value.symbolTable.getbin("copy"      );
            case TYPE       -> value.symbolTable.getbin("type"      );
            case BRACKET    -> value.symbolTable.getbin("bracket"   );
            default         -> null;
        };
        if (bin != null) {
            List<Obj> args = new ArrayList<>();
            int length = argx.length;
            for (int i = 0; i < length; i++) args.add((Obj) argx[i]);
            RTResult awesomePossum = bin.execute(args, new Interpreter());
            return new Pair<>(awesomePossum.value, awesomePossum.error);
        }
        return switch (name) {
            case ACCESS     ->    access((Obj) argx[0]);
            case DICTIONARY ->    dictionary();
            case ALIST      ->    alist();
            case TYPE       ->    type();
            case ASTRING    ->    astring();
            case NUMBER     ->    number();
            case BOOL       ->    bool();
            case ANULL      ->    anull();
            case COPY       ->    copy();
            case FUNCTION   ->    function();
            case TOSTRING   ->    toString();
            default -> new Value(value).getattr(name, argx);
        };
    }

    public Obj dictionary() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("dictionary");
        if (func == null)
            return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj alist() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("list");
        if (func == null)
            return new PList(new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new PList(new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj tstr(CMethod func) {
        RTResult res = new RTResult();
        if (func == null)
            return new Str(value.displayName).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Str(value.displayName).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj type() {
        CMethod func = value.symbolTable.getbin("type");
        return tstr(func);
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
        Obj x = res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Num(0).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj bytes() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("bytes");
        if (func == null)
            return new Bytes(new byte[0]).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Bytes(new byte[0]).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj bool() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("boolean");
        if (func == null)
            return new Bool(true).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Bool(true).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj anull() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("null");
        if (func == null)
            return new Null().set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new Null().set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj copy() {
        RTResult res = new RTResult();
        CMethod func = value.symbolTable.getbin("copy");
        if (func == null)
            return new ClassInstance(value).set_context(context).set_pos(pos_start, pos_end);
        Obj x = res.register(func.execute(new ArrayList<>(), new Interpreter()));
        if (res.error != null)
            return new ClassInstance(value).set_context(context).set_pos(pos_start, pos_end);
        return x;
    }

    public Obj function() {
        return this;
    }

    public String toString() { return (String) (astring().value); }

}
