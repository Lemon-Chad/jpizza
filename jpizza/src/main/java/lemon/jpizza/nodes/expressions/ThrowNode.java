package lemon.jpizza.nodes.expressions;

import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;

public class ThrowNode extends Node {
    Node thrown;

    public ThrowNode(Node thrown) {
        this.thrown = thrown;
        pos_start = thrown.pos_start; pos_end = thrown.pos_end;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        Obj err = res.register(thrown.visit(inter, context));
        if (res.error != null)
            return res;
        return res.failure(new RTError(
                pos_start, pos_end,
                err.astring().toString(),
                context
        ));
    }

}
