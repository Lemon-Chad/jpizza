package lemon.jpizza.Cases;

import lemon.jpizza.Nodes.Node;

import java.io.Serializable;

public class Case implements Serializable {
    public boolean x;
    public Node condition;
    public Node statements;

    public Case(Node condition, Node statements, boolean x) {
        this.condition = condition;
        this.statements = statements;
        this.x = x;
    }

}
