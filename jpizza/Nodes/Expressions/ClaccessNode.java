package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

public class ClaccessNode extends Node {
    public Node class_tok;
    public Token attr_name_tok;

    public ClaccessNode(Node cls, Token atr) {
        class_tok = cls;
        attr_name_tok = atr;
        pos_start = cls.pos_start; pos_end = atr.pos_end;
    }

}
