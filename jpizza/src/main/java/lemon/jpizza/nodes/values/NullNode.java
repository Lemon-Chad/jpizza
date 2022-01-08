package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Token;

public class NullNode extends ValueNode {
    public NullNode(Token tok) {
        super(tok);
        jptype = JPType.Null;
    }

    @Override
    public boolean equals(Node other) {
        return other instanceof NullNode;
    }

    @Override
    public String visualize() {
        return "null";
    }
}
