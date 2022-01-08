package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Token;

import java.util.Arrays;
import java.util.List;

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
        jptype = JPType.For;
    }

    @Override
    public Node optimize() {
        Node start = start_value_node.optimize();
        Node end = end_value_node.optimize();
        Node step = null;
        if (step_value_node != null)
            step = step_value_node.optimize();
        Node body = body_node.optimize();
        return new ForNode(var_name_tok, start, end, step, body, retnull);
    }

    @Override
    public List<Node> getChildren() {
        return Arrays.asList(start_value_node, end_value_node, step_value_node, body_node);
    }

    @Override
    public String visualize() {
        return "for(" + var_name_tok.value + ")";
    }
}
