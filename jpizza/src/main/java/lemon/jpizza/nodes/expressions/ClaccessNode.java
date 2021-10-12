package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.executables.CMethod;
import lemon.jpizza.objects.executables.ClassInstance;
import lemon.jpizza.objects.executables.ClassPlate;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.executables.EnumJ;
import lemon.jpizza.objects.executables.EnumJChild;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

public class ClaccessNode extends Node {
    public final Node class_tok;
    public final Token attr_name_tok;
    public boolean fluctuating = true;

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
        if (var.jptype != Constants.JPType.ClassInstance && var.jptype != Constants.JPType.Enum &&
                var.jptype != Constants.JPType.ClassPlate) return res.failure(RTError.Type(
                pos_start, pos_end,
                "Expected class instance or enum",
                context
        ));

        switch (var.jptype) {
            case Enum:
                EnumJChild child = ((EnumJ) var).getChild((String) attr_name_tok.value);
                if (child == null)
                    return res.failure(RTError.Scope(
                            pos_start.copy(), pos_end.copy(),
                            "Enum child is undefined",
                            context
                    ));
                return res.success(child.
                        set_context(context).set_pos(pos_start, pos_end));

            case ClassInstance:
                Object val = var.access(new Str((String) attr_name_tok.value)
                        .set_pos(pos_start, pos_end)
                        .set_context(context));
                if (val instanceof String)
                    return Interpreter.getThis(val, context, pos_start, pos_end);
                else if (val instanceof CMethod && ((CMethod) val).isprivate) return new RTResult().failure(RTError.Publicity(
                        pos_start, pos_end,
                        "Method is private",
                        context
                ));
                else if (val instanceof RTError) return new RTResult().failure((RTError) val);
                return res.success(((Obj)val).set_context(((ClassInstance)var).ctx));

            case ClassPlate:
                Obj acval = res.register(((ClassPlate) var).access(new Str(attr_name_tok.value.toString())));
                if (res.error != null) return res;
                return res.success(acval.set_context(context).set_pos(pos_start, pos_end));

            default:
                return res.failure(RTError.Type(
                        pos_start, pos_end,
                        "Type has no accessible traits",
                        context
                ));
        }
    }

}
