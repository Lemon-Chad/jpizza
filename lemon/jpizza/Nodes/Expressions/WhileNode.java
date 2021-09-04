package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Bool;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.PList;
import lemon.jpizza.Results.RTResult;

import java.util.ArrayList;
import java.util.List;

public class WhileNode extends Node {
    public Node condition_node;
    public Node body_node;
    public boolean retnull;
    public boolean conLast;
    public boolean fluctuating = true;

    public WhileNode(Node condition_node, Node body_node, boolean retnull, boolean conLast) {
        this.condition_node = condition_node;
        this.body_node = body_node;
        this.retnull = retnull;
        this.conLast = conLast;
        pos_start = condition_node.pos_start.copy(); pos_end = body_node.pos_end.copy();
        jptype = Constants.JPType.While;
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        List<Obj> elements = new ArrayList<>();

        Node conditionNode = condition_node;
        Obj condition, value;
        while (true) {
            if (!conLast) {
                condition = res.register(conditionNode.visit(inter, context));
                if (res.shouldReturn()) return res;

                if (!((Bool) condition.bool()).trueValue()) break;
            }

            value = res.register(body_node.visit(inter, context));
            if (res.shouldReturn() && !res.continueLoop && !res.breakLoop) return res;

            if (res.continueLoop) continue;
            if (res.breakLoop) break;

            if (!retnull)
                elements.add(value);

            if (conLast) {
                condition = res.register(conditionNode.visit(inter, context));
                if (res.shouldReturn()) return res;

                if (!((Bool) condition.bool()).trueValue()) break;
            }
        }

        return res.success(retnull ? new Null() : new PList(new ArrayList<>(elements)).set_context(context)
                .set_pos(pos_start, pos_end));
    }

}
