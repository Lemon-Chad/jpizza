package lemon.jpizza.nodes.variables;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;

import java.util.List;

public class AttrNode extends Node {
    public final Object value_node;

    public AttrNode(Object value_node) {
        this.value_node = value_node;
        jptype = JPType.Attr;
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return List.of();
    }

    @Override
    public String visualize() {
        return "AttrNode";
    }
}

// Jaws Approved!
// Die
