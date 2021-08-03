package lemon.jpizza;

import lemon.jpizza.Nodes.Node;

import java.io.Serializable;

public class PizzaBox implements Serializable {
    Node value;
    public PizzaBox(Node value) {
        this.value = value;
    }
}
