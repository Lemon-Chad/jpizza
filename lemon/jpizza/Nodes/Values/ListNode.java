package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Position;

import java.util.List;

public class ListNode extends Node {
    public List<Node> elements;

    public ListNode(List<Node> element_nodes, Position pos_start, Position pos_end) {
        elements = element_nodes;
        this.pos_start = pos_start.copy(); this.pos_end = pos_end.copy();
        jptype = Constants.JPType.List;
    }

}
