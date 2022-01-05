package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

import java.util.ArrayList;
import java.util.List;

public class ScopeNode extends Node {
    public final Node statements;
    public final String scopeName;

    public ScopeNode(String name, Node states) {
        statements = states;
        scopeName = name;

        pos_start = states.pos_start;
        pos_end = states.pos_end;
        jptype = JPType.Scope;
    }

    public RTResult visit(Interpreter inter, Context context) {
        Context scopeContext = new Context(
                scopeName == null ? context.displayName : scopeName,
                context,
                statements.pos_start
        );
        scopeContext.symbolTable = new SymbolTable(context.symbolTable);

        RTResult res = new RTResult();
        res.register(inter.visit(statements, scopeContext));
        if (res.shouldReturn() && res.funcReturn == null) {
            return res;
        }
        return res.success(res.funcReturn != null ? res.funcReturn : new Null());
    }

    @Override
    public Node optimize() {
        return new ScopeNode(scopeName, statements.optimize());
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>(List.of(statements));
    }

    @Override
    public String visualize() {
        return "scope" + (scopeName == null ? "" : "[" + scopeName + "]");
    }
}
