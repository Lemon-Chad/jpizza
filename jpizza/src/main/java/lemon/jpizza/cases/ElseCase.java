package lemon.jpizza.cases;

import lemon.jpizza.nodes.Node;

public class ElseCase {
    public final boolean returnValue;
    public final Node statements;

    public ElseCase(Node statements, boolean returnValue) {
        this.statements = statements;
        this.returnValue = returnValue;
    }

}
