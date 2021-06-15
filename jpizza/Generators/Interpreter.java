package lemon.jpizza.Generators;

import lemon.jpizza.*;
import lemon.jpizza.Cases.Case;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Double;
import lemon.jpizza.Errors.Error;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Nodes.Definitions.*;
import lemon.jpizza.Nodes.Expressions.*;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Nodes.Operations.BinOpNode;
import lemon.jpizza.Nodes.Operations.UnaryOpNode;
import lemon.jpizza.Nodes.Values.*;
import lemon.jpizza.Nodes.Variables.AttrAccessNode;
import lemon.jpizza.Nodes.Variables.VarAccessNode;
import lemon.jpizza.Objects.Executables.*;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Results.RTResult;

import static lemon.jpizza.Tokens.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@SuppressWarnings("unused")
public class Interpreter {

    Clock clock = new Clock();

    public boolean memoize = false;
    public Memo memo = new Memo();

    public Interpreter(Memo memo, boolean memoize) {
        this.memo = memo;
        this.memoize = memoize;
    }

    public Interpreter() {}

    interface Condition {
        boolean go(double x);
    }

    public RTResult visit(Node node, Context context) {
        String methodName = "visit_"+node.getClass().getSimpleName();
        try {
            clock.tick();
            Method method = Interpreter.class.getMethod(methodName, Node.class, Context.class);
            RTResult res = (RTResult) method.invoke(this, node, context);
            System.out.println(clock.tick());
            return res;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.printf("No %s method defined!%n", methodName); return null;
        }
    }

