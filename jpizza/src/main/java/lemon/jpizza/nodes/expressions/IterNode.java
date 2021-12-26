package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.objects.primitives.PList;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.List;

public class IterNode extends Node {
    public final Token var_name_tok;
    public final Node iterable_node;
    public final Node body_node;
    public final boolean retnull;

    public IterNode(Token var_name_tok, Node iterable_node, Node body_node,
                    boolean retnull) {
        this.var_name_tok = var_name_tok;
        this.iterable_node = iterable_node;
        this.body_node = body_node;
        this.retnull = retnull;

        pos_start = var_name_tok.pos_start.copy(); pos_end = body_node.pos_end.copy();
        jptype = JPType.Iter;
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        List<Obj> elements = new ArrayList<>();

        Obj iterableNode = res.register(inter.visit(iterable_node, context));
        if (res.shouldReturn()) return res;
        if (iterableNode.jptype != JPType.List) return res.failure(RTError.Type(
                iterableNode.pos_start, iterableNode.pos_end,
                "Value must be an iterable",
                context
        ));
        List<Obj> iterable = iterableNode.list;

        double size = iterable.size();

        String vtk = (String) var_name_tok.value;
        Obj value;

        context.symbolTable.define(vtk, new Null());
        // clock.tick();
        for (int i = 0; i < size; i++) {
            context.symbolTable.set(vtk, iterable.get(i));

            value = res.register(inter.visit(body_node, context));
            if (res.shouldReturn() && !res.continueLoop && !res.breakLoop) return res;

            if (res.continueLoop) continue;
            if (res.breakLoop) break;

            if (!retnull)
                elements.add(value);
        }

        context.symbolTable.remove(vtk);

        return res.success(
                retnull ? new Null() : new PList(new ArrayList<>(elements)).set_context(context)
                        .set_pos(pos_start, pos_end)
        );
    }

    @Override
    public Node optimize() {
        Node iterable = iterable_node.optimize();
        Node body = body_node.optimize();
        return new IterNode(var_name_tok, iterable, body, retnull);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(iterable_node);
        children.add(body_node);
        return children;
    }

    @Override
    public String visualize() {
        return "iter(" + var_name_tok.value + ")";
    }
}
