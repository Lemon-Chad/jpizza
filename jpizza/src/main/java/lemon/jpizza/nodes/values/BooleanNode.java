package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.Position;
import lemon.jpizza.Tokens;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.primitives.Bool;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

public class BooleanNode extends ValueNode {
    public final boolean val;

    public BooleanNode(Token tok) {
        super(tok);
        val = (boolean) tok.value;
        jptype = JPType.Boolean;
    }

    public BooleanNode(boolean val, Position start, Position end) {
        super(new Token(Tokens.TT.BOOL, start, end));
        this.val = val;
        jptype = JPType.Boolean;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().success(new Bool(val).set_context(context)
                .set_pos(pos_start, pos_end));
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
