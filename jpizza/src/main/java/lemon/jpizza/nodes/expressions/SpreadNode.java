package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class SpreadNode extends Node {
    public final Node internal;
    public SpreadNode(Node internal) {
        this.internal = internal;
        pos_start = internal.pos_start; pos_end = internal.pos_end;
        this.jptype = JPType.Spread;
        constant = internal.constant;
    }

    @Override
    public Node optimize() {
        return new SpreadNode(internal.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(List.of(internal));
    }

    @Override
    public String visualize() {
        return "...";
    }
}
