package lemon.jpizza.generators;

import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class Optimizer {
    public static List<Node> optimize(List<Node> nodes) {
        List<Node> optimized = new ArrayList<>();
        for (Node node : nodes) {
            optimized.add(optimize(node));
        }
        return optimized;
    }

    public static Node optimize(Node node) {
        return node.optimize();
    }
}
