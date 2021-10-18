package lemon.jpizza.nodes.definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.Token;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.variables.AttrNode;
import lemon.jpizza.nodes.variables.VarNode;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.executables.ClassInstance;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DestructNode extends Node {
    Node target;
    List<Token> subs = new ArrayList<>();
    boolean glob = false;

    public DestructNode(Node tar) {
        target = tar;
        glob = true;

        pos_start = tar.pos_start;
        pos_end = tar.pos_end;
    }

    public DestructNode(Node tar, List<Token> tars) {
        target = tar;
        subs = tars;

        pos_start = tars.get(0).pos_start;
        pos_end = tar.pos_end;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj tar = res.register(target.visit(inter, context));
        if (res.error != null) return res;
        if (tar.jptype != Constants.JPType.ClassInstance) return res.failure(RTError.Type(
                tar.get_start(), tar.get_end(),
                "Expected class instance",
                context
        ));

        if (glob) return glob((ClassInstance) tar, inter, context);
        else      return spread((ClassInstance) tar, inter, context);
    }

    public RTResult glob(ClassInstance tar, Interpreter inter, Context context) {
        Set<Map.Entry<String, AttrNode>> attrs = tar.ctx.symbolTable.attributes().entrySet();
        for (Map.Entry<String, AttrNode> entry : attrs)
            context.symbolTable.define(entry.getKey(), entry.getValue().value_node);

        Set<Map.Entry<String, VarNode>> symbols = tar.ctx.symbolTable.symbols().entrySet();
        for (Map.Entry<String, VarNode> entry : symbols)
            context.symbolTable.define(entry.getKey(), entry.getValue().value_node);

        return new RTResult().success(new Null());
    }

    public RTResult spread(ClassInstance tar, Interpreter inter, Context context) {
        Map<String, AttrNode> attrs = tar.ctx.symbolTable.attributes();
        for (Token struct : subs) {
            String sub = struct.value.toString();
            Object v = tar.ctx.symbolTable.get(sub);
            if (v == null) return new RTResult().failure(RTError.Scope(
                    struct.pos_start, struct.pos_end,
                    "Attribute not in class",
                    context
            ));
            context.symbolTable.define(sub, v);
        }
        return new RTResult().success(new Null());
    }
}
