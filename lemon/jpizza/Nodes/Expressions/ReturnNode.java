package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Position;

public class ReturnNode extends Node {
    public Node nodeToReturn;

    public ReturnNode(Node nodeToReturn, Position pos_start, Position pos_end) {
        this.nodeToReturn = nodeToReturn;
        this.pos_start = pos_start.copy(); this.pos_end = pos_end.copy();
        jptype = Constants.JPType.Return;
    }

}
