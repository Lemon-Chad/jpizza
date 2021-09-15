package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class DynAssignNode extends Node {
    public Token var_name_tok;
    public Node value_node;

    public DynAssignNode(Token var_name_tok, Node value_node) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;

        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = Constants.JPType.DynAssign;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String varName = (String) var_name_tok.value;

        context.symbolTable.setDyn(varName, value_node);

        return res.success(new Null());
    }

}
