package lemon.jpizza.nodes.values;

import lemon.jpizza.*;
import lemon.jpizza.nodes.Node;
import org.apache.commons.text.StringEscapeUtils;

public class StringNode extends ValueNode {
    public final String val;
    public StringNode(Token tok) {
        super(tok);
        val = ((Pair<String, Boolean>) tok.value).a;
        jptype = JPType.String;
    }

    public StringNode(String val, Position start, Position end) {
        super(new Token(TokenType.String, new Pair<>(val, false), start, end));
        this.val = val;
        jptype = JPType.String;
    }

    @Override
    public boolean asBoolean() {
        return !val.isEmpty();
    }

    @Override
    public String asString() {
        return val;
    }

    @Override
    public double asNumber() {
        return val.length();
    }

    @Override
    public boolean equals(Node other) {
        if (other instanceof StringNode) {
            return val.equals(((StringNode) other).val);
        }
        return false;
    }

    @Override
    public String visualize() {
        return "\"" + StringEscapeUtils.escapeJava(val) + "\"";
    }
}
