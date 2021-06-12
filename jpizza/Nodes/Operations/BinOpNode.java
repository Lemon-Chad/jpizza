package lemon.jpizza.Nodes.Operations;

import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

public class BinOpNode extends Node {
    public Node left_node;
    public Token op_tok;
    public Node right_node;

    public BinOpNode(Node left_node, Token op_tok, Node right_node) {
        this.left_node = left_node;
        this.op_tok = op_tok;
        this.right_node = right_node;

        pos_start = left_node.pos_start; pos_end = right_node.pos_end;
    }

    public String toString() { return String.format("(%s, %s, %s)", left_node, op_tok, right_node); }

}
