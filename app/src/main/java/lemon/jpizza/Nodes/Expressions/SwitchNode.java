package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Cases.Case;
import lemon.jpizza.Cases.ElseCase;
import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Bool;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Operations;
import lemon.jpizza.Pair;
import lemon.jpizza.Results.RTResult;

import java.util.List;

public class SwitchNode extends Node {
    public boolean autoreturn;
    public List<Case> cases;
    public ElseCase elseCase;
    public Node reference;
    public boolean fluctuating = true;

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

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj ret = new Null();

        Obj ref = res.register(reference.visit(inter, context));
        if (res.error != null) return res;

        int entry = -1;
        int size = cases.size();

        Obj compare;
        Case cs;
        for (int i = 0; i < size; i++) {
            cs = cases.get(i);
            compare = res.register(cs.condition.visit(inter, context));
            if (res.error != null) return res;

            if (((Bool)((Pair<Obj, RTError>) ref.getattr(Operations.OP.EQ, compare)).a).trueValue()) {
                entry = i;
                break;
            }
        }

        if (autoreturn && entry > -1) {
            ret = res.register(cases.get(entry).statements.visit(inter, context));
            if (res.error != null) return res;
        } else if (entry > -1) {
            for (; entry < size; entry++) {
                res.register(cases.get(entry).statements.visit(inter, context));
                if (res.error != null) return res;
                if (res.breakLoop) break;
            }
        }

        if (entry == -1 && elseCase != null) {
            ret = res.register(elseCase.statements.visit(inter, context));
            if (res.error != null) return res;
        }

        return res.success(autoreturn ? ret : new Null());
    }

}
