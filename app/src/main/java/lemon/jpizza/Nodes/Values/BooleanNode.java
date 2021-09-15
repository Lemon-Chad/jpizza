package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Primitives.Bool;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class BooleanNode extends ValueNode {
    public boolean val;
    public BooleanNode(Token tok) {
        super(tok);
        val = (boolean) tok.value;
        jptype = Constants.JPType.Boolean;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().success(new Bool(val).set_context(context)
                .set_pos(pos_start, pos_end));
    }

}
