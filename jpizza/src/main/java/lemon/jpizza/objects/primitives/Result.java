package lemon.jpizza.objects.primitives;

import lemon.jpizza.JPType;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.Value;
import lemon.jpizza.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Result extends Value {
    String failure = null;
    String failtype = null;
    Obj success = new Null();

    public Result(String failure, String failtype) {
        this.failure = failure;
        this.failtype = failtype;
        this.jptype = JPType.Res;
    }

    public Result(Obj success) {
        this.success = success;
        this.jptype = JPType.Res;
    }

    // Functions

    public boolean ok() {
        return failure == null;
    }

    public Obj resolve() {
        return success;
    }

    public PList fail() {
        return failure != null ? new PList(Arrays.asList(
                new Str(failtype),
                new Str(failure)
        )) : new PList(new ArrayList<>());
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
            return new Result(failure, failtype)
                        .set_context(this.context)
                        .set_pos(pos_start, pos_end);
    }
    public Obj type() {
        return new Str("catcher").set_context(context).set_pos(pos_start, pos_end);
    }
    public String toString() {
        if (ok())
            return "("+success.toString()+")";
        else
            return String.format("(%s:%s)", failtype, failure);
    }

}
