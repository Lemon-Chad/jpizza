package lemon.jpizza.nodes.values;

import lemon.jpizza.*;
import lemon.jpizza.nodes.Node;

public class BooleanNode extends ValueNode {
    public final boolean val;

    public BooleanNode(Token tok) {
        super(tok);
        val = (boolean) tok.value;
        jptype = JPType.Boolean;
    }

    public BooleanNode(boolean val, Position start, Position end) {
        super(new Token(TokenType.Boolean, start, end));
        this.val = val;
        jptype = JPType.Boolean;
    }

    @Override
    public double asNumber() {
        return val ? 1 : 0;
    }

    @Override
    public boolean asBoolean() {
        return val;
    }

    @Override
    public String asString() {
        return String.valueOf(val);
    }

    @Override
    public boolean equals(Node other) {
        if (other instanceof BooleanNode) {
            return val == ((BooleanNode) other).val;
        }
        return false;
    }

    @Override
    public String visualize() {
        return String.valueOf(val);
    }
}
