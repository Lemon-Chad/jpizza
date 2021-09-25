package lemon.jpizza.nodes.definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.executables.Function;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.List;

public class FuncDefNode extends Node {
    final Token var_name_tok;
    final List<Token> arg_name_toks;
    final Node body_node;
    final boolean autoreturn;
    final boolean async;
    final List<Token> arg_type_toks;
    final List<Token> generic_toks;
    final String returnType;
    final List<Node> defaults;
    final int defaultCount;
    boolean catcher = false;
    final String argname;
    final String kwargname;

    public FuncDefNode(Token var_name_tok, List<Token> arg_name_toks, List<Token> arg_type_toks, Node body_node,
                       boolean autoreturn, boolean async, String returnType, List<Node> defaults, int defaultCount,
                       List<Token> generic_toks, String argname, String kwargname) {
        this.var_name_tok = var_name_tok;
        this.argname = argname;
        this.kwargname = kwargname;
        this.generic_toks = generic_toks;
        this.async = async;
        this.arg_name_toks = arg_name_toks;
        this.arg_type_toks = arg_type_toks;
        this.body_node = body_node;
        this.autoreturn = autoreturn;
        this.returnType = returnType;
        this.defaults = defaults;
        this.defaultCount = defaultCount;

        pos_start = var_name_tok != null ? var_name_tok.pos_start : (
                arg_name_toks != null && arg_name_toks.size() > 0 ? arg_name_toks.get(0).pos_start : body_node.pos_start
                );
        pos_end = body_node.pos_end;
        jptype = Constants.JPType.FuncDef;
    }

    public FuncDefNode setCatcher(boolean c) {
        this.catcher = c;
        return this;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String funcName = var_name_tok != null ? (String) var_name_tok.value : null;
        var argNT = inter.gatherArgs(arg_name_toks, arg_type_toks);

        var dfts = inter.getDefaults(defaults, context);
        res.register(dfts.a);
        if (res.error != null) return res;

        Obj funcValue = new Function(funcName, body_node, argNT.a, argNT.b, async, autoreturn, returnType,
                dfts.b, defaultCount, generic_toks).setCatch(catcher).setIterative(argname).setKwargs(kwargname)
                .set_context(context).set_pos(pos_start, pos_end);

        if (funcName != null) context.symbolTable.define(funcName, funcValue);

        return res.success(funcValue);
    }
}
