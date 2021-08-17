package lemon.jpizza.Nodes.Variables;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;

public class VarNode extends Node {
    public Object value_node;
    public boolean locked;
    public Integer min = null;
    public Integer max = null;

    public VarNode(Object value_node, boolean locked) {
        this.value_node = value_node;
        this.locked = locked;
        jptype = Constants.JPType.Var;
    }

    public VarNode setRange(Integer min, Integer max) {
        this.min = min;
        this.max = max;
        return this;
    }

}
