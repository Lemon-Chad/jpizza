package lemon.jpizza;

import lemon.jpizza.nodes.Node;

import java.io.Serializable;
import java.util.List;

public class PizzaBox implements Serializable {
    final List<Node> value;
    public PizzaBox(List<Node> value) {
        this.value = value;
    }
}
