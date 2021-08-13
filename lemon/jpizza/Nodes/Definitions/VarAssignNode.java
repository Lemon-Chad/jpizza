package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class VarAssignNode extends Node {
    public Token var_name_tok;
    public Node value_node;
    public boolean locked;
    public boolean defining;
    public String type;

    public VarAssignNode setType(String type) {
        this.type = type;
        return this;
    }

    public VarAssignNode setDefining(boolean defining) {
        this.defining = defining;
        return this;
    }

    public VarAssignNode(Token var_name_tok, Node value_node) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;

        locked = false;
        defining = true;
        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = Constants.JPType.VarAssign;
    }

    public VarAssignNode(Token var_name_tok, Node value_node, boolean locked) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;
        this.locked = locked;

        defining = true;
        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = Constants.JPType.VarAssign;
    }

    @SuppressWarnings("unused")
    public VarAssignNode(Token var_name_tok, Node value_node, boolean defining, int _x) {
        this.var_name_tok = var_name_tok;
        this.value_node = value_node;
        locked = false;

        this.defining = defining;
        pos_start = var_name_tok.pos_start; pos_end = var_name_tok.pos_end;
        jptype = Constants.JPType.VarAssign;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String varName = (String) var_name_tok.value;
        Obj value = res.register(value_node.visit(inter, context));
        if (res.shouldReturn()) return res;

        String error;
        if (defining)
            error = context.symbolTable.define(varName, value, locked, type);
        else
            error = context.symbolTable.set(varName, value, locked);
        if (error != null) return res.failure(new RTError(
                pos_start, pos_end,
                error,
                context
        ));

        return res.success(value);
    }

}
