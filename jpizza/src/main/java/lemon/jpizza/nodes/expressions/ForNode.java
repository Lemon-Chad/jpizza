package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.objects.primitives.Num;
import lemon.jpizza.objects.primitives.PList;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.Arrays;

public class ForNode extends Node {
    public final Token var_name_tok;
    public final Node start_value_node;
    public final Node end_value_node;
    public final Node step_value_node;
    public final Node body_node;
    public final boolean retnull;

    public ForNode(Token var_name_tok, Node start_value_node, Node end_value_node, Node step_value_node, Node body_node,
                   boolean retnull) {
        this.var_name_tok = var_name_tok;
        this.start_value_node = start_value_node;
        this.end_value_node = end_value_node;
        this.step_value_node = step_value_node;
        this.body_node = body_node;
        this.retnull = retnull;

        pos_start = var_name_tok.pos_start.copy(); pos_end = body_node.pos_end.copy();
        jptype = Constants.JPType.For;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj startNode = res.register(inter.visit(start_value_node, context));
        if (res.shouldReturn()) return res;
        if (startNode.jptype != Constants.JPType.Number) return res.failure(RTError.Type(
                startNode.pos_start, startNode.pos_end,
                "Start must be an integer",
                context
        ));
        double start = startNode.number;
        Obj endNode = res.register(inter.visit(end_value_node, context));
        if (res.shouldReturn()) return res;
        if (endNode.jptype != Constants.JPType.Number) return res.failure(RTError.Type(
                endNode.pos_start, endNode.pos_end,
                "End must be an integer",
                context
        ));
        double end = endNode.number;
        if (res.shouldReturn()) return res;

        double step;
        if (step_value_node != null) {
            Obj stepNode = res.register(inter.visit(step_value_node, context));
            if (res.shouldReturn()) return res;
            if (stepNode.jptype != Constants.JPType.Number) return res.failure(RTError.Type(
                    stepNode.pos_start, stepNode.pos_end,
                    "Step must be an integer",
                    context
            ));
            step = stepNode.number;
        }
        else {
            step = 1;
        }
        long round = Math.round((end - start) / step);
        Obj[] elements = new Obj[0];
        if (!retnull)
            elements = new Obj[(int) round];

        double i = start;
        int index = 0;
        Interpreter.Condition condition;

        if (step >= 0)
            condition = x -> x < end;
        else
            condition = x -> x > end;

        String vtk = (String) var_name_tok.value;
        Obj value;

        context.symbolTable.define(vtk, new Null());
        // clock.tick();
        while (condition.go(i)) {
            if (!vtk.equals("_"))
                context.symbolTable.set(vtk, new Num(i));
            i += step;

            value = res.register(inter.visit(body_node, context));
            if (res.continueLoop) continue;
            else if (res.breakLoop) break;
            else if (res.error != null || res.funcReturn != null) return res;

            if (!retnull) {
                elements[index] = value;
                index++;
            }
        }
        context.symbolTable.remove(vtk);

        if (retnull)
            return res.success(new Null());
        Obj lst = new PList(new ArrayList<>(Arrays.asList(elements))).set_context(context).set_pos(pos_start, pos_end);
        return res.success(lst);
    }

}
