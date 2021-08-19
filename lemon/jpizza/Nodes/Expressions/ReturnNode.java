package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;

public class ReturnNode extends Node {
    public Node nodeToReturn;
    public boolean fluctuating = true;

    public ReturnNode(Node nodeToReturn, Position pos_start, Position pos_end) {
        this.nodeToReturn = nodeToReturn;
        this.pos_start = pos_start.copy(); this.pos_end = pos_end.copy();
        jptype = Constants.JPType.Return;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Node ret = nodeToReturn;
        Obj value;
        if (ret != null) {
            value = res.register(ret.visit(inter, context));
            if (res.shouldReturn()) return res;
        } else value = new Null();

        return res.sreturn(value);
    }
}
