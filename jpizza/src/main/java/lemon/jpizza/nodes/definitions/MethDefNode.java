package lemon.jpizza.nodes.definitions;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.executables.CMethod;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.List;

public class MethDefNode extends Node {
    public final Token var_name_tok;
    public final List<Token> arg_name_toks;
    public final Node body_node;
    public final boolean autoreturn;
    public final boolean async;
    public final boolean bin;
    public final List<Token> arg_type_toks;
    public final List<Token> generic_toks;
    public final List<String> returnType;
    public final List<Node> defaults;
    public final int defaultCount;
    public boolean catcher = false;
    public final boolean stat;
    public final boolean priv;
    public final String argname;
    public final String kwargname;

    public MethDefNode(Token var_name_tok, List<Token> arg_name_toks, List<Token> arg_type_toks, Node body_node,
                       boolean autoreturn, boolean bin, boolean async, List<String> returnType, List<Node> defaults,
                       int defaultCount, List<Token> generics, boolean stat, boolean priv, String argname,
                       String kwargname) {
        this.var_name_tok = var_name_tok;
        this.stat = stat;
        this.priv = priv;
        this.generic_toks = generics;
        this.async = async;
        this.arg_name_toks = arg_name_toks;
        this.arg_type_toks = arg_type_toks;
        this.body_node = body_node;
        this.autoreturn = autoreturn;
        this.bin = bin;
        this.returnType = returnType;
        this.defaults = defaults;
        this.defaultCount = defaultCount;
        this.argname = argname;
        this.kwargname = kwargname;

        pos_start = var_name_tok.pos_start;
        pos_end = body_node.pos_end;
        jptype = JPType.MethDef;
    }

    public MethDefNode setCatcher(boolean c) {
        this.catcher = c;
        return this;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String funcName = (String) var_name_tok.value;
        var argNT = inter.gatherArgs(arg_name_toks, arg_type_toks, context);

        var dfts = inter.getDefaults(defaults, context);
        res.register(dfts.a);
        if (res.error != null) return res;

        CMethod methValue = new CMethod(funcName, var_name_tok, context, body_node, argNT.a, argNT.b, bin, async,
                autoreturn, returnType, dfts.b, defaultCount, generic_toks, stat, priv, argname, kwargname);

        context.symbolTable.define(funcName, methValue);
        return res.success(methValue.setCatch(catcher));
    }

    public FuncDefNode asFuncDef() {
        return new FuncDefNode(var_name_tok, arg_name_toks, arg_type_toks, body_node, autoreturn, async,
                returnType, defaults, defaultCount, generic_toks, argname, kwargname).setCatcher(catcher);
    }

    @Override
    public Node optimize() {
        Node body = body_node.optimize();
        List<Node> optDefaults = new ArrayList<>();
        for (Node n : defaults) {
            optDefaults.add(n.optimize());
        }
        return new MethDefNode(var_name_tok, arg_name_toks, arg_type_toks, body, autoreturn, bin, async,
                returnType, optDefaults, defaultCount, generic_toks, stat, priv, argname, kwargname)
                .setCatcher(catcher);
    }

    @Override
    public List<Node> getChildren() {
        return List.of(body_node);
    }

    @Override
    public String visualize() {
        String stc = stat ? "static " : "";
        String prv = priv ? "private " : "public ";
        return prv + stc + "mthd " + var_name_tok.value + "(" + arg_name_toks.stream().map(x -> x.value.toString()).reduce("", (a, b) -> a + b + ",") + ")";
    }
}
