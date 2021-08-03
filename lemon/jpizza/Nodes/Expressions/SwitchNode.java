package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Cases.Case;
import lemon.jpizza.Cases.ElseCase;
import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;

import java.util.List;

public class SwitchNode extends Node {
    public boolean autoreturn;
    public List<Case> cases;
    public ElseCase elseCase;
    public Node reference;

    public SwitchNode(Node ref, List<Case> css, ElseCase else_case, boolean autoret) {
        elseCase = else_case;
        autoreturn = autoret;
        cases = css;
        reference = ref;
        pos_start = cases.get(0).condition.pos_start.copy(); pos_end = (
                else_case != null ? else_case.statements : cases.get(cases.size() - 1).condition
        ).pos_end.copy();
        jptype = Constants.JPType.Switch;
    }

}
