package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Cache;
import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Executables.BaseFunction;
import lemon.jpizza.Objects.Executables.ClassPlate;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.EnumJChild;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Results.RTResult;

import java.util.ArrayList;
import java.util.List;

public class CallNode extends Node {
    public Node nodeToCall;
    public List<Node> argNodes;
    public boolean fluctuating = true;

    public CallNode(Node nodeToCall, List<Node> argNodes) {
        this.nodeToCall = nodeToCall;
        this.argNodes = argNodes;

        pos_start = nodeToCall.pos_start.copy();
        pos_end = (argNodes != null && argNodes.size() > 0 ? argNodes.get(argNodes.size() - 1) : nodeToCall).pos_end.copy();
        jptype = Constants.JPType.Call;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        List<Obj> args = new ArrayList<>();
        Obj valueToCall = res.register(nodeToCall.visit(inter, context));
        if (res.shouldReturn()) return res;

        valueToCall = valueToCall.function();
        if (valueToCall.jptype == Constants.JPType.CMethod)
            valueToCall = valueToCall.copy().set_pos(pos_start, pos_end);
        else
            valueToCall = valueToCall.copy().set_pos(pos_start, pos_end).set_context(context);
        int size = argNodes.size();
        for (int i = 0; i < size; i++) {
            Obj obj = res.register(argNodes.get(i).visit(inter, context));
            args.add(obj);
            if (res.shouldReturn()) return res;
        }

        if (valueToCall.jptype == Constants.JPType.EnumChild) {
            Obj ret = res.register(((EnumJChild) valueToCall).instance(context, args));
            if (res.error != null) return res;
            return res.success(ret);
        }

        BaseFunction bValueToCall;
        ClassPlate cValueToCall;
        Obj retValue;
        if (valueToCall.jptype == Constants.JPType.ClassPlate) {
            cValueToCall = (ClassPlate) valueToCall;
            retValue = res.register(cValueToCall.execute(args, inter));
        } else {
            bValueToCall = (BaseFunction) valueToCall.copy().set_pos(pos_start, pos_end);
            Cache cache;
            if (bValueToCall.jptype == Constants.JPType.Library)
                cache = null;
            else
                cache = (Cache) inter.memo.get(bValueToCall.name, args.toArray(new Obj[0]));

            if (context.memoize && (cache != null)) retValue = (Obj) cache.result;
            else {
                if (bValueToCall.isAsync()) {
                    Thread thread = new Thread(() -> bValueToCall.execute(args, inter));
                    thread.start();
                    return res.success(new Null().set_pos(pos_start, pos_end).set_context(context));
                }
                retValue = res.register(bValueToCall.execute(args, inter));
                if (context.memoize)
                    inter.memo.add(new Cache(bValueToCall.name, args.toArray(new Obj[0]), retValue));
            }
        }
        if (res.shouldReturn()) return res;
        return res.success(retValue.copy().set_pos(pos_start, pos_end).set_context(context));
    }

}
