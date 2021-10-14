package lemon.jpizza.nodes.expressions;

import lemon.jpizza.cases.Case;
import lemon.jpizza.cases.ElseCase;
import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.values.PatternNode;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

import java.util.List;

public class SwitchNode extends Node {
    public final boolean autoreturn;
    public final List<Case> cases;
    public final ElseCase elseCase;
    public final Node reference;
    public boolean fluctuating = true;

    public SwitchNode(Node ref, List<Case> css, ElseCase else_case, boolean autoret) {
        elseCase = else_case;
        autoreturn = autoret;
        cases = css;
        reference = ref;
        pos_start = (cases.size() > 0 ? cases.get(0).condition : ref).pos_start.copy();
        pos_end = (else_case != null ? else_case.statements :
                (cases.size() > 0 ? cases.get(cases.size() - 1).condition : ref)
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
            if (cs.condition.jptype == Constants.JPType.Pattern) {
                // Pattern matching
                PatternNode pattern = (PatternNode) cs.condition;
                Obj matches = res.register(pattern.compare(inter, context, ref));
                if (res.error != null) return res;
                if (matches.boolval) {
                    entry = i;
                    break;
                }
                continue;
            }
            compare = res.register(cs.condition.visit(inter, context));
            if (res.error != null) return res;

            if (ref.equals(compare)) {
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
