package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Position;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReturnNode extends Node {
    public final Node nodeToReturn;

    public ReturnNode(Node nodeToReturn, @NotNull Position pos_start, @NotNull Position pos_end) {
        this.nodeToReturn = nodeToReturn;
        this.pos_start = pos_start.copy(); this.pos_end = pos_end.copy();
        jptype = JPType.Return;
    }

    @Override
    public Node optimize() {
        return new ReturnNode(nodeToReturn != null ? nodeToReturn.optimize() : null, pos_start, pos_end);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(nodeToReturn));
    }

    @Override
    public String visualize() {
        return "return";
    }
}
