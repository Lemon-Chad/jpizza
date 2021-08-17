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
import lemon.jpizza.Results.RTResult;

import java.util.List;

public class QueryNode extends Node {
    public List<Case> cases;
    public ElseCase else_case;

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
            if (bx.jptype != Constants.JPType.Boolean) return res.failure(new RTError(
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
