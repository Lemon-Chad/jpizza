package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Position;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

import java.util.List;

public class BodyNode extends Node {
    final public List<Node> statements;

    public BodyNode(List<Node> statements) {
        this.statements = statements;
        this.pos_start = statements.get(0).pos_start;
        this.pos_end = statements.get(statements.size() - 1).pos_end;
    }

    public BodyNode(List<Node> statements, Position start, Position end) {
        this.statements = statements;
        this.pos_start = start;
        this.pos_end = end;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        for (Node statement : statements) {
           res.register(statement.visit(inter, context));
            if (res.shouldReturn()) {
                return res;
            }
        }
        return res.success(new Null());
    }
}
