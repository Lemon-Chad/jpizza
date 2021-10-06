package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

public class AssertNode extends Node {
    final Node condition;
    public AssertNode(Node condition) {
        this.condition = condition;
        pos_start = condition.pos_start;
        pos_end = condition.pos_end;
        jptype = Constants.JPType.Assert;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj conditional = res.register(condition.visit(inter, context));
        if (res.error != null) return res;
        boolean value = conditional.bool().boolval;
        if (value)
            return res.success(new Null());
        return res.failure(RTError.Assertion(pos_start, pos_end, "Assertion failed",
                context));

    }

}