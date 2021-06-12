package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Position;

public class BreakNode extends Node {
    public BreakNode(Position start_pos, Position end_pos) {
        this.pos_start = start_pos; this.pos_end = end_pos;
    }
}
