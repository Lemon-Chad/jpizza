package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Token;

import java.util.List;

public class EnumNode extends ValueNode {
    public List<Token> children;
    public EnumNode(Token tok, List<Token> children) {
        super(tok);
        this.children = children;
        jptype = Constants.JPType.Enum;
    }
}
