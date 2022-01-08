package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.Token;
import lemon.jpizza.nodes.Node;

import java.util.*;

public class PatternNode extends Node {
    public final Node accessNode;
    public final HashMap<Token, Node> patterns;

    public PatternNode(Node accessNode, HashMap<Token, Node> patterns) {
        this.accessNode = accessNode;
        this.patterns = patterns;

        pos_start = accessNode.pos_start; pos_end = accessNode.pos_end;
        jptype = JPType.Pattern;
    }

    @Override
    public Node optimize() {
        accessNode.optimize();
        for (Node node : patterns.values())
            node.optimize();
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(accessNode));
    }

    @Override
    public String visualize() {
        return "Pattern";
    }
}
