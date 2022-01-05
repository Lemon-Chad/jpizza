package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ReturnNode extends Node {
    public final Node nodeToReturn;

    public ReturnNode(Node nodeToReturn, @NotNull Position pos_start, @NotNull Position pos_end) {
        this.nodeToReturn = nodeToReturn;
        this.pos_start = pos_start.copy(); this.pos_end = pos_end.copy();
        jptype = JPType.Return;
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

    @Override
    public Node optimize() {
        return new ReturnNode(nodeToReturn != null ? nodeToReturn.optimize() : null, pos_start, pos_end);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(List.of(nodeToReturn));
    }

    @Override
    public String visualize() {
        return "return";
    }
}
