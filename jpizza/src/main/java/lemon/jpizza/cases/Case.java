package lemon.jpizza.cases;

import lemon.jpizza.nodes.Node;

public class Case {
    public final boolean returnValue;
    public final Node condition;
    public final Node statements;

    public Case(Node condition, Node statements, boolean returnValue) {
        this.condition = condition;
        this.statements = statements;
        this.returnValue = returnValue;
    }

}
