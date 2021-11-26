package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.primitives.Num;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens;

public class NumberNode extends ValueNode {
    public final double val;
    public final boolean hex;

    public NumberNode(Token tok) {
        super(tok);
        val = (double) tok.value;
        hex = false;
        jptype = Constants.JPType.Number;
    }

    public NumberNode(Token tok, boolean hex) {
        super(tok);
        val = (double) tok.value;
        this.hex = hex;
        jptype = Constants.JPType.Number;
    }

    public NumberNode(int v, Position pos_start, Position pos_end) {
        super(new Token(Tokens.TT.IDENTIFIER, "null", pos_start, pos_end));
        val = v;
        hex = true;
        jptype = Constants.JPType.Number;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().success(new Num(val, hex).set_context(context)
                .set_pos(pos_start, pos_end));
    }

}
