package lemon.jpizza.libraries;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.objects.primitives.Num;
import lemon.jpizza.results.RTResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

@SuppressWarnings("unused")
public class Time extends Library {

    public Time(String name) { super(name, "time"); }

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
        if (value.jptype != Constants.JPType.Number) return new RTResult().failure(RTError.Type(
                pos_start.copy(), pos_end.copy(),
                "Argument must be a number",
                execCtx
        ));
        try {
            Thread.sleep(Math.round(value.number));
        } catch (InterruptedException e) {
            return new RTResult().failure(RTError.Interrupted(
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
        if (value.jptype != Constants.JPType.Function) return new RTResult().failure(RTError.Type(
                pos_start.copy(), pos_end.copy(),
                "Argument must be a function",
                execCtx
        ));
        long start = System.currentTimeMillis();
        res.register(value.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new Interpreter()));
        if (res.error != null) return res;
        return res.success(new Num(System.currentTimeMillis() - start));
    }

    public RTResult execute_epoch(Context execCtx) {
        return new RTResult().success(new Num(System.currentTimeMillis()));
    }
}
