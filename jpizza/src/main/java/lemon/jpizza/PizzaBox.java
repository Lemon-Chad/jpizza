package lemon.jpizza;

import lemon.jpizza.Nodes.Node;

import java.io.Serializable;
import java.util.List;

public class PizzaBox implements Serializable {
    List<Node> value;
    public PizzaBox(List<Node> value) {
        this.value = value;
    }
}