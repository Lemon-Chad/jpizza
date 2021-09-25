package lemon.jpizza.cases;

import lemon.jpizza.nodes.Node;

import java.io.Serializable;

public class Case implements Serializable {
    public final boolean x;
    public final Node condition;
    public final Node statements;

    public Case(Node condition, Node statements, boolean x) {
        this.condition = condition;
        this.statements = statements;
        this.x = x;
    }

}
