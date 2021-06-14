package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

public class UseNode extends Node {
    public Token useToken;

    public UseNode(Token useToken) {
        this.useToken = useToken;
        pos_start = useToken.pos_start; pos_end = useToken.pos_end;
    }

}
