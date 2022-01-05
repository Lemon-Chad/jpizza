package lemon.jpizza.nodes.variables;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.List;

public class AttrAccessNode extends Node {
    public final Token var_name_tok;

    public AttrAccessNode(Token var_name_tok) {
        this.var_name_tok = var_name_tok;
        pos_start = var_name_tok.pos_start.copy(); pos_end = var_name_tok.pos_end.copy();
        jptype = JPType.AttrAccess;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String varName = (String) var_name_tok.value;
        Obj value = (Obj) context.symbolTable.getattr(varName);

        if (value == null) return res.failure(RTError.Scope(
                pos_start, pos_end,
                "'" + varName + "' is not defined",
                context
        ));

        value = value.copy().set_pos(pos_start, pos_end).set_context(context);
        return res.success(value);
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public String visualize() {
        return var_name_tok.value.toString();
    }
}
