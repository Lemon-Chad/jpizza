package lemon.jpizza.Libraries;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.Num;
import lemon.jpizza.Results.RTResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

@SuppressWarnings("unused")
public class Time extends Library {

    public Time(String name) { super(name); }

    public static void initialize() {
        initialize("time", Time.class, new HashMap<>(){{
            put("halt", Collections.singletonList("ms"));
            put("stopwatch", Collections.singletonList("func"));
            put("epoch", new ArrayList<>());
        }});
    }

    public RTResult execute_halt(Context execCtx) {
        Obj value = (Obj) execCtx.symbolTable.get("ms");
        value = value.number();
        if (value.jptype != Constants.JPType.Number) return new RTResult().failure(new RTError(
                pos_start.copy(), pos_end.copy(),
                "Argument must be a number",
                execCtx
        ));
        try {
            Thread.sleep(Math.round(((Num) value).trueValue()));
        } catch (InterruptedException e) {
            return new RTResult().failure(new RTError(
                    pos_start.copy(), pos_end.copy(),
                    "Delay was interrupted",
                    execCtx
            ));
        }
        return new RTResult().success(new Null());
    }

    public RTResult execute_stopwatch(Context execCtx) {
        Obj value = (Obj) execCtx.symbolTable.get("func");
        RTResult res = new RTResult();
        value = value.function();
        if (value.jptype != Constants.JPType.Function) return new RTResult().failure(new RTError(
                pos_start.copy(), pos_end.copy(),
                "Argument must be a function",
                execCtx
        ));
        long start = System.currentTimeMillis();
        res.register(((Function) value).execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null) return res;
        return res.success(new Num(System.currentTimeMillis() - start));
    }

    public RTResult execute_epoch(Context execCtx) {
        return new RTResult().success(new Num(System.currentTimeMillis()));
    }
}
