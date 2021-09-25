package lemon.jpizza.cases;

import lemon.jpizza.nodes.Node;

import java.io.Serializable;

public class ElseCase implements Serializable {
    public final boolean x;
    public final Node statements;

    public ElseCase(Node statements, boolean x) {
        this.statements = statements;
        this.x = x;
    }

}
