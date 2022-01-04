package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;

import java.util.List;

public class ThrowNode extends Node {
    public final Node thrown;
    public final Node throwType;

    public ThrowNode(Node throwType, Node thrown) {
        this.thrown = thrown;
        this.throwType = throwType;
        pos_start = throwType.pos_start; pos_end = thrown.pos_end;
        jptype = JPType.Throw;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj err = res.register(inter.visit(thrown, context));
        if (res.error != null)
            return res;

        Obj type = res.register(inter.visit(throwType, context));
        if (res.error != null)
            return res;

        return res.failure(new RTError(
                type.toString(),
                pos_start, pos_end,
                err.toString(),
                context
        ));
    }

    @Override
    public Node optimize() {
        return new ThrowNode(throwType.optimize(), thrown.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return List.of(throwType, thrown);
    }

    @Override
    public String visualize() {
        return "throw";
    }
}
