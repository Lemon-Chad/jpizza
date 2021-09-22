package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Token;

public class ValueNode extends Node {
    public Token tok;

    public ValueNode(Token tok) {
        this.tok = tok;
        pos_start = tok.pos_start; pos_end = tok.pos_end;
        jptype = Constants.JPType.Value;
    }

    public String toString() {
        return tok.toString();
    }

}
