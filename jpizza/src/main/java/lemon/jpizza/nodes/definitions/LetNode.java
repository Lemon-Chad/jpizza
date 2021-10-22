package lemon.jpizza.nodes.definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.Collections;

public class LetNode extends Node {
    public final Token var_name_tok;
    public final Node value_node;

    public LetNode(Token var_name_tok, Node value_node) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;

        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = Constants.JPType.VarAssign;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String varName = (String) var_name_tok.value;
        Obj value = res.register(inter.visit(value_node, context));
        if (res.shouldReturn()) return res;

        RTError.ErrorDetails error = context.symbolTable.define(varName, value, false,
                Collections.singletonList(value.type().toString()), null, null);
        if (error != null) return res.failure(error.build(
                pos_start, pos_end,
                context
        ));

        return res.success(value);
    }

}
