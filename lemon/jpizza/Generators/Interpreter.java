package lemon.jpizza.Generators;

import lemon.jpizza.*;
import lemon.jpizza.Cases.Case;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Pair;
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
import lemon.jpizza.Results.RTResult;

import static lemon.jpizza.Operations.*;
import static lemon.jpizza.Tokens.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SuppressWarnings("unused")
public class Interpreter {

    Clock clock = new Clock();
    public boolean reflection = false;

    public Memo memo = new Memo();
    boolean main = false;

    String fnFinish = null;
    String clFinish = null;

    public Interpreter(Memo memo) {
        this.memo = memo;
    }

    public Interpreter() {}

    public void makeMain() {
        main = true;
    }

    interface Condition {
        boolean go(double x);
    }

    public RTResult finish(Context context) {
        RTResult res = new RTResult();

        if (!main) return res.success(new Null());

        Object cargs = context.symbolTable.get("CMDARGS");
        Obj cmdArgs = cargs != null ? (PList) cargs : new PList(new ArrayList<>());

        if (fnFinish != null) {
            Object func = context.symbolTable.get(fnFinish);
            if (!(func instanceof Function)) return new RTResult().failure(new RTError(
                    null, null,
                    "Main function provided does not exist",
                    context
            ));

            Function fn = (Function) func;
            if (fn.argNames.size() != 1) return new RTResult().failure(new RTError(
                    fn.pos_start, fn.pos_end,
                    "Function must take 1 argument (CMD line arguments)",
                    context
            ));
            res.register(fn.execute(Collections.singletonList(cmdArgs), this));
            if (res.error != null) return res;
        }
        else if (clFinish != null) {
            Object cls = context.symbolTable.get(clFinish);
            if (!(cls instanceof ClassPlate)) return new RTResult().failure(new RTError(
                    null, null,
                    "Main recipe provided does not exist",
                    context
            ));

            ClassPlate recipe = (ClassPlate) cls;
            if (recipe.make.argNames.size() != 0) return new RTResult().failure(new RTError(
                    recipe.get_start(), recipe.get_end(),
                    "Recipe shouldn't take any arguments",
                    context
            ));

            ClassInstance clsi = (ClassInstance) res.register(recipe.execute(new ArrayList<>(), this));
            if (res.error != null) return res;

            Object func = clsi.getattr(OP.ACCESS, new Str("main").set_context(recipe.context));
            if (!(func instanceof CMethod)) return new RTResult().failure(new RTError(
                    recipe.get_start(), recipe.get_end(),
                    "Recipe has no main method",
                    recipe.context
            ));

            CMethod meth = (CMethod) func;
            if (meth.argNames.size() != 1) return new RTResult().failure(new RTError(
                    recipe.get_start(), recipe.get_end(),
                    "Method does not take in 1 argument",
                    recipe.context
            ));

            res.register(meth.execute(Collections.singletonList(cmdArgs), this));
            if (res.error != null) return res;
        }

        return res.success(new Null());
    }

