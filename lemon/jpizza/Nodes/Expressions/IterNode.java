package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

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

}
