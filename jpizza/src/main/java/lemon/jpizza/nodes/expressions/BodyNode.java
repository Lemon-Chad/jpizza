package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.Position;
import lemon.jpizza.nodes.Node;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BodyNode extends Node {
    final public List<Node> statements;

    public BodyNode(List<Node> statements) {
        this.statements = statements;
        this.pos_start = statements.get(0).pos_start;
        this.pos_end = statements.get(statements.size() - 1).pos_end;
        this.jptype = JPType.Body;
    }

    public BodyNode(List<Node> statements, @NotNull Position start, @NotNull Position end) {
        this.statements = statements;
        this.pos_start = start;
        this.pos_end = end;
        this.jptype = JPType.Body;
    }

    @Override
    public Node optimize() {
        for (int i = 0; i < statements.size(); i++) {
            statements.set(i, statements.get(i).optimize());
        }
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return statements;
    }

    @Override
    public String visualize() {
        return "< body >";
    }
}
