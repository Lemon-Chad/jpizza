package lemon.jpizza.nodes.variables;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;

import java.util.List;

public class VarNode extends Node {
    public final Object value_node;
    public final boolean locked;
    public Integer min = null;
    public Integer max = null;

    public VarNode(Object value_node, boolean locked) {
        this.value_node = value_node;
        this.locked = locked;
        jptype = JPType.Var;
    }

    public VarNode setRange(Integer min, Integer max) {
        this.min = min;
        this.max = max;
        return this;
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
        return "VarNode";
    }
}
