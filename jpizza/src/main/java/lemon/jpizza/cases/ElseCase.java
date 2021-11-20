package lemon.jpizza.cases;

import lemon.jpizza.nodes.Node;

import java.io.Serializable;

public class ElseCase implements Serializable {
    public final boolean returnValue;
    public final Node statements;

    public ElseCase(Node statements, boolean returnValue) {
        this.statements = statements;
        this.returnValue = returnValue;
    }

}
