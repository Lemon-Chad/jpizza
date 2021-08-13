package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Primitives.Num;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens;

public class NumberNode extends ValueNode {
    public double val;
    public boolean flt;

    public NumberNode(Token tok) {
        super(tok);
        val = (double) tok.value;
        flt = tok.type == Tokens.TT.FLOAT;
        jptype = Constants.JPType.Number;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().success(new Num(val, flt, true).set_context(context)
                .set_pos(pos_start, pos_end));
    }

}
