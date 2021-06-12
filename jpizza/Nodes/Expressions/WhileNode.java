package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Nodes.Node;

public class WhileNode extends Node {
    public Node condition_node;
    public Node body_node;
    public boolean retnull;

    public WhileNode(Node condition_node, Node body_node, boolean retnull) {
        this.condition_node = condition_node;
        this.body_node = body_node;
        this.retnull = retnull;
        pos_start = condition_node.pos_start; pos_end = body_node.pos_end;
    }

}
