package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.cases.Case;
import lemon.jpizza.cases.ElseCase;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class QueryNode extends Node {
    public final List<Case> cases;
    public final ElseCase else_case;

    public QueryNode(List<Case> cases, ElseCase else_case) {
        this.else_case = else_case;
        this.cases = cases;
        pos_start = cases.get(0).condition.pos_start.copy(); pos_end = (
                else_case != null ? else_case.statements : cases.get(cases.size() - 1).condition
        ).pos_end.copy();
        jptype = JPType.Query;
    }

    @Override
    public Node optimize() {
        List<Case> optimizedCases = new ArrayList<>();
        for (Case c : cases) {
            optimizedCases.add(new Case(c.condition.optimize(), c.statements.optimize(), c.returnValue));
        }
        return new QueryNode(optimizedCases, else_case != null ? new ElseCase(else_case.statements.optimize(), else_case.returnValue) : null);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        for (Case c : cases) {
            children.add(c.condition);
            children.add(c.statements);
        }
        if (else_case != null) {
            children.add(else_case.statements);
        }
        return children;
    }

    @Override
    public String visualize() {
        return "query";
    }
}
