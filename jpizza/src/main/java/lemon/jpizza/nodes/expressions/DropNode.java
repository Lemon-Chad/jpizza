package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.List;

public class DropNode extends Node {
    public final Token varTok;

    public DropNode(Token varTok) {
        pos_start = varTok.pos_start; pos_end = varTok.pos_end;
        this.varTok = varTok;
        jptype = JPType.Drop;
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public String visualize() {
        return "free " + varTok.value;
    }
}
