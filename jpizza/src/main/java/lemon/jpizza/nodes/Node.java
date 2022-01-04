package lemon.jpizza.nodes;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Node {
    public Position pos_start;
    public Position pos_end;
    public JPType jptype;
    public boolean constant = false;

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().failure(RTError.Internal(
                pos_start.copy(), pos_end.copy(),
                "No visit method for " + getClass().getSimpleName(),
                context
        ));
    }

    public abstract Node optimize();

    public double asNumber() {
        return 0;
    }

    public boolean asBoolean() {
        return false;
    }

    public String asString() {
        return "";
    }

    public Map<Node, Node> asMap() {
        return new ConcurrentHashMap<>();
    }

    public List<Node> asList() {
        return Collections.singletonList(this);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Node && equals((Node) other);
    }

    public boolean equals(Node other) {
        return false;
    }

    public abstract List<Node> getChildren();

    public abstract String visualize();

    protected Node setStatic(boolean b) {
        constant = b;
        return this;
    }
}
