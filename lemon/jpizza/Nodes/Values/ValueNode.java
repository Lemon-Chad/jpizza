package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

public class ValueNode extends Node {
    public Token tok;

    public ValueNode(Token tok) {
        this.tok = tok;
        pos_start = tok.pos_start.copy(); pos_end = tok.pos_end.copy();
        jptype = Constants.JPType.Value;
    }

    public String toString() {
        return tok.toString();
    }

}
