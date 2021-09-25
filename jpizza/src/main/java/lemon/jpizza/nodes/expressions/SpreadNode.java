package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.nodes.Node;

public class SpreadNode extends Node {
    final Node internal;
    public SpreadNode(Node internal) {
        this.internal = internal;
        pos_start = internal.pos_start; pos_end = internal.pos_end;
        this.jptype = Constants.JPType.Spread;
    }
}
