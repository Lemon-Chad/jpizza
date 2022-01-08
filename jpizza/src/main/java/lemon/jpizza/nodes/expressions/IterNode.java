package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
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
