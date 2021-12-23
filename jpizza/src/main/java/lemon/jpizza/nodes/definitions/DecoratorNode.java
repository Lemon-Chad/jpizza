package lemon.jpizza.nodes.definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.Token;
import lemon.jpizza.nodes.Node;

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
        jptype = Constants.JPType.Decorator;
    }
}
