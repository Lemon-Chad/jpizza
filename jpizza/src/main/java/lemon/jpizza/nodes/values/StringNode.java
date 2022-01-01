package lemon.jpizza.nodes.values;

import lemon.jpizza.*;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.results.RTResult;
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

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().success(new Str(StringEscapeUtils.unescapeJava(val)).set_context(context)
                .set_pos(pos_start, pos_end));
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
