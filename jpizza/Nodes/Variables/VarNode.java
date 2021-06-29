package lemon.jpizza.Nodes.Variables;

import lemon.jpizza.Nodes.Node;

public class VarNode extends Node {
    public Object value_node;
    public boolean locked;

    public VarNode(Object value_node, boolean locked) {
        this.value_node = value_node;
        this.locked = locked;
    }

}
