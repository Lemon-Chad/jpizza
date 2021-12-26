package lemon.jpizza.nodes.definitions;

import lemon.jpizza.JPType;
import lemon.jpizza.Token;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class DecoratorNode extends Node {
    public Node decorator;
    public Node decorated;
    public Token name;

    public DecoratorNode(Node decorator, Node fn, Token name) {
        this.decorator = decorator;
        this.decorated = fn;
        this.name = name;

        pos_start = decorator.pos_start;
        pos_end = decorated.pos_end;
        jptype = JPType.Decorator;
    }

    @Override
    public Node optimize() {
        return new DecoratorNode(decorator.optimize(), decorated.optimize(), name);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(decorator);
        children.add(decorated);
        return children;
    }

    @Override
    public String visualize() {
        return "/decorator de " + name.value + "/";
    }
}
