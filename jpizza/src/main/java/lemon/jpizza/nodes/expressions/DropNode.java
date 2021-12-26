package lemon.jpizza.nodes.expressions;

import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.List;

public class DropNode extends Node {
    public final Token varTok;

    public DropNode(Token varTok) {
        pos_start = varTok.pos_start; pos_end = varTok.pos_end;
        this.varTok = varTok;
    }

    public RTResult visit(Interpreter inter, Context context) {
        context.symbolTable.remove(varTok.value.toString());
        return new RTResult().success(new Null());
    }

    @Override
    public Node optimize() {
        return this;
    }

    @Override
    public List<Node> getChildren() {
        return List.of();
    }

    @Override
    public String visualize() {
        return "free " + varTok.value;
    }
}
