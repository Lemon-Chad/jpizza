package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Results.RTResult;

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
