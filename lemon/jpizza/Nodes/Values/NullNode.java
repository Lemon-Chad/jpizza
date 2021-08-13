package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class NullNode extends ValueNode {
    public NullNode(Token tok) {
        super(tok);
        jptype = Constants.JPType.Null;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().success(new Null().set_context(context).set_pos(pos_start, pos_end));
    }

}
