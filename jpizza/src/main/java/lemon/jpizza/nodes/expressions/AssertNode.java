package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AssertNode extends Node {
    public final Node condition;
    public AssertNode(Node condition) {
        this.condition = condition;
        pos_start = condition.pos_start;
        pos_end = condition.pos_end;
        jptype = JPType.Assert;
    }

    @Override
    public Node optimize() {
        return new AssertNode(condition.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(condition));
    }

    @Override
    public String visualize() {
        return "assert";
    }
}
