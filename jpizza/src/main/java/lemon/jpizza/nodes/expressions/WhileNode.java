package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class WhileNode extends Node {
    public final Node condition_node;
    public final Node body_node;
    public final boolean retnull;
    public final boolean conLast;

    public WhileNode(Node condition_node, Node body_node, boolean retnull, boolean conLast) {
        this.condition_node = condition_node;
        this.body_node = body_node;
        this.retnull = retnull;
        this.conLast = conLast;
        pos_start = condition_node.pos_start.copy(); pos_end = body_node.pos_end.copy();
        jptype = JPType.While;
    }

    @Override
    public Node optimize() {
        Node condition = condition_node.optimize();
        Node body = body_node.optimize();
        return new WhileNode(condition, body, retnull, conLast);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(condition_node);
        children.add(body_node);
        return children;
    }

    @Override
    public String visualize() {
        return "while";
    }
}
