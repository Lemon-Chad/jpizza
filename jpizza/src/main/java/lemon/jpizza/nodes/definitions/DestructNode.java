package lemon.jpizza.nodes.definitions;

import lemon.jpizza.JPType;
import lemon.jpizza.Token;
import lemon.jpizza.nodes.Node;

import java.util.*;

public class DestructNode extends Node {
    public final Node target;
    public List<Token> subs = new ArrayList<>();
    public boolean glob = false;

    public DestructNode(Node tar) {
        target = tar;
        glob = true;

        pos_start = tar.pos_start;
        pos_end = tar.pos_end;
        jptype = JPType.Destruct;
    }

    public DestructNode(Node tar, List<Token> tars) {
        target = tar;
        subs = tars;

        pos_start = tars.get(0).pos_start;
        pos_end = tar.pos_end;
        jptype = JPType.Destruct;
    }

    @Override
    public Node optimize() {
        Node target = this.target.optimize();
        if (glob) return new DestructNode(target);
        return new DestructNode(target, subs);
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(target));
    }

    @Override
    public String visualize() {
        if (glob) {
            return "destruct *";
        }
        else {
            StringBuilder sb = new StringBuilder();
            sb.append("destruct ");
            for (Token struct : subs)
                sb.append(struct.value.toString()).append(" ");
            return sb.substring(0, sb.length() - 1);
        }
    }
}
