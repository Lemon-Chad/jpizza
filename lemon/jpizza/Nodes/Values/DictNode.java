package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Position;

import java.util.Map;

public class DictNode extends Node {
    public Map<Node, Node> dict;

    public DictNode(Map<Node, Node> dict, Position pos_start, Position pos_end) {
        this.dict = dict;
        this.pos_start = pos_start.copy();
        this.pos_end = pos_end.copy();
        jptype = Constants.JPType.Dict;
    }

    public Object get(Node key) {
        return dict.get(key);
    }
}
