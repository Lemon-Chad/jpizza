package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Executables.ClassInstance;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.EnumJ;
import lemon.jpizza.Objects.Primitives.EnumJChild;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Operations;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

public class ClaccessNode extends Node {
    public Node class_tok;
    public Token attr_name_tok;

    public ClaccessNode(Node cls, Token atr) {
        class_tok = cls;
        attr_name_tok = atr;
        pos_start = cls.pos_start.copy(); pos_end = atr.pos_end.copy();
        jptype = Constants.JPType.Claccess;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        Obj var = res.register(class_tok.visit(inter, context));

        if (res.error != null) return res;
        if (var.jptype != Constants.JPType.ClassInstance && var.jptype != Constants.JPType.Enum) return res.failure(new RTError(
                pos_start, pos_end,
                "Expected class instance or enum",
                context
        ));

        if (var.jptype == Constants.JPType.Enum) {
            EnumJChild child = ((EnumJ) var).getChild((String) attr_name_tok.value);
            if (child == null)
                return res.failure(new RTError(
                        pos_start.copy(), pos_end.copy(),
                        "Enum child is undefined!",
                        context
                ));
            return res.success(child.
                    set_context(context).set_pos(pos_start, pos_end));
        }

        Object val = var.getattr(Operations.OP.ACCESS, new Str((String) attr_name_tok.value)
                .set_pos(pos_start, pos_end)
                .set_context(context));
        if (val instanceof String)
            return Interpreter.getThis(val, context, pos_start, pos_end);
        else if (val instanceof RTError) return new RTResult().failure((RTError) val);
        return res.success(((Obj)val).set_context(((ClassInstance)var).value));
    }

}
