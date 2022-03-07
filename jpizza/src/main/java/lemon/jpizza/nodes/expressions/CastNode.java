package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.Token;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class CastNode extends Node {
    public final Node expr;
    public final Token type;

    public CastNode(Node expr, Token type) {
        this.expr = expr;
        this.type = type;
        pos_start = type.pos_start;
        pos_end = expr.pos_end;
        jptype = JPType.Cast;
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(expr);
        return children;
    }

    @Override
    public String visualize() {
        return "| " + String.join("", (List<String>) type.value) + " |";
    }
}
