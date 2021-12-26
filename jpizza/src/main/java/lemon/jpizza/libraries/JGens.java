package lemon.jpizza.libraries;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.Error;
import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Num;
import lemon.jpizza.objects.primitives.PList;
import lemon.jpizza.Pair;
import lemon.jpizza.Point3;
import lemon.jpizza.results.RTResult;

import java.util.Arrays;
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

        Obj s = res.register(checkType(execCtx.symbolTable.get("start"), "number", JPType.Number));
        Obj e = res.register(checkType(execCtx.symbolTable.get("stop"), "number", JPType.Number));
        Obj sp = res.register(checkType(execCtx.symbolTable.get("step"), "number", JPType.Number));

        if (res.error != null) return new Pair<>(null, res.error);

        double start = s.number;
        double stop = e.number;
        double step = sp.number;

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

        Obj s = res.register(checkType(execCtx.symbolTable.get("slope"), "number", JPType.Number));
        Obj y = res.register(checkType(execCtx.symbolTable.get("y-inter"), "number", JPType.Number));
        if (res.error != null) return res;
        double m = s.number;
        double b = y.number;

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

        Obj A = res.register(checkType(execCtx.symbolTable.get("a"), "number", JPType.Number));
        Obj B = res.register(checkType(execCtx.symbolTable.get("b"), "number", JPType.Number));
        Obj C = res.register(checkType(execCtx.symbolTable.get("c"), "number", JPType.Number));
        if (res.error != null) return res;
        double a = A.number;
        double b = B.number;
        double c = C.number;

        int index = 0;
        Num[] l = r.a.b;
        for (double x = r.a.a.x; x <= r.a.a.y; x = x + r.a.a.z) {
            l[index] = new Num(a * x * x + b * x + c);
            index++;
        }
        return res.success(new PList(Arrays.asList(l)));
    }

}
