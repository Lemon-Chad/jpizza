package lemon.jpizza.nodes.definitions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VarAssignNode extends Node {
    public final Token var_name_tok;
    public final Node value_node;
    public final boolean locked;
    public boolean defining;
    public Integer min = null;
    public Integer max = null;
    public List<String> type;

    public VarAssignNode setType(List<String> type) {
        this.type = type;
        return this;
    }

    public VarAssignNode setDefining(boolean defining) {
        this.defining = defining;
        return this;
    }

    public VarAssignNode setRange(Integer min, Integer max) {
        this.max = max;
        this.min = min;
        return this;
    }

    public VarAssignNode(Token var_name_tok, Node value_node) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;

        locked = false;
        defining = true;
        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = JPType.VarAssign;
    }

    public VarAssignNode(Token var_name_tok, Node value_node, boolean locked) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;
        this.locked = locked;

        defining = true;
        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = JPType.VarAssign;
    }

    @SuppressWarnings("unused")
    public VarAssignNode(Token var_name_tok, Node value_node, boolean defining, int _x) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;
        locked = false;

        this.defining = defining;
        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = JPType.VarAssign;
    }

    @Override
    public Node optimize() {
        Node val = value_node.optimize();
        return new VarAssignNode(var_name_tok, val, locked)
                .setDefining(defining)
                .setRange(min, max)
                .setType(type);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(value_node));
    }

    @Override
    public String visualize() {
        return "var " + var_name_tok.value;
    }
}
