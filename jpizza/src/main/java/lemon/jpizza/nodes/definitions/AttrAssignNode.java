package lemon.jpizza.nodes.definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;
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

        RTError.ErrorDetails v = context.symbolTable.setattr(varName, value);
        if (v != null) return res.failure(v.build(
                pos_start, pos_end,
                context
        ));

        return res.success(value);
    }

}
