package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.Num;
import lemon.jpizza.Objects.Primitives.PList;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.Arrays;

public class ForNode extends Node {
    public Token var_name_tok;
    public Node start_value_node;
    public Node end_value_node;
    public Node step_value_node;
    public Node body_node;
    public boolean retnull;

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

    @SuppressWarnings("DuplicatedCode")
    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj startNode = res.register(start_value_node.visit(inter, context));
        if (res.shouldReturn()) return res;
        if (startNode.jptype != Constants.JPType.Number) return res.failure(new RTError(
                startNode.pos_start, startNode.pos_end,
                "Start must be an integer!",
                context
        ));
        double start = ((Num) startNode).trueValue();
        Obj endNode = res.register(end_value_node.visit(inter, context));
        if (res.shouldReturn()) return res;
        if (endNode.jptype != Constants.JPType.Number) return res.failure(new RTError(
                endNode.pos_start, endNode.pos_end,
                "Start must be an integer!",
                context
        ));
        double end = ((Num) endNode).trueValue();
        if (res.shouldReturn()) return res;

        double step;
        if (step_value_node != null) {
            Obj stepNode = res.register(step_value_node.visit(inter, context));
            if (res.shouldReturn()) return res;
            if (stepNode.jptype != Constants.JPType.Number) return res.failure(new RTError(
                    stepNode.pos_start, stepNode.pos_end,
                    "Start must be an integer!",
                    context
            ));
            step = ((Num) stepNode).trueValue();
        } else {
            step = 1;
        }
        long round = Math.round((end - start) / step);
        Obj[] elements = new Obj[(int) round];

        double i = start;
        int index = 0;
        Interpreter.Condition condition = step >= 0 ? x -> x < end : x -> x > end;

        String vtk = (String) var_name_tok.value;
        Obj value;

        context.symbolTable.define(vtk, new Null());
        // clock.tick();
        while (condition.go(i)) {
            context.symbolTable.set(vtk, new Num(i));
            i += step;

            value = res.register(body_node.visit(inter, context));
            // value = null;
            if (res.shouldReturn() && !res.continueLoop && !res.breakLoop) return res;

            if (res.continueLoop) continue;
            if (res.breakLoop) break;

            elements[index] = value;
            index++;
        }
        context.symbolTable.remove(vtk);

        return res.success(
                retnull ? new Null() : new PList(new ArrayList<>(Arrays.asList(elements))).set_context(context)
                        .set_pos(pos_start, pos_end)
        );
    }

}
