package lemon.jpizza.Nodes.Variables;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;

public class AttrNode extends Node {
    public Object value_node;

    public AttrNode(Object value_node) {
        this.value_node = value_node;
        jptype = Constants.JPType.Attr;
    }

}

// Jaws Approved!
