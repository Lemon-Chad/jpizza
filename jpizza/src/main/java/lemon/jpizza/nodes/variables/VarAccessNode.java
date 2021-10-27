package lemon.jpizza.nodes.variables;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.executables.ClassInstance;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

public class VarAccessNode extends Node {
    public final Token var_name_tok;

    public VarAccessNode(Token var_name_tok) {
        this.var_name_tok = var_name_tok;
        pos_start = var_name_tok.pos_start.copy(); pos_end = var_name_tok.pos_end.copy();
        jptype = Constants.JPType.VarAccess;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String varName = var_name_tok.value.toString();
        if (context.symbolTable.isDyn(varName)) {
            Node value = context.symbolTable.getDyn(varName);
            Obj ret = res.register(inter.visit(value, context));
            if (res.shouldReturn()) return res;
            return res.success(ret);
        }

        Object value = context.symbolTable.get(varName);

        if (value == null) {
            if (varName.equals("this"))
                return res.success(new ClassInstance(context).set_pos(pos_start, pos_end));
            else
                return res.failure(RTError.Scope(
                        pos_start, pos_end,
                        "'" + varName + "' is not defined",
                        context
                ));
        }
        Obj val = ((Obj) value).copy().set_pos(pos_start, pos_end);
        return res.success(val);
    }

}
