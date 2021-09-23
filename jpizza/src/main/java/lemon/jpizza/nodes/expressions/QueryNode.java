package lemon.jpizza.nodes.expressions;

import lemon.jpizza.cases.Case;
import lemon.jpizza.cases.ElseCase;
import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Bool;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

import java.util.List;

public class QueryNode extends Node {
    public List<Case> cases;
    public ElseCase else_case;
    public boolean fluctuating = true;

    public QueryNode(List<Case> cases, ElseCase else_case) {
        this.else_case = else_case;
        this.cases = cases;
        pos_start = cases.get(0).condition.pos_start.copy(); pos_end = (
                else_case != null ? else_case.statements : cases.get(cases.size() - 1).condition
        ).pos_end.copy();
        jptype = Constants.JPType.Query;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj conditionValue, exprValue;
        int size = cases.size();
        for (int i = 0; i < size; i++) {
            Case c = cases.get(i);
            conditionValue = res.register(c.condition.visit(inter, context));
            if (res.shouldReturn()) return res;
            Obj bx = conditionValue.bool();
            if (bx.jptype != Constants.JPType.Boolean) return res.failure(RTError.Type(
                    pos_start, pos_end,
                    "Conditional must be a boolean",
                    context
            ));
            Bool b = (Bool) bx;
            if (b.trueValue()) {
                exprValue = res.register(c.statements.visit(inter, context));
                if (res.shouldReturn()) return res;
                return res.success(c.x ? new Null() : exprValue);
            }
        }

        if (else_case != null) {
            Obj elseValue = res.register(else_case.statements.visit(inter, context));
            if (res.shouldReturn()) return res;
            return res.success(else_case.x ? new Null() : elseValue);
        }

        return res.success(new Null());
    }

}
