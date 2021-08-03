package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Token;

public class NullNode extends ValueNode {
    public NullNode(Token tok) {
        super(tok);
        jptype = Constants.JPType.Null;
    }
}
