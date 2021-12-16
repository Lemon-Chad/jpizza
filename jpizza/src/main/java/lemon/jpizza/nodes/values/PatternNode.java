package lemon.jpizza.nodes.values;

import lemon.jpizza.Constants;
import lemon.jpizza.Token;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.variables.VarAccessNode;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.executables.ClassInstance;
import lemon.jpizza.objects.executables.ClassPlate;
import lemon.jpizza.objects.primitives.Bool;
import lemon.jpizza.objects.executables.EnumJChild;
import lemon.jpizza.results.RTResult;

import java.util.HashMap;
import java.util.Map;

public class PatternNode extends Node {
    public final Node accessNode;
    public final HashMap<Token, Node> patterns;

    public PatternNode(Node accessNode, HashMap<Token, Node> patterns) {
        this.accessNode = accessNode;
        this.patterns = patterns;

        pos_start = accessNode.pos_start; pos_end = accessNode.pos_end;
        jptype = Constants.JPType.Pattern;
    }

    public RTResult compare(Interpreter inter, Context context, Obj contrast) {
        RTResult res = new RTResult();

        if (contrast.jptype != Constants.JPType.ClassInstance) return res.success(new Bool(false));
        ClassInstance other = (ClassInstance) contrast;

        Obj cls = res.register(inter.visit(accessNode, context));
        if (res.error != null) return res;

        SymbolTable table = other.ctx.symbolTable;

        HashMap<Token, String> substitutes = new HashMap<>();
        HashMap<Token, Obj>    checks      = new HashMap<>();
        for (Map.Entry<Token, Node> entry : patterns.entrySet()) {
            Obj val = res.register(inter.visit(entry.getValue(), context));
            if (res.error != null) {
                if (entry.getValue().jptype == Constants.JPType.VarAccess && res.error.error_name.equals("Scope")) {
                    substitutes.put(entry.getKey(), ((VarAccessNode) entry.getValue()).var_name_tok.value.toString());
                    res.error = null;
                    continue;
                }
                else {
                    return res;
                }
            }
            checks.put(entry.getKey(), val);
        }

        switch (cls.jptype) {
            case EnumChild: {
                EnumJChild self = (EnumJChild) cls;

                Object parent = table.get("$parent");
                Object child  = table.get("$child");

                if (parent == null || child == null)
                    return res.success(new Bool(false));

                if (!parent.equals(self.parent.name) || !child.equals(self.val))
                    return res.success(new Bool(false));

                break;
            }
            case ClassPlate: {
                ClassPlate self = (ClassPlate) cls;

                if (!self.name.equals(other.parent))
                    return res.success(new Bool(false));

                break;
            }
            default: return res.failure(RTError.Type(
                    cls.get_start(), cls.get_end(),
                    "Expected data type",
                    context
            ));
        }

        for (Map.Entry<Token, Obj> entry : checks.entrySet()) {
            String key = entry.getKey().value.toString();
            Object v = table.get(key);
            if (v == null)
                return res.failure(RTError.Scope(
                        entry.getKey().pos_start, entry.getKey().pos_end,
                        "Attribute does not exist",
                        context
                ));
            Obj val = (Obj) v;
            if (!val.equals(entry.getValue()))
                return res.success(new Bool(false));
        }

        for (Map.Entry<Token, String> entry : substitutes.entrySet()) {
            String key = entry.getKey().value.toString();
            Object v = table.get(key);
            if (v == null)
                return res.failure(RTError.Scope(
                        entry.getKey().pos_start, entry.getKey().pos_end,
                        "Attribute does not exist",
                        context
                ));
            context.symbolTable.define(entry.getValue(), v);
        }

        return res.success(new Bool(true));
    }
}
