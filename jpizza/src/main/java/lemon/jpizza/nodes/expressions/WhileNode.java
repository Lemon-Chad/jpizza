package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.objects.primitives.PList;
import lemon.jpizza.results.RTResult;

import java.util.ArrayList;
import java.util.List;

public class WhileNode extends Node {
    public final Node condition_node;
    public final Node body_node;
    public final boolean retnull;
    public final boolean conLast;
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
                condition = res.register(inter.visit(conditionNode, context));
                if (res.shouldReturn()) return res;

                if (!condition.bool().boolval) break;
            }

            value = res.register(inter.visit(body_node, context));
            if (res.shouldReturn() && !res.continueLoop && !res.breakLoop) return res;

            if (res.continueLoop) continue;
            if (res.breakLoop) break;

            if (!retnull)
                elements.add(value);

            if (conLast) {
                condition = res.register(inter.visit(conditionNode, context));
                if (res.shouldReturn()) return res;

                if (!condition.bool().boolval) break;
            }
        }

        return res.success(retnull ? new Null() : new PList(new ArrayList<>(elements)).set_context(context)
                .set_pos(pos_start, pos_end));
    }

}
