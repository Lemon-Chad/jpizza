package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Executables.CMethod;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.List;

public class MethDefNode extends Node {
    public Token var_name_tok;
    public List<Token> arg_name_toks;
    public Node body_node;
    public boolean autoreturn;
    public boolean async;
    public boolean bin;
    public List<Token> arg_type_toks;
    public String returnType;
    public List<Node> defaults;
    public int defaultCount;

    public MethDefNode(Token var_name_tok, List<Token> arg_name_toks, List<Token> arg_type_toks, Node body_node,
                       boolean autoreturn, boolean bin, boolean async, String returnType, List<Node> defaults,
                       int defaultCount) {
        this.var_name_tok = var_name_tok;
        this.async = async;
        this.arg_name_toks = arg_name_toks;
        this.arg_type_toks = arg_type_toks;
        this.body_node = body_node;
        this.autoreturn = autoreturn;
        this.bin = bin;
        this.returnType = returnType;
        this.defaults = defaults;
        this.defaultCount = defaultCount;

        pos_start = var_name_tok.pos_start;
        pos_end = body_node.pos_end;
        jptype = Constants.JPType.MethDef;
    }

    public RTResult visit(Interpreter inter, Context context) {
        RTResult res = new RTResult();

        String funcName = (String) var_name_tok.value;
        Token nameTok = var_name_tok;
        Node bodyNode = body_node;
        var argNT = inter.gatherArgs(arg_name_toks, arg_type_toks);

        var dfts = inter.getDefaults(defaults, context);
        res.register(dfts.a);
        if (res.error != null) return res;

        CMethod methValue = new CMethod(funcName, nameTok, context, bodyNode, argNT.a, argNT.b, bin, async,
                autoreturn, returnType, dfts.b, defaultCount);

        context.symbolTable.define(funcName, methValue);
        return res.success(methValue);
    }
}
