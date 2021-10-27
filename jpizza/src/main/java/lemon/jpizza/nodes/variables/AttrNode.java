package lemon.jpizza.nodes.variables;

import lemon.jpizza.Constants;
import lemon.jpizza.nodes.Node;

public class AttrNode extends Node {
    public final Object value_node;

    public AttrNode(Object value_node) {
        this.value_node = value_node;
        jptype = Constants.JPType.Attr;
    }

}

// Jaws Approved!
