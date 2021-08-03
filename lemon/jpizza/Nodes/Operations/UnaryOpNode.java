package lemon.jpizza.Nodes.Operations;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

public class UnaryOpNode extends Node {
    public Token op_tok;
    public Node node;

    public UnaryOpNode(Token op_tok, Node node) {
        this.node = node;
        this.op_tok = op_tok;

        pos_start = op_tok.pos_start.copy(); pos_end = node.pos_end.copy();
        jptype = Constants.JPType.UnaryOp;
    }

    public String toString() { return String.format("(%s, %s)", op_tok, node); }

}
