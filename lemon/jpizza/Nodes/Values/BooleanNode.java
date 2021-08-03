package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Token;

public class BooleanNode extends ValueNode {
    public boolean val;
    public BooleanNode(Token tok) {
        super(tok);
        val = (boolean) tok.value;
        jptype = Constants.JPType.Boolean;
    }
}
