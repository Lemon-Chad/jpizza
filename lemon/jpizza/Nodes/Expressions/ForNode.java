package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

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

}