    public RTResult visit(Node node, Context context) {
        String methodName = node.getClass().getSimpleName();
        Constants.JPType type = node.jptype;
        return switch (type) {
            case AttrAssign -> visit_AttrAssignNode ((AttrAssignNode)   node, context);
            case Switch     -> visit_SwitchNode     ((SwitchNode)       node, context);
            case ClassDef   -> visit_ClassDefNode   ((ClassDefNode)     node, context);
            case DynAssign  -> visit_DynAssignNode  ((DynAssignNode)    node, context);
            case FuncDef    -> visit_FuncDefNode    ((FuncDefNode)      node, context);
            case MethDef    -> visit_MethDefNode    ((MethDefNode)      node, context);
            case VarAssign  -> visit_VarAssignNode  ((VarAssignNode)    node, context);
            case Break      -> visit_BreakNode      ((BreakNode)        node, context);
            case Call       -> visit_CallNode       ((CallNode)         node, context);
            case Claccess   -> visit_ClaccessNode   ((ClaccessNode)     node, context);
            case Continue   -> visit_ContinueNode   ((ContinueNode)     node, context);
            case For        -> visit_ForNode        ((ForNode)          node, context);
            case Iter       -> visit_IterNode       ((IterNode)         node, context);
            case Pass       -> visit_PassNode       ((PassNode)         node, context);
            case Query      -> visit_QueryNode      ((QueryNode)        node, context);
            case Return     -> visit_ReturnNode     ((ReturnNode)       node, context);
            case Use        -> visit_UseNode        ((UseNode)          node, context);
            case While      -> visit_WhileNode      ((WhileNode)        node, context);
            case BinOp      -> visit_BinOpNode      ((BinOpNode)        node, context);
            case UnaryOp    -> visit_UnaryOpNode    ((UnaryOpNode)      node, context);
            case Boolean    -> visit_BooleanNode    ((BooleanNode)      node, context);
            case Dict       -> visit_DictNode       ((DictNode)         node, context);
            case List       -> visit_ListNode       ((ListNode)         node, context);
            case Null       -> visit_NullNode       ((NullNode)         node, context);
            case Number     -> visit_NumberNode     ((NumberNode)       node, context);
            case String     -> visit_StringNode     ((StringNode)       node, context);
            case AttrAccess -> visit_AttrAccessNode ((AttrAccessNode)   node, context);
            case VarAccess  -> visit_VarAccessNode  ((VarAccessNode)    node, context);
            case Enum       -> visit_EnumNode       ((EnumNode)         node, context);
            case Import     -> {
                try {
                    yield visit_ImportNode((ImportNode) node, context);
                } catch (IOException e) {
                    yield new RTResult().failure(new RTError(
                            node.pos_start.copy(), node.pos_end.copy(),
                            e.toString(),
                            context
                    ));
                }
            }
            default         -> new RTResult().failure(new RTError(
                                        node.pos_start.copy(), node.pos_end.copy(),
                                        "No visit method for " + methodName + "!",
                                        context
                                ));
        };
    }

    public RTResult visit_SwitchNode(SwitchNode node, Context context) {
        RTResult res = new RTResult();

        Obj ret = new Null();

        Obj ref = res.register(visit(node.reference, context));
        if (res.error != null) return res;

        int entry = -1;
        int size = node.cases.size();

        Obj compare;
        Case cs;
        for (int i = 0; i < size; i++) {
            cs = node.cases.get(i);
            compare = res.register(visit(cs.condition, context));
            if (res.error != null) return res;

            if (((Bool)((Pair<Obj, RTError>) ref.getattr(OP.EQ, compare)).a).trueValue()) {
                entry = i;
                break;
            }
        }

        if (node.autoreturn && entry > -1) {
            ret = res.register(visit(node.cases.get(entry).statements, context));
            if (res.error != null) return res;
        } else if (entry > -1) {
            for (; entry < size; entry++) {
                res.register(visit(node.cases.get(entry).statements, context));
                if (res.error != null) return res;
                if (res.breakLoop) break;
            }
        }

        if (entry == -1 && node.elseCase != null) {
            ret = res.register(visit(node.elseCase.statements, context));
            if (res.error != null) return res;
        }

        return res.success(node.autoreturn ? ret : new Null());
    }

