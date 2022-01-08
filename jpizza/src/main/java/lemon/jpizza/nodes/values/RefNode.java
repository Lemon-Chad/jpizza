package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RefNode extends Node {
    public final Node inner;
    public RefNode(Node inner) {
        this.inner = inner;
        pos_start = inner.pos_start; pos_end = inner.pos_end;
        jptype = JPType.Ref;
    }

    @Override
    public Node optimize() {
        return new RefNode(inner.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(inner));
    }

    @Override
    public String visualize() {
        return "&Ref";
    }
}
