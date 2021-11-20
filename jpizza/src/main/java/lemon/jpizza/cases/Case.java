package lemon.jpizza.cases;

import lemon.jpizza.nodes.Node;

import java.io.Serializable;

public class Case implements Serializable {
    public final boolean returnValue;
    public final Node condition;
    public final Node statements;

    public Case(Node condition, Node statements, boolean returnValue) {
        this.condition = condition;
        this.statements = statements;
        this.returnValue = returnValue;
    }

}
