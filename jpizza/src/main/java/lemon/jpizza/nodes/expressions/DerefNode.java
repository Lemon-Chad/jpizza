package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DerefNode extends Node {
    public final Node ref;
    public DerefNode(Node ref) {
        this.ref = ref;

        pos_start = ref.pos_start; pos_end = ref.pos_end;
        jptype = JPType.Deref;
    }

    @Override
    public Node optimize() {
        return new DerefNode(ref.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(ref));
    }

    @Override
    public String visualize() {
        return "*Ref";
    }
}
