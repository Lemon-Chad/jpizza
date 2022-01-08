package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.cases.Case;
import lemon.jpizza.cases.ElseCase;
import lemon.jpizza.nodes.Node;

import java.util.ArrayList;
import java.util.List;

public class SwitchNode extends Node {
    public final boolean match;
    public final List<Case> cases;
    public final ElseCase elseCase;
    public final Node reference;

    public SwitchNode(Node ref, List<Case> css, ElseCase else_case, boolean isMatch) {
        elseCase = else_case;
        match = isMatch;
        cases = css;
        reference = ref;
        pos_start = (cases.size() > 0 ? cases.get(0).condition : ref).pos_start.copy();
        pos_end = (else_case != null ? else_case.statements :
                (cases.size() > 0 ? cases.get(cases.size() - 1).condition : ref)
        ).pos_end.copy();
        jptype = JPType.Switch;
    }

    @Override
    public Node optimize() {
        Node ref = reference.optimize();
        List<Case> newCases = new ArrayList<>();
        for (Case cs : cases) {
            newCases.add(new Case(cs.condition.optimize(), cs.statements.optimize(), cs.returnValue));
        }
        ElseCase newElseCase = elseCase != null ? new ElseCase(elseCase.statements.optimize(), elseCase.returnValue) : null;
        return new SwitchNode(ref, newCases, newElseCase, match);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(reference);
        for (Case cs : cases) {
            children.add(cs.condition);
            children.add(cs.statements);
        }
        if (elseCase != null) {
            children.add(elseCase.statements);
        }
        return children;
    }

    @Override
    public String visualize() {
        return match ? "match" : "switch";
    }
}
