package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Position;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ListNode extends Node {
    public final List<Node> elements;

    public ListNode(List<Node> element_nodes, @NotNull Position pos_start, @NotNull Position pos_end) {
        elements = element_nodes;

        constant = true;
        for (Node element : elements) {
            if (!element.constant) {
                constant = false;
                break;
            }
        }

        this.pos_start = pos_start.copy(); this.pos_end = pos_end.copy();
        jptype = JPType.List;
    }

    @Override
    public Node optimize() {
        List<Node> optimizedElements = new ArrayList<>();
        for (Node element : elements) {
            optimizedElements.add(element.optimize());
        }
        return new ListNode(optimizedElements, pos_start, pos_end);
    }

    @Override
    public double asNumber() {
        return elements.size();
    }

    @Override
    public List<Node> asList() {
        return elements;
    }

    @Override
    public String asString() {
        StringBuilder result = new StringBuilder("[");
        elements.forEach(k -> {
            if (k.jptype == JPType.String) {
                result.append('"').append(k.asString()).append('"');
            }
            else {
                result.append(k.asString());
            }
            result.append(", ");
        });
        if (result.length() > 1) {
            result.setLength(result.length() - 2);
        } result.append("]");
        return result.toString();
    }

    @Override
    public boolean asBoolean() {
        return !elements.isEmpty();
    }

    @Override
    public boolean equals(Node other) {
        if (other.jptype != JPType.List)
            return false;
        ListNode otherList = (ListNode) other;
        if (otherList.elements.size() != elements.size())
            return false;
        for (int i = 0; i < elements.size(); i++) {
            if (!elements.get(i).equals(otherList.elements.get(i)))
                return false;
        }
        return true;
    }

    @Override
    public List<Node> getChildren() {
        return elements;
    }

    @Override
    public String visualize() {
        return "[ List ]";
    }
}
