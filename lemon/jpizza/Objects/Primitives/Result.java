package lemon.jpizza.Objects.Primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Pair;

import java.util.Arrays;
import java.util.HashMap;

public class Result extends Value {
    String failure = null;
    Obj success = new Null();

    public Result(String failure) {
        this.failure = failure;
        this.jptype = Constants.JPType.Res;
    }

    public Result(Obj success) {
        this.success = success;
        this.jptype = Constants.JPType.Res;
    }

    // Functions

    public boolean ok() {
        return failure == null;
    }

    public Obj resolve() {
        return success;
    }

    public Str fail() {
        return failure != null ? new Str(failure) : new Str("");
    }

    // Methods

    public Pair<Obj, RTError> eq(Obj o) {
        return new Pair<>(new Bool(false), null);
    }

    // Conversions

    public Obj dictionary() {
        return new Dict(new HashMap<>(){{
            put(new Str("value"), success);
            put(new Str("error"), fail());
        }}).set_context(context).set_pos(pos_start, pos_end);
    }
    public Obj function() {
        return new Null().function();
    }
    public Obj number() {
        return new Num(ok() ? 1 : 0)
                    .set_context(context).set_pos(pos_start, pos_end);
    }
    public Obj bool() {
        return new Bool(ok())
            .set_context(context).set_pos(pos_start, pos_end);
    }
    public Obj alist() {
        return new PList(Arrays.asList(fail(), success))
                .set_context(context)
                .set_pos(pos_start, pos_end);
    }

    // Defaults

    public Obj copy() {
        if (ok())
            return new Result(success)
                        .set_context(this.context)
                        .set_pos(pos_start, pos_end);
        else
            return new Result(failure)
                        .set_context(this.context)
                        .set_pos(pos_start, pos_end);
    }
    public Obj type() {
        return new Str("catcher").set_context(context).set_pos(pos_start, pos_end);
    }
    public String toString() {
        if (ok())
            return success.toString();
        else
            return failure;
    }

}
