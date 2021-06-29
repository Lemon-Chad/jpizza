package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Position;

import java.util.Map;

public class DictNode extends Node {
    public Map<Node, Node> dict;

    public DictNode(Map<Node, Node> dict, Position pos_start, Position pos_end) {
        this.dict = dict;
        this.pos_start = pos_start;
        this.pos_end = pos_end;
    }

    public Object get(Node key) {
        return dict.get(key);
    }
}
