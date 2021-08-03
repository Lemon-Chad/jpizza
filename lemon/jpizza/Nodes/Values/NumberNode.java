package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Token;

public class NumberNode extends ValueNode {
    public double val;
    public NumberNode(Token tok) {
        super(tok);
        val = (double) tok.value;
        jptype = Constants.JPType.Number;
    }
}
