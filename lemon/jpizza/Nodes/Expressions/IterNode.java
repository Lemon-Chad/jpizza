package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.PList;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.List;

public class IterNode extends Node {
    public Token var_name_tok;
    public Node iterable_node;
    public Node body_node;
    public boolean retnull;

    public IterNode(Token var_name_tok, Node iterable_node, Node body_node,
                    boolean retnull) {
        this.var_name_tok = var_name_tok;
        this.iterable_node = iterable_node;
        this.body_node = body_node;
        this.retnull = retnull;

        pos_start = var_name_tok.pos_start.copy(); pos_end = body_node.pos_end.copy();
        jptype = Constants.JPType.Iter;
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        List<Obj> elements = new ArrayList<>();

        Obj iterableNode = res.register(iterable_node.visit(inter, context));
        if (res.shouldReturn()) return res;
        if (iterableNode.jptype != Constants.JPType.List) return res.failure(new RTError(
                iterableNode.pos_start, iterableNode.pos_end,
                "Value must be an iterable!",
                context
        ));
        List<Obj> iterable = ((PList) iterableNode).trueValue();

        double size = iterable.size();

        String vtk = (String) var_name_tok.value;
        Obj value;

        context.symbolTable.define(vtk, new Null());
        // clock.tick();
        for (int i = 0; i < size; i++) {
            context.symbolTable.set(vtk, iterable.get(i));

            value = res.register(body_node.visit(inter, context));
            if (res.shouldReturn() && !res.continueLoop && !res.breakLoop) return res;

            if (res.continueLoop) continue;
            if (res.breakLoop) break;

            elements.add(value);
        }

        context.symbolTable.remove(vtk);

        return res.success(
                retnull ? new Null() : new PList(new ArrayList<>(elements)).set_context(context)
                        .set_pos(pos_start, pos_end)
        );
    }

}
