package lemon.jpizza.generators;

import lemon.jpizza.*;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.Pair;
import lemon.jpizza.errors.Error;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.executables.*;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.*;
import lemon.jpizza.results.RTResult;

import static lemon.jpizza.Operations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unused")
public class Interpreter {

    Clock clock = new Clock();
    public boolean reflection = false;

    public static Map< Pair3<Obj, OP, Obj>, Obj > compCache = new HashMap<>();

    public Memo memo = new Memo();
    public boolean main = false;

    public String fnFinish = null;
    public String clFinish = null;

    public Interpreter(Memo memo) {
        this.memo = memo;
    }

    public Interpreter() {}

    public void makeMain() {
        main = true;
    }

    public interface Condition {
        boolean go(double x);
    }

    public RTResult finish(Context context) {
        RTResult res = new RTResult();

        if (!main) return res.success(new Null());

        Object cargs = context.symbolTable.get("CMDARGS");
        Obj cmdArgs = cargs != null ? (PList) cargs : new PList(new ArrayList<>());

        if (fnFinish != null) {
            Object func = context.symbolTable.get(fnFinish);
            if (!(func instanceof Function)) return new RTResult().failure(RTError.Scope(
                    null, null,
                    "Main function provided does not exist",
                    context
            ));

            Function fn = (Function) func;
            if (fn.argNames.size() != 1) return new RTResult().failure(RTError.ArgumentCount(
                    fn.pos_start, fn.pos_end,
                    "Function must take 1 argument (CMD line arguments)",
                    context
            ));
            res.register(fn.execute(Collections.singletonList(cmdArgs), new ArrayList<>(), new HashMap<>(), this));
            if (res.error != null) return res;
        }
        else if (clFinish != null) {
            Object cls = context.symbolTable.get(clFinish);
            if (!(cls instanceof ClassPlate)) return new RTResult().failure(RTError.Scope(
                    null, null,
                    "Main recipe provided does not exist",
                    context
            ));

            ClassPlate recipe = (ClassPlate) cls;
            if (recipe.make.argNames.size() != 0) return new RTResult().failure(RTError.ArgumentCount(
                    recipe.get_start(), recipe.get_end(),
                    "Recipe shouldn't take any arguments",
                    context
            ));

            ClassInstance clsi = (ClassInstance) res.register(recipe.execute(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), this));
            if (res.error != null) return res;

            Object func = clsi.getattr(OP.ACCESS, new Str("main").set_context(recipe.context));
            if (!(func instanceof CMethod)) return new RTResult().failure(RTError.Scope(
                    recipe.get_start(), recipe.get_end(),
                    "Recipe has no main method",
                    recipe.context
            ));

            CMethod meth = (CMethod) func;
            if (meth.argNames.size() != 1) return new RTResult().failure(RTError.Scope(
                    recipe.get_start(), recipe.get_end(),
                    "Method does not take in 1 argument",
                    recipe.context
            ));

            res.register(meth.execute(Collections.singletonList(cmdArgs), new ArrayList<>(), new HashMap<>(), this));
            if (res.error != null) return res;
        }

        return res.success(new Null());
    }

    public RTResult visit(Node node, Context context) {
        return node.visit(this, context);
    }

    public RTResult interpret(List<Node> nodes, Context context, boolean log) {
        RTResult res = new RTResult();
        ArrayList<Obj> vals = new ArrayList<>();
        for (Node node: nodes) {
            Obj v = res.register(visit(node, context));
            if (res.error != null) return res;
            if (log) vals.add(v);
        }
        return res.success(new PList(vals));
    }

    @SuppressWarnings("DuplicatedCode")
    public Pair< List<String>, List<String> > gatherArgs(List<Token> argNameToks, List<Token> argTypeToks) {
        List<String> argNames = new ArrayList<>();
        List<String> argTypes = new ArrayList<>();
        int size = argNameToks.size();
        for (int i = 0; i < size; i++) {
            argNames.add((String) argNameToks.get(i).value);
            argTypes.add((String) argTypeToks.get(i).value);
        }
        return new Pair<>(argNames, argTypes);
    }

    public Pair< RTResult, List<Obj> > getDefaults(List<Node> dfts, Context ctx) {
        RTResult res = new RTResult();
        List<Obj> defaults = new ArrayList<>();
        for (Node n : dfts)
            if (n == null)
                defaults.add(null);
            else {
                Obj val = res.register(visit(n, ctx));
                if (res.error != null) return new Pair<>(res, defaults);
                defaults.add(val);
            }
        return new Pair<>(res, defaults);
    }

    public static RTResult getThis(Object val, Context context, Position pos_start, Position pos_end) {
                while (context.displayName.hashCode() != val.hashCode()) {
                    if (context.parent == null) return new RTResult().failure(RTError.Scope(
                            pos_start, pos_end,
                            "Invalid 'this'",
                            context
                    ));
                    context = context.parent;
                } return new RTResult().success(new ClassInstance(context).copy().set_pos(pos_start, pos_end)
                        .set_context(context));
        }

    public static RTResult getImprt(String path, String fn, Context context, Position pos_start, Position pos_end)
            throws IOException {
        Pair<ClassInstance, Error> i = Shell.imprt(fn, Files.readString(Path.of(path)), context, pos_start);
        ClassInstance imp = i.a;
        Error error = i.b;
        if (error != null) return new RTResult().failure(error);
        imp.set_pos(pos_start, pos_end).set_context(context);
        return new RTResult().success(imp);
    }
}
