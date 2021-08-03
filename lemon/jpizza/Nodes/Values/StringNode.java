package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Token;

public class StringNode extends ValueNode {
    public String val;
    public StringNode(Token tok) {
        super(tok);
        val = (String) tok.value;
        jptype = Constants.JPType.String;
    }
}