    public RTResult visit_ListNode(Node node, Context context) {
        RTResult res = new RTResult();
        List<Obj> elements = new ArrayList<>();
        Object[] nodeElements = ((ListNode) node).elements.toArray();
        int length = nodeElements.length;
        for (int i = 0; i < length; i++) {
            elements.add((Obj) res.register(this.visit((Node) nodeElements[i], context)));
            if (res.shouldReturn())
                return res;
        } return res.success(new PList(elements).set_context(context).set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_StringNode(Node node, Context context) {
        return new RTResult().success(new Str((String) ((StringNode) node).tok.value).set_context(context)
                .set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_NullNode(Node node, Context context) {
        return new RTResult().success(new Null().set_context(context).set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_DictNode(Node node, Context context) {
        RTResult res = new RTResult();
        Dict dict = new Dict(new HashMap<>());

        Map.Entry<Object, Object>[] entrySet = new Map.Entry[0];
        entrySet = (((DictNode) node).dict).entrySet().toArray(entrySet);
        int length = entrySet.length;
        for (int i = 0; i < length; i++) {
            Map.Entry<Object, Object> entry = entrySet[i];
            Obj key = (Obj) res.register(visit((Node) entry.getKey(), context));
            if (res.shouldReturn()) return res;
            Obj value = (Obj) res.register(visit((Node) entry.getValue(), context));
            if (res.shouldReturn()) return res;
            dict.set(key, value);
        } return res.success(dict.set_context(context).set_pos(node.pos_start, node.pos_end));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult visit_ForNode(Node node, Context context) {
        RTResult res = new RTResult();
        List<Obj> elements = new ArrayList<>();

        ForNode loop = (ForNode) node;

        Obj startNode = ((Obj) res.register(visit(loop.start_value_node, context)));
        if (res.shouldReturn()) return res;
        if (!(startNode instanceof Num)) return res.failure(new RTError(
                startNode.pos_start, startNode.pos_end,
                "Start must be an integer!",
                context
        ));
        double start = ((Num) startNode).trueValue();
        Obj endNode = ((Obj) res.register(visit(loop.end_value_node, context)));
        if (res.shouldReturn()) return res;
        if (!(endNode instanceof Num)) return res.failure(new RTError(
                endNode.pos_start, endNode.pos_end,
                "Start must be an integer!",
                context
        ));
        double end = ((Num) endNode).trueValue();
        if (res.shouldReturn()) return res;

        double step;
        if (loop.step_value_node != null) {
            Obj stepNode = ((Obj) res.register(visit(loop.step_value_node, context)));
            if (res.shouldReturn()) return res;
            if (!(stepNode instanceof Num)) return res.failure(new RTError(
                    stepNode.pos_start, stepNode.pos_end,
                    "Start must be an integer!",
                    context
            ));
            step = ((Num) startNode).trueValue();
        } else {
            step = 1;
        }

        double i = start;
        Condition condition;
        if (step >= 0)
            condition = x -> x < end;
        else
            condition = x -> x > end;

        String vtk = (String) loop.var_name_tok.value;
        Obj value;

        clock.tick();
        while (condition.go(i)) {
            context.symbolTable.set(vtk, new Num(i));
            i += step;

            value = (Obj) res.register(visit(loop.body_node, context));
            //value = null;
            if (res.shouldReturn() && !res.continueLoop && !res.breakLoop) return res;

            if (res.continueLoop) continue;
            if (res.breakLoop) break;

            elements.add(value);
            //System.out.println(clock.tick());
        }

        context.symbolTable.remove(vtk);

        return res.success(
                loop.retnull ? new Null() : new PList(elements).set_context(context)
                        .set_pos(node.pos_start, node.pos_end)
        );
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult visit_WhileNode(Node node, Context context) {
        RTResult res = new RTResult();
        List<Obj> elements = new ArrayList<>();

        WhileNode loop = (WhileNode) node;
        Node conditionNode = loop.condition_node;
        Obj condition, value;
        while (true) {
            condition = (Obj) res.register(visit(conditionNode, context));
            if (res.shouldReturn()) return res;

            if (!((Bool) condition.bool()).trueValue()) break;

            value = (Obj) res.register(visit(loop.body_node, context));
            if (res.shouldReturn() && !res.continueLoop && !res.breakLoop) return res;

            if (res.continueLoop) continue;
            if (res.breakLoop) break;

            elements.add(value);
        }

        return res.success(loop.retnull ? new Null() : new PList(elements).set_context(context)
                .set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_NumberNode(Node node, Context context) {
        Object value = ((ValueNode) node).tok.value;
        double v;
        if (value instanceof Long) v = ((Long) value).doubleValue();
        else v = (double) value;
        return new RTResult().success(new Num(v).set_context(context)
                .set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_MethDefNode(Node node, Context context) {
        RTResult res = new RTResult();

        MethDefNode defNode = (MethDefNode) node;
        String funcName = (String) defNode.var_name_tok.value;
        Token nameTok = defNode.var_name_tok;
        Node bodyNode = defNode.body_node;
        List<String> argNames = gatherArgs(defNode.arg_name_toks);
        CMethod methValue = new CMethod(funcName, nameTok, context, bodyNode, argNames, defNode.bin, defNode.async,
                defNode.autoreturn);

        context.symbolTable.set(funcName, methValue);
        return res.success(methValue);
    }

    public RTResult visit_ClassDefNode(Node node, Context context) {
        RTResult res = new RTResult();
        ClassDefNode defNode = (ClassDefNode) node;

        String name = (String) defNode.class_name_tok.value;
        Context classContext = new Context("<"+name+"-context>", context, node.pos_start);
        classContext.symbolTable = new SymbolTable(context.symbolTable);
        Token[] attributes = new Token[0];
        attributes = defNode.attribute_name_toks.toArray(attributes);
        List<String> argNames = new ArrayList<>();
        int size = defNode.arg_name_toks.size();
        for (int i = 0; i < size; i++) argNames.add((String) defNode.arg_name_toks.get(i).value);
        CMethod make = (CMethod) new CMethod("<make>", null, classContext, defNode.make_node, argNames, false,
                false, false).set_pos(node.pos_start, node.pos_end);
        size = defNode.methods.size();
        CMethod[] methods = new CMethod[size];
        for (int i = 0; i < size; i++) {
            Object mthd = res.register(visit_MethDefNode(defNode.methods.get(i), classContext));
            if (res.shouldReturn()) return res;
            methods[i] = (CMethod) mthd;
        }

        Value classValue = new ClassPlate(name, attributes, make, methods)
                .set_context(classContext).set_pos(node.pos_start, node.pos_end);
        context.symbolTable.set(name, classValue);

        return res.success(classValue);
    }

    public List<String> gatherArgs(List<Token> argNameToks) {
        List<String> argNames = new ArrayList<>();
        int size = argNameToks.size();
        for (int i = 0; i < size; i++) argNames.add((String) argNameToks.get(i).value);
        return argNames;
    }

    public RTResult visit_FuncDefNode(Node node, Context context) {
        RTResult res = new RTResult();
        FuncDefNode defNode = (FuncDefNode) node;

        String funcName = defNode.var_name_tok != null ? (String) defNode.var_name_tok.value : null;
        Node bodyNode = defNode.body_node;
        List<String> argNames = gatherArgs(defNode.arg_name_toks);
        Value funcValue = new Function(funcName, bodyNode, argNames, defNode.async, defNode.autoreturn)
                .set_context(context).set_pos(node.pos_start, node.pos_end);

        if (funcName != null) context.symbolTable.set(funcName, funcValue);

        return res.success(funcValue);
    }

    public RTResult visit_ReturnNode(Node node, Context context) {
        RTResult res = new RTResult();

        Node ret = ((ReturnNode) node).nodeToReturn;
        Obj value;
        if (ret != null) {
            value = (Obj) res.register(visit(ret, context));
            if (res.shouldReturn()) return res;
        } else value = new Null();

        return res.sreturn(value);
    }

    public RTResult visit_ContinueNode(Node node, Context context) { return new RTResult().scontinue(); }

    public RTResult visit_BreakNode(Node node, Context context) { return new RTResult().sbreak(); }

    public RTResult visit_CallNode(Node node, Context context) {
        RTResult res = new RTResult();
        List<Obj> args = new ArrayList<>();
        CallNode cn = (CallNode) node;
        Obj valueToCall = ((Obj) res.register(visit(cn.nodeToCall, context))).function();
        if (res.shouldReturn()) return res;
        if (valueToCall instanceof CMethod)
            valueToCall = valueToCall.copy().set_pos(node.pos_start, node.pos_end);
        else
            valueToCall = valueToCall.copy().set_pos(node.pos_start, node.pos_end).set_context(context);
        int size = cn.argNodes.size();
        for (int i = 0; i < size; i++) {
            Obj obj = (Obj) res.register(visit(cn.argNodes.get(i), context));
            args.add(obj);
            if (res.shouldReturn()) return res;
        }

        BaseFunction bValueToCall;
        ClassPlate cValueToCall;
        Obj retValue;
        if (valueToCall instanceof BaseFunction) {
            bValueToCall = (BaseFunction) valueToCall;
            Cache cache;
            if (bValueToCall instanceof Library)
                cache = null;
            else
                cache = (Cache) memo.get(bValueToCall.name, args.toArray(new Obj[0]));

            if (memoize && (cache != null)) retValue = (Obj) cache.result;
            else {
                if (bValueToCall.isAsync()) {
                    Thread thread = new Thread(() -> bValueToCall.execute(args, this));
                    return res.success(new Null().set_pos(node.pos_start, node.pos_end).set_context(context));
                }
                retValue = (Obj) res.register(bValueToCall.execute(args, this));
                if (memoize) memo.add(new Cache(bValueToCall.name, args.toArray(new Obj[0]), retValue));
            }
        } else {
            cValueToCall = (ClassPlate) valueToCall;
            retValue = (Obj) res.register(cValueToCall.execute(args, this));
        }
        if (res.shouldReturn()) return res;
        return res.success(retValue.copy().set_pos(node.pos_start, node.pos_end).set_context(context));
    }

    public RTResult visit_QueryNode(Node node, Context context) {
        RTResult res = new RTResult();
        QueryNode qnode = (QueryNode) node;

        Obj conditionValue, exprValue;
        int size = qnode.cases.size();
        for (int i = 0; i < size; i++) {
            Case c = qnode.cases.get(i);
            conditionValue = (Obj) res.register(visit(c.condition, context));
            if (res.shouldReturn()) return res;
            Obj bx = conditionValue.bool();
            if (!(bx instanceof Bool)) return res.failure(new RTError(
                    node.pos_start, node.pos_end,
                    "Conditional must be a boolean!",
                    context
            ));
            Bool b = (Bool) bx;
            if (b.trueValue()) {
                exprValue = (Obj) res.register(visit(c.statements, context));
                if (res.shouldReturn()) return res;
                return res.success(c.x ? new Null() : exprValue);
            }
        }

        if (qnode.else_case != null) {
            Obj elseValue = (Obj) res.register(visit(qnode.else_case.statements, context));
            if (res.shouldReturn()) return res;
            return res.success(qnode.else_case.x ? new Null() : elseValue);
        }

        return res.success(new Null());
    }

    public RTResult visit_BooleanNode(Node node, Context context) {
        return new RTResult().success(new Bool((boolean) ((ValueNode) node).tok.value).set_context(context)
                .set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_ClaccessNode(Node node, Context context) {
        RTResult res = new RTResult();
        ClaccessNode cnode = (ClaccessNode) node;

        Obj var = (Obj) res.register(visit(cnode.class_tok, context));

        if (res.error != null) return res;
        if (!(var instanceof ClassInstance)) return res.failure(new RTError(
                node.pos_start, node.pos_end,
                "Expected class instance",
                context
        ));
        Object val = ((ClassInstance) var).access((String) cnode.attr_name_tok.value);
        if (val instanceof String)
            return getThis(val, context, node.pos_start, node.pos_end);
        return res.success(((Obj)val).set_context(((ClassInstance)var).value));
    }

    public RTResult getThis(Object val, Context context, Position pos_start, Position pos_end) {
            while (context.displayName.equals(val)) {
                if (context.parent == null) return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        "Invalid 'this'",
                        context
                ));
                context = context.parent;
            } return new RTResult().success(new ClassInstance(context).copy().set_pos(pos_start, pos_end)
                    .set_context(context));
    }

    public RTResult visit_VarAccessNode(Node node, Context context) {
        RTResult res = new RTResult();
        VarAccessNode anode = (VarAccessNode) node;

        String varName = (String) anode.var_name_tok.value;
        Object value = context.symbolTable.get(varName);

        if (value == null) return res.failure(new RTError(
                node.pos_start, node.pos_end,
                "'" + varName + "' is not defined",
                context
        ));
        if (value instanceof String)
            return getThis(value, context, node.pos_start, node.pos_end);
        Obj val = ((Obj) value).copy().set_pos(node.pos_start, node.pos_end).set_context(context);
        return res.success(val);
    }

    public RTResult visit_VarAssignNode(Node node, Context context) {
        RTResult res = new RTResult();
        VarAssignNode assignment = (VarAssignNode) node;

        String varName = (String) assignment.var_name_tok.value;
        Obj value = (Obj) res.register(visit(assignment.value_node, context));
        if (res.shouldReturn()) return res;

        String error = context.symbolTable.set(varName, value, assignment.locked);
        if (error != null) return res.failure(new RTError(
                node.pos_start, node.pos_end,
                error,
                context
        ));

        return res.success(value);
    }

    public RTResult visit_AttrAssignNode(Node node, Context context) {
        RTResult res = new RTResult();
        AttrAssignNode assignment = (AttrAssignNode) node;

        String varName = (String) assignment.var_name_tok.value;
        Obj value = (Obj) res.register(visit(assignment.value_node, context));
        if (res.shouldReturn()) return res;

        context.symbolTable.setattr(varName, value);
        return res.success(value);

    }

    public RTResult getImprt(String path, String fn, Context context, Position pos_start, Position pos_end)
            throws IOException {
        Double<ClassInstance, Error> i = Shell.imprt(fn, Files.readString(Paths.get(path)), context, pos_start);
        ClassInstance imp = i.a;
        Error error = i.b;
        if (error != null) return new RTResult().failure(error);
        imp.set_pos(pos_start, pos_end).set_context(context);
        return new RTResult().success(imp);
    }

    public RTResult visit_ImportNode(Node node, Context context) throws IOException {
        ImportNode in = (ImportNode) node;
        String fn = (String) in.file_name_tok.value;
        String file_name = fn + ".devp";
        String modPath = "C:\\DP\\modules\\" + fn;
        String modFilePath = modPath + "\\" + file_name;
        var mkdirs = new File("C:\\DP\\modules").mkdirs();
        ClassInstance imp = null;
        RTResult res = new RTResult();
        if (Constants.LIBRARIES.containsKey(fn)) imp = (ClassInstance) new ClassInstance(Constants.LIBRARIES.get(fn))
                .set_pos(node.pos_start, node.pos_end).set_context(context);
        else {
            if (Files.exists(Paths.get(modPath)))
                imp = (ClassInstance) res.register(getImprt(modFilePath, fn, context, node.pos_start,
                        node.pos_end));
            else if (Files.exists(Paths.get(file_name)))
                imp = (ClassInstance) res.register(getImprt(modFilePath, file_name, context, node.pos_start,
                        node.pos_end));
            if (res.error != null) return res;
        }
        if (imp == null) return res.failure(new RTError(
                node.pos_start, node.pos_end,
                "Module does not exist!",
                context
        ));
        context.symbolTable.set(fn, imp);
        return res.success(new Null());
    }

    public RTResult visit_AttrAccessNode(Node node, Context context) {
        RTResult res = new RTResult();
        AttrAccessNode attrAccessNode = (AttrAccessNode) node;

        String varName = (String) attrAccessNode.var_name_tok.value;
        Obj value = (Obj) context.symbolTable.getattr(varName);

        if (value == null) return res.failure(new RTError(
                node.pos_start, node.pos_end,
                "'" + varName + "' is not defined",
                context
        ));

        value = value.copy().set_pos(node.pos_start, node.pos_end).set_context(context);
        return res.success(value);
    }

    public RTResult visit_BinOpNode(Node node, Context context) {
        RTResult res = new RTResult();
        BinOpNode opNode = (BinOpNode) node;
        Double<Obj, RTError> ret;

        Obj left = (Obj) res.register(visit(opNode.left_node, context));
        if (res.shouldReturn()) return res;
        Obj right = (Obj) res.register(visit(opNode.right_node, context));
        if (res.shouldReturn()) return res;


        if (opNode.op_tok.type.equals(TT_GT) || opNode.op_tok.type.equals(TT_GTE)) {
            ret = (Double<Obj, RTError>) right.getattr(opNode.op_tok.type.equals(TT_GT) ? "lte" : "lt", left);
        } else ret = (Double<Obj, RTError>) left.getattr(switch (opNode.op_tok.type) {
            case TT_MINUS -> "sub";
            case TT_MUL -> "mul";
            case TT_DIV -> "div";
            case TT_POWER -> "fastpow";
            case TT_EE -> "eq";
            case TT_NE -> "ne";
            case TT_LT -> "lt";
            case TT_LTE -> "lte";
            case TT_AND -> "including";
            case TT_OR -> "also";
            case TT_MOD -> "mod";
            case TT_DOT -> "get";
            default -> "add";
        }, right);
        if (ret.b != null) return res.failure(ret.b);
        return res.success((ret.a).set_pos(node.pos_start, node.pos_end).set_context(context));
    }

    public RTResult visit_UnaryOpNode(Node node, Context context) {
        RTResult res = new RTResult();
        UnaryOpNode unOp = (UnaryOpNode) node;

        Obj number = (Obj) res.register(visit(unOp.node, context));
        if (res.shouldReturn()) return res;

        String opTokType = unOp.op_tok.type;
        Double<Obj, RTError> ret;
        if (opTokType.equals(TT_MINUS))
            ret = (Double<Obj, RTError>) number.getattr("mul", new Num(-1));
        else if (Arrays.asList(TT_INCR, TT_DECR).contains(opTokType))
            ret = (Double<Obj, RTError>) number.getattr("add", new Num(opTokType.equals(TT_INCR) ? 1 : -1));
        else if (opTokType.equals(TT_NOT))
            ret = (Double<Obj, RTError>) number.getattr("invert");
        else
            ret = new Double<>(number, null);
        if (ret.b != null)
            return res.failure(ret.b);
        number = ret.a;
        return res.success(number.set_pos(node.pos_start, node.pos_end).set_context(context));
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public RTResult visit_UseNode(Node node, Context context) {
        Token useToken = ((UseNode) node).useToken;
        switch ((String) useToken.value) {
            case "memoize":
                memoize = true;
                break;
            default:
                break;

        }
        return new RTResult().success(new Null());
    }

}
