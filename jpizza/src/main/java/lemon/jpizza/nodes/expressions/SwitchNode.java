package lemon.jpizza.nodes.expressions;

import lemon.jpizza.JPType;
import lemon.jpizza.cases.Case;
import lemon.jpizza.cases.ElseCase;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.values.PatternNode;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

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

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Context ctx = new Context("<switch>", context, pos_start);
        ctx.symbolTable = new SymbolTable(context.symbolTable);

        Obj ret = new Null();

        Obj ref = res.register(inter.visit(reference, ctx));
        if (res.error != null) return res;

        int entry = -1;
        int size = cases.size();

        Obj compare;
        Case cs;
        for (int i = 0; i < size; i++) {
            cs = cases.get(i);
            if (cs.condition.jptype == JPType.Pattern) {
                // Pattern matching
                PatternNode pattern = (PatternNode) cs.condition;
                Obj matches = res.register(pattern.compare(inter, ctx, ref));
                if (res.error != null) return res;
                if (matches.boolval) {
                    entry = i;
                    break;
                }
                continue;
            }
            compare = res.register(inter.visit(cs.condition, ctx));
            if (res.error != null) return res;

            if (ref.equals(compare)) {
                entry = i;
                break;
            }
        }

        if (match && entry > -1) {
            ret = res.register(inter.visit(cases.get(entry).statements, ctx));
            if (res.error != null) return res;
        }
        else if (entry > -1) {
            for (; entry < size; entry++) {
                res.register(inter.visit(cases.get(entry).statements, ctx));
                if (res.error != null) return res;
                if (res.breakLoop) break;
            }
        }

        if (entry == -1 && elseCase != null) {
            ret = res.register(inter.visit(elseCase.statements, ctx));
            if (res.error != null) return res;
        }

        return res.success(match ? ret : new Null());
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
