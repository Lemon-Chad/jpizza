package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScopeNode extends Node {
    public final Node statements;
    public final String scopeName;

    public ScopeNode(String name, Node states) {
        statements = states;
        scopeName = name;

        pos_start = states.pos_start;
        pos_end = states.pos_end;
        jptype = JPType.Scope;
    }

    @Override
    public Node optimize() {
        return new ScopeNode(scopeName, statements.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(Collections.singletonList(statements));
    }

    @Override
    public String visualize() {
        return "scope" + (scopeName == null ? "" : "[" + scopeName + "]");
    }
}
