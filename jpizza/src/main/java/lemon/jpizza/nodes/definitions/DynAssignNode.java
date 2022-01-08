package lemon.jpizza.nodes.definitions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DynAssignNode extends Node {
    public final Token var_name_tok;
    public final Node value_node;

    public DynAssignNode(Token var_name_tok, Node value_node) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;

        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = JPType.DynAssign;
    }

    @Override
    public Node optimize() {
        return new DynAssignNode(var_name_tok, value_node.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(value_node));
    }

    @Override
    public String visualize() {
        return "macro";
    }
}
