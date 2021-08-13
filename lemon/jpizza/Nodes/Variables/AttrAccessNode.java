package lemon.jpizza.Nodes.Variables;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class AttrAccessNode extends Node {
    public Token var_name_tok;

    public AttrAccessNode(Token var_name_tok) {
        this.var_name_tok = var_name_tok;
        pos_start = var_name_tok.pos_start.copy(); pos_end = var_name_tok.pos_end.copy();
        jptype = Constants.JPType.AttrAccess;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String varName = (String) var_name_tok.value;
        Obj value = (Obj) context.symbolTable.getattr(varName);

        if (value == null) return res.failure(new RTError(
                pos_start, pos_end,
                "'" + varName + "' is not defined",
                context
        ));

        value = value.copy().set_pos(pos_start, pos_end).set_context(context);
        return res.success(value);
    }

}
