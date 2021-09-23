package lemon.jpizza.nodes.variables;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

public class AttrAccessNode extends Node {
    public Token var_name_tok;
    public boolean fluctuating = true;

    public AttrAccessNode(Token var_name_tok) {
        this.var_name_tok = var_name_tok;
        pos_start = var_name_tok.pos_start.copy(); pos_end = var_name_tok.pos_end.copy();
        jptype = Constants.JPType.AttrAccess;
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

}
