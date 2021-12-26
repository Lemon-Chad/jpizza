package lemon.jpizza.nodes.expressions;

import lemon.jpizza.*;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.executables.BaseFunction;
import lemon.jpizza.objects.executables.ClassPlate;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.executables.EnumJChild;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

import java.util.*;

public class CallNode extends Node {
    public final Node nodeToCall;
    public final List<Node> argNodes;
    public final Map<String, Node> kwargs;
    public final List<Token> generics;

    public CallNode(Node nodeToCall, List<Node> argNodes, List<Token> generics, Map<String, Node> kwargs) {
        this.nodeToCall = nodeToCall;
        this.argNodes = argNodes;
        this.generics = generics;
        this.kwargs = kwargs;

        pos_start = nodeToCall.pos_start.copy();
        pos_end = (argNodes != null && argNodes.size() > 0 ? argNodes.get(argNodes.size() - 1) : nodeToCall).pos_end.copy();
        jptype = JPType.Call;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();
        List<Obj> args = new ArrayList<>();
        Map<String, Obj> kwargs = new HashMap<>();
        Obj valueToCall = res.register(inter.visit(nodeToCall, context));
        if (res.shouldReturn()) return res;

        valueToCall = valueToCall.function().copy().set_pos(pos_start, pos_end);
        int size = argNodes.size();
        for (int i = 0; i < size; i++) {
            Node node = argNodes.get(i);
            if (node.jptype == JPType.Spread) {
                SpreadNode spread = (SpreadNode) node;
                Obj obj = res.register(inter.visit(spread.internal, context));
                if (res.shouldReturn()) return res;
                args.addAll(obj.alist().list);
            }
            else {
                Obj obj = res.register(inter.visit(argNodes.get(i), context));
                args.add(obj);
                if (res.shouldReturn()) return res;
            }
        }

        List<Token> processedTypes = new ArrayList<>();
        for (Token generic : generics)
            processedTypes.add(new Token(Tokens.TT.TYPE,
                    Collections.singletonList(context.symbolTable.getType((List<String>) generic.value)),
                    generic.pos_start, generic.pos_end));

        for (Map.Entry<String, Node> entry : this.kwargs.entrySet()) {
            Obj obj = res.register(inter.visit(entry.getValue(), context));
            kwargs.put(entry.getKey(), obj);
            if (res.shouldReturn()) return res;
        }

        if (valueToCall.jptype == JPType.EnumChild) {
            Obj ret = res.register(((EnumJChild) valueToCall).instance(context, args, processedTypes));
            if (res.error != null) return res;
            return res.success(ret);
        }

        BaseFunction bValueToCall;
        ClassPlate cValueToCall;
        Obj retValue;
        if (valueToCall.jptype == JPType.ClassPlate) {
            cValueToCall = (ClassPlate) valueToCall;
            retValue = res.register(cValueToCall.execute(args, processedTypes, kwargs, inter));
        }
        else {
            bValueToCall = (BaseFunction) valueToCall.copy().set_pos(pos_start, pos_end);
            Cache cache;
            if (bValueToCall.jptype == JPType.Library)
                cache = null;
            else
                cache = (Cache) inter.memo.get(bValueToCall.name, args.toArray(new Obj[0]));

            if (context.memoize && (cache != null)) retValue = (Obj) cache.result;
            else {
                if (bValueToCall.isAsync()) {
                    Thread thread = new Thread(() -> bValueToCall.execute(args, processedTypes, kwargs, inter));
                    thread.start();
                    return res.success(new Null().set_pos(pos_start, pos_end).set_context(context));
                }
                retValue = res.register(bValueToCall.execute(args, processedTypes, kwargs, inter));
                if (context.memoize)
                    inter.memo.add(new Cache(bValueToCall.name, args.toArray(new Obj[0]), retValue));
            }
        }
        if (res.shouldReturn()) return res;
        return res.success(retValue.copy().set_pos(pos_start, pos_end));
    }

    @Override
    public Node optimize() {
        List<Node> optimizedArgNodes = new ArrayList<>();
        for (Node argNode : argNodes) {
            Node optimized = argNode.optimize();
            if (optimized.jptype == JPType.Spread && optimized.constant) {
                SpreadNode spread = (SpreadNode) optimized;
                if (spread.internal.jptype == JPType.List) {
                    for (Node node : spread.internal.asList())
                        optimizedArgNodes.add(node.optimize());
                }
            }
            else {
                optimizedArgNodes.add(optimized);
            }
        }

        Map<String, Node> optimizedKwargs = new HashMap<>();
        for (Map.Entry<String, Node> entry : kwargs.entrySet()) {
            Node optimized = entry.getValue().optimize();
            optimizedKwargs.put(entry.getKey(), optimized);
        }

        return new CallNode(nodeToCall, optimizedArgNodes, generics, optimizedKwargs);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>();
        children.add(nodeToCall);
        children.addAll(argNodes);
        children.addAll(kwargs.values());
        return children;
    }

    @Override
    public String visualize() {
        return "call";
    }
}
