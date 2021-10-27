package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.Pair;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.nodes.Node;

public class DerefNode extends Node {
    final Node ref;
    public DerefNode(Node ref) {
        this.ref = ref;

        pos_start = ref.pos_start; pos_end = ref.pos_end;
        jptype = Constants.JPType.Deref;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj ref = res.register(inter.visit(this.ref, context));
        if (res.error != null) return res;

        Pair<Obj, RTError> pair = ref.deref();
        if (pair.b != null) return res.failure(pair.b);
        return res.success(pair.a);
    }

}
