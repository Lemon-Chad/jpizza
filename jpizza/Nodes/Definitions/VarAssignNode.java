package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

public class VarAssignNode extends Node {
    public Token var_name_tok;
    public Node value_node;
    public boolean locked;

    public VarAssignNode(Token var_name_tok, Node value_node) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;

        locked = false;
        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
    }

    public VarAssignNode(Token var_name_tok, Node value_node, boolean locked) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;
        this.locked = locked;

        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
    }

}
