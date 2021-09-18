package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class AttrAssignNode extends Node {
    public Token var_name_tok;
    public Node value_node;

    public AttrAssignNode(Token var_name_tok, Node value_node) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;

        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = Constants.JPType.AttrAssign;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String varName = (String) var_name_tok.value;
        Obj value = res.register(value_node.visit(inter, context));
        if (res.shouldReturn()) return res;

        String v = context.symbolTable.setattr(varName, value);
        if (v != null) return res.failure(new RTError(
                pos_start, pos_end,
                v,
                context
        ));

        return res.success(value);

    }

}
