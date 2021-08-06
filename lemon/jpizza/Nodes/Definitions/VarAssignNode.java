package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Token;

public class VarAssignNode extends Node {
    public Token var_name_tok;
    public Node value_node;
    public boolean locked;
    public boolean defining;
    public String type;

    public VarAssignNode setType(String type) {
        this.type = type;
        return this;
    }

    public VarAssignNode setDefining(boolean defining) {
        this.defining = defining;
        return this;
    }

    public VarAssignNode(Token var_name_tok, Node value_node) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;

        locked = false;
        defining = true;
        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = Constants.JPType.VarAssign;
    }

    public VarAssignNode(Token var_name_tok, Node value_node, boolean locked) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;
        this.locked = locked;

        defining = true;
        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = Constants.JPType.VarAssign;
    }

    @SuppressWarnings("unused")
    public VarAssignNode(Token var_name_tok, Node value_node, boolean defining, int _x) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;
        locked = false;

        this.defining = defining;
        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = Constants.JPType.VarAssign;
    }

}