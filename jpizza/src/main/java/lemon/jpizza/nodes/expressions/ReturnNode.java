package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;
import org.jetbrains.annotations.NotNull;

public class ReturnNode extends Node {
    public final Node nodeToReturn;

    public ReturnNode(Node n, @NotNull Position ps, @NotNull Position pe) {
        this(n, ps, pe, false);
    }

    public ReturnNode(Node nodeToReturn, @NotNull Position pos_start, @NotNull Position pos_end, boolean newline) {
        this.nodeToReturn = nodeToReturn;
        this.pos_start = pos_start.copy(); this.pos_end = pos_end.copy();
        jptype = Constants.JPType.Return;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Node ret = nodeToReturn;
        Obj value;
        if (ret != null) {
            value = res.register(inter.visit(ret, context));
            if (res.shouldReturn()) return res;
        }
        else value = new Null();

        return res.sreturn(value);
    }
}
