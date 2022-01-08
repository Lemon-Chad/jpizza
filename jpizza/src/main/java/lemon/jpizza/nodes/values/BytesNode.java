package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BytesNode extends Node {
    public final Node toBytes;

    public BytesNode(Node toBytes) {
        this.toBytes = toBytes;
        this.pos_start = toBytes.pos_start.copy(); this.pos_end = toBytes.pos_end.copy();
        jptype = JPType.Bytes;
    }

    @Override
    public Node optimize() {
        return new BytesNode(toBytes.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(toBytes));
    }

    @Override
    public String visualize() {
        return "@";
    }
}
