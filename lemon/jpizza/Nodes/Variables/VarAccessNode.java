package lemon.jpizza.Nodes.Variables;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class VarAccessNode extends Node {
    public Token var_name_tok;

    public VarAccessNode(Token var_name_tok) {
        this.var_name_tok = var_name_tok;
        pos_start = var_name_tok.pos_start.copy(); pos_end = var_name_tok.pos_end.copy();
        jptype = Constants.JPType.VarAccess;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String varName = (String) var_name_tok.value;
        if (context.symbolTable.isDyn(varName)) {
            Node value = context.symbolTable.getDyn(varName);
            Obj ret = res.register(value.visit(inter, context));
            if (res.shouldReturn()) return res;
            return res.success(ret);
        }

        Object value = context.symbolTable.get(varName);

        if (value == null) return res.failure(new RTError(
                pos_start, pos_end,
                "'" + varName + "' is not defined",
                context
        ));
        else if (value instanceof String)
            return Interpreter.getThis(value, context, pos_start, pos_end);
        Obj val = ((Obj) value).set_pos(pos_start, pos_end).set_context(context).copy();
        return res.success(val);
    }

}
