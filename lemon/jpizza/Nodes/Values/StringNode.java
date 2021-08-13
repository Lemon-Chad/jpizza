package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class StringNode extends ValueNode {
    public String val;
    public StringNode(Token tok) {
        super(tok);
        val = (String) tok.value;
        jptype = Constants.JPType.String;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().success(new Str(val).set_context(context)
                .set_pos(pos_start, pos_end));
    }

}
