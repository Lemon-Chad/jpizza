package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.primitives.Bool;
import lemon.jpizza.results.RTResult;
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
