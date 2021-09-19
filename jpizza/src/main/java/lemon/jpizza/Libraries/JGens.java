package lemon.jpizza.Libraries;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.Error;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Num;
import lemon.jpizza.Objects.Primitives.PList;
import lemon.jpizza.Pair;
import lemon.jpizza.Point3;
import lemon.jpizza.Results.RTResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

@SuppressWarnings("unused")
public class JGens extends Library {
    public JGens(String name) {
        super(name, "gens");
    }

    public static void initialize() {
        initialize("gens", JGens.class, new HashMap<>(){{
            put("range", Arrays.asList("start", "stop", "step"));
            put("linear", Arrays.asList("start", "stop", "step", "slope", "y-inter"));
            put("quadratic", Arrays.asList("start", "stop", "step", "a", "b", "c"));
        }});
    }

    public Pair<Pair<Point3, Num[]>, Error> getRange(Context execCtx) {
        RTResult res = new RTResult();

        Obj s = res.register(checkType(execCtx.symbolTable.get("start"), "number", Constants.JPType.Number));
        Obj e = res.register(checkType(execCtx.symbolTable.get("stop"), "number", Constants.JPType.Number));
        Obj sp = res.register(checkType(execCtx.symbolTable.get("step"), "number", Constants.JPType.Number));

        if (res.error != null) return new Pair<>(null, res.error);

        double start = ((Num) s).trueValue();
        double stop = ((Num) e).trueValue();
        double step = ((Num) sp).trueValue();

        Num[] l = new Num[(int) Math.ceil((stop - start) / step) + 1];
        return new Pair<>(new Pair<>(new Point3(start, stop, step), l), null);
    }

    public RTResult execute_range(Context execCtx) {
        RTResult res = new RTResult();
        var r = getRange(execCtx);
        if (r.b != null) return res.failure(r.b);
        int index = 0;
        Num[] l = r.a.b;
        for (double i = r.a.a.x; i <= r.a.a.y; i = i + r.a.a.z) {
            l[index] = new Num(i);
            index++;
        }
        return res.success(new PList(Arrays.asList(l)));
    }

    public RTResult execute_linear(Context execCtx) {
        RTResult res = new RTResult();
        var r = getRange(execCtx);
        if (r.b != null) return res.failure(r.b);

        Obj s = res.register(checkType(execCtx.symbolTable.get("slope"), "number", Constants.JPType.Number));
        Obj y = res.register(checkType(execCtx.symbolTable.get("y-inter"), "number", Constants.JPType.Number));
        if (res.error != null) return res;
        double m = ((Num) s).trueValue();
        double b = ((Num) y).trueValue();

        int index = 0;
        Num[] l = r.a.b;
        for (double x = r.a.a.x; x <= r.a.a.y; x = x + r.a.a.z) {
            l[index] = new Num(m * x + b);
            index++;
        }
        return res.success(new PList(Arrays.asList(l)));
    }

    public RTResult execute_quadratic(Context execCtx) {
        RTResult res = new RTResult();
        var r = getRange(execCtx);
        if (r.b != null) return res.failure(r.b);

        Obj A = res.register(checkType(execCtx.symbolTable.get("a"), "number", Constants.JPType.Number));
        Obj B = res.register(checkType(execCtx.symbolTable.get("b"), "number", Constants.JPType.Number));
        Obj C = res.register(checkType(execCtx.symbolTable.get("c"), "number", Constants.JPType.Number));
        if (res.error != null) return res;
        double a = ((Num) A).trueValue();
        double b = ((Num) B).trueValue();
        double c = ((Num) C).trueValue();

        int index = 0;
        Num[] l = r.a.b;
        for (double x = r.a.a.x; x <= r.a.a.y; x = x + r.a.a.z) {
            l[index] = new Num(a * x * x + b * x + c);
            index++;
        }
        return res.success(new PList(Arrays.asList(l)));
    }

}
