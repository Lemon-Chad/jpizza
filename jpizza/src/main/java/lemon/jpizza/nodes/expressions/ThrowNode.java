package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ThrowNode extends Node {
    public final Node thrown;
    public final Node throwType;

    public ThrowNode(Node throwType, Node thrown) {
        this.thrown = thrown;
        this.throwType = throwType;
        pos_start = throwType.pos_start; pos_end = thrown.pos_end;
        jptype = JPType.Throw;
    }

    @Override
    public Node optimize() {
        return new ThrowNode(throwType.optimize(), thrown.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Arrays.asList(throwType, thrown));
    }

    @Override
    public String visualize() {
        return "throw";
    }
}
