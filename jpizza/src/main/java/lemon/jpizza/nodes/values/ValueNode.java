package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.List;

public class ValueNode extends Node {
    public final Token tok;

    public ValueNode(Token tok) {
        this.tok = tok;
        pos_start = tok.pos_start; pos_end = tok.pos_end;
        jptype = JPType.Value;
        constant = true;
    }

    public String toString() {
        return tok.toString();
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public String visualize() {
        return toString();
    }
}
