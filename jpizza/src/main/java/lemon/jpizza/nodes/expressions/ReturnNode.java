package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;

public class ReturnNode extends Node {
    final Node nodeToReturn;
    final boolean newline;

    public ReturnNode(Node n, Position ps, Position pe) {
        this(n, ps, pe, false);
    }

    public ReturnNode(Node nodeToReturn, Position pos_start, Position pos_end, boolean newline) {
        this.nodeToReturn = nodeToReturn;
        this.pos_start = pos_start.copy(); this.pos_end = pos_end.copy();
        this.newline = newline;
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
