package lemon.jpizza.Objects;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Position;
import lemon.jpizza.Double;

public abstract class Obj {
    public Object value;
    public Position pos_start, pos_end;
    public Context context;

    public Object getValue() { return value; }

    public abstract Obj set_pos(Position pos_start, Position pos_end);
    public abstract Obj set_pos(Position pos_start);
    public abstract Obj set_pos();

    public abstract Obj number();
    public abstract Obj alist();
    public abstract Obj bool();
    public abstract Obj anull();
    public abstract Obj function();
    public abstract Obj dictionary();
    public abstract Obj astring();

    public abstract Object getattr(String name, Object... argx);

    public abstract Obj type();
    public abstract Obj copy();

    public abstract Obj set_context(Context value);
    public abstract Obj set_context();

}
