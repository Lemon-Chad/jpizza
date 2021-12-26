package lemon.jpizza.nodes.values;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.List;

public class NullNode extends ValueNode {
    public NullNode(Token tok) {
        super(tok);
        jptype = JPType.Null;
    }

    public RTResult visit(Interpreter inter, Context context) {
        return new RTResult().success(new Null().set_context(context).set_pos(pos_start, pos_end));
    }

    @Override
    public boolean equals(Node other) {
        return other instanceof NullNode;
    }

    @Override
    public String visualize() {
        return "null";
    }
}
