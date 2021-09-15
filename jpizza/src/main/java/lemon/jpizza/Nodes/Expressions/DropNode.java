package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class DropNode extends Node {
    Token varTok;

    public DropNode(Token varTok) {
        pos_start = varTok.pos_start; pos_end = varTok.pos_end;
        this.varTok = varTok;
    }

    public RTResult visit(Interpreter inter, Context context) {
        context.symbolTable.remove(varTok.value.toString());
        return new RTResult().success(new Null());
    }
}
