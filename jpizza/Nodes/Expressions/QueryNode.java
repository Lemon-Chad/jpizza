package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Cases.Case;
import lemon.jpizza.Cases.ElseCase;
import lemon.jpizza.Nodes.Node;

import java.util.List;

public class QueryNode extends Node {
    public List<Case> cases;
    public ElseCase else_case;

    public QueryNode(List<Case> cases, ElseCase else_case) {
        this.else_case = else_case;
        this.cases = cases;
        pos_start = cases.get(0).condition.pos_start; pos_end = (
                else_case != null ? else_case.statements : cases.get(cases.size() - 1).condition
        ).pos_end;
    }

}