    public RTResult visit_ListNode(ListNode node, Context context) {
        RTResult res = new RTResult();
        ArrayList<Obj> elements = new ArrayList<>();
        Object[] nodeElements = node.elements.toArray();
        int length = nodeElements.length;
        for (int i = 0; i < length; i++) {
            elements.add(res.register(this.visit((Node) nodeElements[i], context)));
            if (res.shouldReturn())
                return res;
        } return res.success(new PList(elements).set_context(context).set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_StringNode(StringNode node, Context context) {
        return new RTResult().success(new Str(node.val).set_context(context)
                .set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_NullNode(NullNode node, Context context) {
        return new RTResult().success(new Null().set_context(context).set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_DictNode(DictNode node, Context context) {
        RTResult res = new RTResult();
        Dict dict = new Dict(new HashMap<>());

        Map.Entry<Node, Node>[] entrySet = new Map.Entry[0];
        entrySet = (node.dict).entrySet().toArray(entrySet);
        int length = entrySet.length;
        for (int i = 0; i < length; i++) {
            Map.Entry<Node, Node> entry = entrySet[i];
            Obj key = res.register(visit(entry.getKey(), context));
            if (res.shouldReturn()) return res;
            Obj value = res.register(visit(entry.getValue(), context));
            if (res.shouldReturn()) return res;
            dict.set(key, value);
        } return res.success(dict.set_context(context).set_pos(node.pos_start, node.pos_end));
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult visit_ForNode(ForNode node, Context context) {
        RTResult res = new RTResult();

        Obj startNode = res.register(visit(node.start_value_node, context));
        if (res.shouldReturn()) return res;
        if (startNode.jptype != Constants.JPType.Number) return res.failure(new RTError(
                startNode.pos_start, startNode.pos_end,
                "Start must be an integer!",
                context
        ));
        double start = ((Num) startNode).trueValue();
        Obj endNode = res.register(visit(node.end_value_node, context));
        if (res.shouldReturn()) return res;
        if (endNode.jptype != Constants.JPType.Number) return res.failure(new RTError(
                endNode.pos_start, endNode.pos_end,
                "Start must be an integer!",
                context
        ));
        double end = ((Num) endNode).trueValue();
        if (res.shouldReturn()) return res;

        double step;
        if (node.step_value_node != null) {
            Obj stepNode = res.register(visit(node.step_value_node, context));
            if (res.shouldReturn()) return res;
            if (stepNode.jptype != Constants.JPType.Number) return res.failure(new RTError(
                    stepNode.pos_start, stepNode.pos_end,
                    "Start must be an integer!",
                    context
            ));
            step = ((Num) stepNode).trueValue();
        } else {
            step = 1;
        }
        long round = Math.round((end - start) / step);
        Obj[] elements = new Obj[(int) round + 1];

        double i = start;
        Condition condition = step >= 0 ? x -> x < end : x -> x > end;

        String vtk = (String) node.var_name_tok.value;
        Obj value;

        context.symbolTable.define(vtk, new Null());
        // clock.tick();
        while (condition.go(i)) {
            context.symbolTable.set(vtk, new Num(i));
            i += step;

            value = res.register(visit(node.body_node, context));
            // value = null;
            if (res.shouldReturn() && !res.continueLoop && !res.breakLoop) return res;

            if (res.continueLoop) continue;
            if (res.breakLoop) break;

            elements[(int) i] = value;
        }
        context.symbolTable.remove(vtk);

        return res.success(
                node.retnull ? new Null() : new PList(new ArrayList<>(Arrays.asList(elements))).set_context(context)
                        .set_pos(node.pos_start, node.pos_end)
        );
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult visit_IterNode(IterNode node, Context context) {
        RTResult res = new RTResult();
        List<Obj> elements = new ArrayList<>();

        Obj iterableNode = res.register(visit(node.iterable_node, context));
        if (res.shouldReturn()) return res;
        if (iterableNode.jptype != Constants.JPType.List) return res.failure(new RTError(
                iterableNode.pos_start, iterableNode.pos_end,
                "Value must be an iterable!",
                context
        ));
        List<Obj> iterable = ((PList) iterableNode).trueValue();

        double size = iterable.size();

        String vtk = (String) node.var_name_tok.value;
        Obj value;

        context.symbolTable.define(vtk, new Null());
        // clock.tick();
        for (int i = 0; i < size; i++) {
            context.symbolTable.set(vtk, iterable.get(i));

            value = res.register(visit(node.body_node, context));
            // value = null;
            if (res.shouldReturn() && !res.continueLoop && !res.breakLoop) return res;

            if (res.continueLoop) continue;
            if (res.breakLoop) break;

            elements.add(value);
        }

        context.symbolTable.remove(vtk);

        return res.success(
                node.retnull ? new Null() : new PList(new ArrayList<>(elements)).set_context(context)
                        .set_pos(node.pos_start, node.pos_end)
        );
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult visit_WhileNode(WhileNode node, Context context) {
        RTResult res = new RTResult();
        List<Obj> elements = new ArrayList<>();

        Node conditionNode = node.condition_node;
        Obj condition, value;
        while (true) {
            if (!node.conLast) {
                condition = res.register(visit(conditionNode, context));
                if (res.shouldReturn()) return res;

                if (!((Bool) condition.bool()).trueValue()) break;
            }

            value = res.register(visit(node.body_node, context));
            if (res.shouldReturn() && !res.continueLoop && !res.breakLoop) return res;

            if (res.continueLoop) continue;
            if (res.breakLoop) break;

            elements.add(value);

            if (node.conLast) {
                condition = res.register(visit(conditionNode, context));
                if (res.shouldReturn()) return res;

                if (!((Bool) condition.bool()).trueValue()) break;
            }
        }

        return res.success(node.retnull ? new Null() : new PList(new ArrayList<>(elements)).set_context(context)
                .set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_NumberNode(NumberNode node, Context context) {
        return new RTResult().success(new Num(node.val).set_context(context)
                .set_pos(node.pos_start, node.pos_end));
    }

    public RTResult visit_MethDefNode(MethDefNode node, Context context) {
        RTResult res = new RTResult();

        String funcName = (String) node.var_name_tok.value;
        Token nameTok = node.var_name_tok;
        Node bodyNode = node.body_node;
        var argNT = gatherArgs(node.arg_name_toks, node.arg_type_toks);

        var dfts = getDefaults(node.defaults, context);
        res.register(dfts.a);
        if (res.error != null) return res;

        CMethod methValue = new CMethod(funcName, nameTok, context, bodyNode, argNT.a, argNT.b, node.bin, node.async,
                node.autoreturn, node.returnType, dfts.b, node.defaultCount);

        context.symbolTable.define(funcName, methValue);
        return res.success(methValue);
    }

    public RTResult visit_ClassDefNode(ClassDefNode node, Context context) {
        RTResult res = new RTResult();

        String name = (String) node.class_name_tok.value;
        Context classContext = new Context("<"+name+"-context>", context, node.pos_start);
        classContext.symbolTable = new SymbolTable(context.symbolTable);
        Token[] attributes = new Token[0];
        attributes = node.attribute_name_toks.toArray(attributes);
        List<String> argNames = new ArrayList<>();
        List<String> argTypes = new ArrayList<>();
        int size = node.arg_name_toks.size();
        for (int i = 0; i < size; i++) {
            argNames.add((String) node.arg_name_toks.get(i).value);
            argTypes.add((String) node.arg_type_toks.get(i).value);
        }

        var dfts = getDefaults(node.defaults, context);
        res.register(dfts.a);
        if (res.error != null) return res;

        CMethod make = (CMethod) new CMethod("<make>", null, classContext, node.make_node, argNames,
                argTypes, false, false, false, "any", dfts.b, node.defaultCount)
                .set_pos(node.pos_start, node.pos_end);
        size = node.methods.size();
        CMethod[] methods = new CMethod[size];
        for (int i = 0; i < size; i++) {
            Object mthd = res.register(visit_MethDefNode(node.methods.get(i), classContext));
            if (res.shouldReturn()) return res;
            methods[i] = (CMethod) mthd;
        }

        Obj classValue = new ClassPlate(name, attributes, make, methods)
                .set_context(classContext).set_pos(node.pos_start, node.pos_end);
        context.symbolTable.define(name, classValue);

        return res.success(classValue);
    }

    public Pair< List<String>, List<String> > gatherArgs(List<Token> argNameToks, List<Token> argTypeToks) {
        List<String> argNames = new ArrayList<>();
        List<String> argTypes = new ArrayList<>();
        int size = argNameToks.size();
        for (int i = 0; i < size; i++) {
            argNames.add((String) argNameToks.get(i).value);
            argTypes.add((String) argTypeToks.get(i).value);
        }
        return new Pair<>(argNames, argTypes);
    }

    public Pair< RTResult, List<Obj> > getDefaults(List<Node> dfts, Context ctx) {
        RTResult res = new RTResult();
        List<Obj> defaults = new ArrayList<>();
        for (Node n : dfts)
            if (n == null)
                defaults.add(null);
            else {
                Obj val = res.register(visit(n, ctx));
                if (res.error != null) return new Pair<>(res, defaults);
                defaults.add(val);
            }
        return new Pair<>(res, defaults);
    }

    public RTResult visit_FuncDefNode(FuncDefNode node, Context context) {
        RTResult res = new RTResult();

        String funcName = node.var_name_tok != null ? (String) node.var_name_tok.value : null;
        Node bodyNode = node.body_node;
        var argNT = gatherArgs(node.arg_name_toks, node.arg_type_toks);

        var dfts = getDefaults(node.defaults, context);
        res.register(dfts.a);
        if (res.error != null) return res;

        Obj funcValue = new Function(funcName, bodyNode, argNT.a, argNT.b, node.async, node.autoreturn, node.returnType,
                dfts.b, node.defaultCount)
                .set_context(context).set_pos(node.pos_start, node.pos_end);

        if (funcName != null) context.symbolTable.define(funcName, funcValue);

        return res.success(funcValue);
    }

    public RTResult visit_ReturnNode(ReturnNode node, Context context) {
        RTResult res = new RTResult();

        Node ret = node.nodeToReturn;
        Obj value;
        if (ret != null) {
            value = res.register(visit(ret, context));
            if (res.shouldReturn()) return res;
        } else value = new Null();

        return res.sreturn(value);
    }

    public RTResult visit_ContinueNode(ContinueNode node, Context context) { return new RTResult().scontinue(); }

    public RTResult visit_PassNode(PassNode node, Context context) { return new RTResult(); }

    public RTResult visit_BreakNode(BreakNode node, Context context) { return new RTResult().sbreak(); }

    public RTResult visit_CallNode(CallNode node, Context context) {
        RTResult res = new RTResult();
        List<Obj> args = new ArrayList<>();
        Obj valueToCall = res.register(visit(node.nodeToCall, context));
        if (res.shouldReturn()) return res;
        valueToCall = valueToCall.function();
        if (valueToCall.jptype == Constants.JPType.CMethod)
            valueToCall = valueToCall.copy().set_pos(node.pos_start, node.pos_end);
        else
            valueToCall = valueToCall.copy().set_pos(node.pos_start, node.pos_end).set_context(context);
        int size = node.argNodes.size();
        for (int i = 0; i < size; i++) {
            Obj obj = res.register(visit(node.argNodes.get(i), context));
            args.add(obj);
            if (res.shouldReturn()) return res;
        }

        BaseFunction bValueToCall;
        ClassPlate cValueToCall;
        Obj retValue;
        if (valueToCall.jptype == Constants.JPType.ClassPlate) {
            cValueToCall = (ClassPlate) valueToCall;
            retValue = res.register(cValueToCall.execute(args, this));
        } else {
            bValueToCall = (BaseFunction) valueToCall.copy().set_pos(node.pos_start, node.pos_end);
            Cache cache;
            if (bValueToCall.jptype == Constants.JPType.Library)
                cache = null;
            else
                cache = (Cache) memo.get(bValueToCall.name, args.toArray(new Obj[0]));

            if (context.memoize && (cache != null)) retValue = (Obj) cache.result;
            else {
                if (bValueToCall.isAsync()) {
                    Thread thread = new Thread(() -> bValueToCall.execute(args, this));
                    thread.start();
                    return res.success(new Null().set_pos(node.pos_start, node.pos_end).set_context(context));
                }
                retValue = res.register(bValueToCall.execute(args, this));
                if (context.memoize) memo.add(new Cache(bValueToCall.name, args.toArray(new Obj[0]), retValue));
            }
        }
        if (res.shouldReturn()) return res;
        return res.success(retValue.copy().set_pos(node.pos_start, node.pos_end).set_context(context));
    }

    public RTResult visit_QueryNode(QueryNode node, Context context) {
        RTResult res = new RTResult();

        Obj conditionValue, exprValue;
        int size = node.cases.size();
        for (int i = 0; i < size; i++) {
            Case c = node.cases.get(i);
            conditionValue = res.register(visit(c.condition, context));
            if (res.shouldReturn()) return res;
            Obj bx = conditionValue.bool();
            if (bx.jptype != Constants.JPType.Boolean) return res.failure(new RTError(
                    node.pos_start, node.pos_end,
                    "Conditional must be a boolean!",
                    context
            ));
            Bool b = (Bool) bx;
            if (b.trueValue()) {
                exprValue = res.register(visit(c.statements, context));
                if (res.shouldReturn()) return res;
                return res.success(c.x ? new Null() : exprValue);
            }
        }

        if (node.else_case != null) {
            Obj elseValue = res.register(visit(node.else_case.statements, context));
            if (res.shouldReturn()) return res;
            return res.success(node.else_case.x ? new Null() : elseValue);
        }

        return res.success(new Null());
    }

    public RTResult visit_BooleanNode(BooleanNode node, Context context) {
        return new RTResult().success(new Bool(node.val).set_context(context)
                .set_pos(node.pos_start, node.pos_end));
    }

    public static RTResult getThis(Object val, Context context, Position pos_start, Position pos_end) {
            while (context.displayName.hashCode() != val.hashCode()) {
                if (context.parent == null) return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        "Invalid 'this'",
                        context
                ));
                context = context.parent;
            } return new RTResult().success(new ClassInstance(context).copy().set_pos(pos_start, pos_end)
                    .set_context(context));
    }

    public RTResult visit_VarAccessNode(VarAccessNode node, Context context) {
        RTResult res = new RTResult();

        String varName = (String) node.var_name_tok.value;
        if (context.symbolTable.isDyn(varName)) {
            Node value = context.symbolTable.getDyn(varName);
            Obj ret = res.register(visit(value, context));
            if (res.shouldReturn()) return res;
            return res.success(ret);
        }

        Object value = context.symbolTable.get(varName);

        if (value == null) return res.failure(new RTError(
                node.pos_start, node.pos_end,
                "'" + varName + "' is not defined",
                context
        ));
        else if (value instanceof String)
            return getThis(value, context, node.pos_start, node.pos_end);
        Obj val = ((Obj) value).set_pos(node.pos_start, node.pos_end).set_context(context).copy();
        return res.success(val);
    }

    public RTResult visit_VarAssignNode(VarAssignNode node, Context context) {
        RTResult res = new RTResult();

        String varName = (String) node.var_name_tok.value;
        Obj value = res.register(visit(node.value_node, context));
        if (res.shouldReturn()) return res;

        String error;
        if (node.defining)
            error = context.symbolTable.define(varName, value, node.locked, node.type);
        else
            error = context.symbolTable.set(varName, value, node.locked);
        if (error != null) return res.failure(new RTError(
                node.pos_start, node.pos_end,
                error,
                context
        ));

        return res.success(value);
    }

    public RTResult visit_DynAssignNode(DynAssignNode node, Context context) {
        RTResult res = new RTResult();

        String varName = (String) node.var_name_tok.value;

        context.symbolTable.setDyn(varName, node.value_node);

        return res.success(new Null());
    }

    public RTResult visit_AttrAssignNode(AttrAssignNode node, Context context) {
        RTResult res = new RTResult();

        String varName = (String) node.var_name_tok.value;
        Obj value = res.register(visit(node.value_node, context));
        if (res.shouldReturn()) return res;

        context.symbolTable.setattr(varName, value);
        return res.success(value);

    }

    public RTResult getImprt(String path, String fn, Context context, Position pos_start, Position pos_end)
            throws IOException {
        Pair<ClassInstance, Error> i = Shell.imprt(fn, Files.readString(Path.of(path)), context, pos_start);
        ClassInstance imp = i.a;
        Error error = i.b;
        if (error != null) return new RTResult().failure(error);
        imp.set_pos(pos_start, pos_end).set_context(context);
        return new RTResult().success(imp);
    }

    public RTResult visit_ImportNode(ImportNode node, Context context) throws IOException {
        String fn = (String) node.file_name_tok.value;
        String file_name = System.getProperty("user.dir") + "\\" + fn + ".devp";
        String modPath = Shell.root + "\\modules\\" + fn;
        String modFilePath = modPath + "\\" + fn + ".devp";
        var mkdirs = new File(Shell.root + "\\modules").mkdirs();
        ClassInstance imp = null;
        RTResult res = new RTResult();
        if (Constants.LIBRARIES.containsKey(fn)) imp = (ClassInstance) new ClassInstance(Constants.LIBRARIES.get(fn))
                .set_pos(node.pos_start, node.pos_end).set_context(context);
        else {
            if (Files.exists(Paths.get(modPath)))
                imp = (ClassInstance) res.register(getImprt(modFilePath, fn, context, node.pos_start,
                        node.pos_end));
            else if (Files.exists(Paths.get(file_name)))
                imp = (ClassInstance) res.register(getImprt(file_name, fn, context, node.pos_start,
                        node.pos_end));
            if (res.error != null) return res;
        }
        if (imp == null) return res.failure(new RTError(
                node.pos_start, node.pos_end,
                "Module does not exist!",
                context
        ));
        context.symbolTable.define(fn, imp);
        return res.success(new Null());
    }

    public RTResult visit_AttrAccessNode(AttrAccessNode node, Context context) {
        RTResult res = new RTResult();

        String varName = (String) node.var_name_tok.value;
        Obj value = (Obj) context.symbolTable.getattr(varName);

        if (value == null) return res.failure(new RTError(
                node.pos_start, node.pos_end,
                "'" + varName + "' is not defined",
                context
        ));

        value = value.copy().set_pos(node.pos_start, node.pos_end).set_context(context);
        return res.success(value);
    }

    public RTResult visit_EnumNode(EnumNode node, Context context) {
        Map<String, EnumJChild> children = new HashMap<>();
        String name = (String) node.tok.value;
        int size = node.children.size();

        for (int i = 0; i < size; i++) {
            children.put(
                    (String) node.children.get(i).value,
                    new EnumJChild(i)
            );
        }

        EnumJ e = new EnumJ(name, children);

        context.symbolTable.define(name, e);

        return new RTResult().success(e);
    }

    public RTResult visit_ClaccessNode(ClaccessNode node, Context context) {
        RTResult res = new RTResult();

        Obj var = res.register(visit(node.class_tok, context));

        if (res.error != null) return res;
        if (var.jptype != Constants.JPType.ClassInstance && var.jptype != Constants.JPType.Enum) return res.failure(new RTError(
                node.pos_start, node.pos_end,
                "Expected class instance or enum",
                context
        ));

        if (var.jptype == Constants.JPType.Enum) {
            EnumJChild child = ((EnumJ) var).getChild((String) node.attr_name_tok.value);
            if (child == null)
                return res.failure(new RTError(
                        node.pos_start.copy(), node.pos_end.copy(),
                        "Enum child is undefined!",
                        context
                ));
            return res.success(child.
                                set_context(context).set_pos(node.pos_start, node.pos_end));
        }

        Object val = var.getattr(OP.ACCESS, new Str((String) node.attr_name_tok.value)
                                                .set_pos(node.pos_start, node.pos_end)
                                                .set_context(context));
        if (val instanceof String)
            return getThis(val, context, node.pos_start, node.pos_end);
        else if (val instanceof RTError) return new RTResult().failure((RTError) val);
        return res.success(((Obj)val).set_context(((ClassInstance)var).value));
    }

    public boolean complexEquals(String a, String b) {
        return a.length() == b.length() && a.equals(b);
    }

    public RTResult visit_BinOpNode(BinOpNode node, Context context) {
        RTResult res = new RTResult();
        Pair<Obj, RTError> ret;

        Obj left = res.register(visit(node.left_node, context));
        if (res.shouldReturn()) return res;
        Obj right = res.register(visit(node.right_node, context));
        if (res.shouldReturn()) return res;

        OP op = switch (node.op_tok.type) {
            case PLUS    -> OP.ADD;
            case MINUS   -> OP.SUB;
            case MUL     -> OP.MUL;
            case DIV     -> OP.DIV;
            case POWER   -> OP.FASTPOW;
            case EE      -> OP.EQ;
            case NE      -> OP.NE;
            case LT      -> OP.LT;
            case LTE     -> OP.LTE;
            case AND     -> OP.INCLUDING;
            case OR      -> OP.ALSO;
            case MOD     -> OP.MOD;
            case DOT     -> OP.GET;
            case LSQUARE -> OP.BRACKET;
            default      -> null;
        };

        if (node.op_tok.type == TT.GT || node.op_tok.type == TT.GTE) {
            ret = (Pair<Obj, RTError>) right.getattr(node.op_tok.type.hashCode() == TT.GT.hashCode() ? OP.LT : OP.LTE, left);
        } else ret = (Pair<Obj, RTError>) left.getattr(op, right);
        if (ret.b != null) return res.failure(ret.b);
        return res.success((ret.a).set_pos(node.pos_start, node.pos_end).set_context(context));
    }

    public RTResult visit_UnaryOpNode(UnaryOpNode node, Context context) {
        RTResult res = new RTResult();

        Obj number = res.register(visit(node.node, context));
        if (res.shouldReturn()) return res;

        TT opTokType = node.op_tok.type;
        Pair<Obj, RTError> ret = switch (opTokType) {
            case MINUS -> (Pair<Obj, RTError>) number.getattr(OP.MUL, new Num(-1.0));
            case INCR, DECR -> (Pair<Obj, RTError>) number.getattr(OP.ADD,
                    new Num(opTokType.hashCode() == TT.INCR.hashCode() ? 1.0 : -1.0));
            case NOT -> (Pair<Obj, RTError>) number.getattr(OP.INVERT);
            default -> new Pair<>(number, null);
        };
        if (ret.b != null)
            return res.failure(ret.b);
        number = ret.a;
        return res.success(number.set_pos(node.pos_start, node.pos_end).set_context(context));
    }

    public RTResult visit_UseNode(UseNode node, Context context) {
        Token useToken = node.useToken;
        switch ((String) useToken.value) {
            case "memoize" ->
                context.doMemoize();
            case "func" -> {
                if (!main) break;

                if (node.args.size() < 1) return new RTResult().failure(new RTError(
                        node.pos_start, node.pos_end,
                        "Expected function name",
                        context
                ));

                fnFinish = (String) node.args.get(0).value;
            }
            case "object" -> {
                if (!main) break;

                if (node.args.size() < 1) return new RTResult().failure(new RTError(
                        node.pos_start, node.pos_end,
                        "Expected object name",
                        context
                ));

                clFinish = (String) node.args.get(0).value;
            }
        }
        return new RTResult().success(new Null());
    }

}
