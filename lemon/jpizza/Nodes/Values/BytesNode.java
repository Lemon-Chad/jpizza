package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Position;

import java.util.List;

public class BytesNode extends Node {
    public Node toBytes;

    public BytesNode(Node toBytes) {
        this.toBytes = toBytes;
        this.pos_start = toBytes.pos_start.copy(); this.pos_end = toBytes.pos_end.copy();
        jptype = Constants.JPType.Bytes;
    }

}
