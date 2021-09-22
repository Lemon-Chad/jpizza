package lemon.jpizza.objects;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.Operations;
import lemon.jpizza.Position;

import java.io.Serializable;

public abstract class Obj implements Serializable {
    public Object value;
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

    public abstract Object getattr(Operations.OP name, Object... argx);

    public abstract Obj type();
    public abstract Obj copy();

    public abstract Obj set_context(Context value);
    public abstract Obj set_context();

}
