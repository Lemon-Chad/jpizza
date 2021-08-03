package lemon.jpizza.Nodes.Variables;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

public class AttrAccessNode extends Node {
    public Token var_name_tok;

    public AttrAccessNode(Token var_name_tok) {
        this.var_name_tok = var_name_tok;
        pos_start = var_name_tok.pos_start.copy(); pos_end = var_name_tok.pos_end.copy();
        jptype = Constants.JPType.AttrAccess;
    }

}
