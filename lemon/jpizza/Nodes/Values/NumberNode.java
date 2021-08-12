package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens;

public class NumberNode extends ValueNode {
    public double val;
    public boolean flt;

    public NumberNode(Token tok) {
        super(tok);
        val = (double) tok.value;
        flt = tok.type == Tokens.TT.FLOAT;
        jptype = Constants.JPType.Number;
    }

}
