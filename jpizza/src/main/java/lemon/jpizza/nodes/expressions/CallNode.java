package lemon.jpizza.nodes.expressions;

import lemon.jpizza.Cache;
import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.executables.BaseFunction;
import lemon.jpizza.objects.executables.ClassPlate;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.EnumJChild;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallNode extends Node {
    public final Node nodeToCall;
    public final List<Node> argNodes;
    public final Map<String, Node> kwargs;
    public final List<Token> generics;
    public boolean fluctuating = true;

    public CallNode(Node nodeToCall, List<Node> argNodes, List<Token> generics, Map<String, Node> kwargs) {
        this.nodeToCall = nodeToCall;
        this.argNodes = argNodes;
        this.generics = generics;
        this.kwargs = kwargs;

        pos_start = nodeToCall.pos_start.copy();
        pos_end = (argNodes != null && argNodes.size() > 0 ? argNodes.get(argNodes.size() - 1) : nodeToCall).pos_end.copy();
        jptype = Constants.JPType.Call;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        List<Obj> args = new ArrayList<>();
        Map<String, Obj> kwargs = new HashMap<>();
        Obj valueToCall = res.register(nodeToCall.visit(inter, context));
        if (res.shouldReturn()) return res;

        valueToCall = valueToCall.function();
        if (valueToCall.jptype == Constants.JPType.CMethod)
            valueToCall = valueToCall.copy().set_pos(pos_start, pos_end);
        else
            valueToCall = valueToCall.copy().set_pos(pos_start, pos_end).set_context(context);
        int size = argNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = argNodes.get(i);
            if (node.jptype == Constants.JPType.Spread) {
                SpreadNode spread = (SpreadNode) node;
                Obj obj = res.register(spread.internal.visit(inter, context));
                if (res.shouldReturn()) return res;
                args.addAll(obj.alist().list);
            } else {
                Obj obj = res.register(argNodes.get(i).visit(inter, context));
                args.add(obj);
                if (res.shouldReturn()) return res;
            }
        }

        for (Map.Entry<String, Node> entry : this.kwargs.entrySet()) {
            Obj obj = res.register(entry.getValue().visit(inter, context));
            kwargs.put(entry.getKey(), obj);
            if (res.shouldReturn()) return res;
        }

        if (valueToCall.jptype == Constants.JPType.EnumChild) {
            Obj ret = res.register(((EnumJChild) valueToCall).instance(context, args, generics));
            if (res.error != null) return res;
            return res.success(ret);
        }

        BaseFunction bValueToCall;
        ClassPlate cValueToCall;
        Obj retValue;
        if (valueToCall.jptype == Constants.JPType.ClassPlate) {
            cValueToCall = (ClassPlate) valueToCall;
            retValue = res.register(cValueToCall.execute(args, generics, kwargs, inter));
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
                    Thread thread = new Thread(() -> bValueToCall.execute(args, generics, kwargs, inter));
                    thread.start();
                    return res.success(new Null().set_pos(pos_start, pos_end).set_context(context));
                }
                retValue = res.register(bValueToCall.execute(args, generics, kwargs, inter));
                if (context.memoize)
                    inter.memo.add(new Cache(bValueToCall.name, args.toArray(new Obj[0]), retValue));
            }
        }
        if (res.shouldReturn()) return res;
        return res.success(retValue.copy().set_pos(pos_start, pos_end).set_context(context));
    }

}
