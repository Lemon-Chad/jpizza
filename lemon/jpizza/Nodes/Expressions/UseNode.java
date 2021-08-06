package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

import java.util.List;

public class UseNode extends Node {
    public Token useToken;
    public List<Token> args;

    public UseNode(Token useToken, List<Token> args) {
        this.useToken = useToken;
        this.args = args;
        pos_start = useToken.pos_start.copy(); pos_end = useToken.pos_end.copy();
        jptype = Constants.JPType.Use;
    }

}
