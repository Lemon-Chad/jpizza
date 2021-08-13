package lemon.jpizza.Nodes.Definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Executables.CMethod;
import lemon.jpizza.Objects.Executables.ClassPlate;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.List;

public class ClassDefNode extends Node {
    public Token class_name_tok;
    public List<Token> attribute_name_toks;
    public List<Token> arg_name_toks;
    public Node make_node;
    public List<MethDefNode> methods;
    public List<Token> arg_type_toks;
    public List<Node> defaults;
    public int defaultCount;

    public ClassDefNode(Token class_name_tok, List<Token> attribute_name_toks, List<Token> arg_name_toks,
                        List<Token> arg_type_toks, Node make_node, List<MethDefNode> methods, Position pos_end,
                        List<Node> defaults, int defaultCount) {
        this.class_name_tok = class_name_tok;
        this.defaultCount = defaultCount;
        this.defaults = defaults;
        this.attribute_name_toks = attribute_name_toks;
        this.make_node = make_node;
        this.arg_name_toks = arg_name_toks;
        this.arg_type_toks = arg_type_toks;
        this.methods = methods;
        this.pos_end = pos_end;
        this.pos_start = class_name_tok.pos_start;
        jptype = Constants.JPType.ClassDef;
    }

    public RTResult visit(Interpreter inter, Context context) {
            RTResult res = new RTResult();

            String name = (String) class_name_tok.value;
            Context classContext = new Context("<"+name+"-context>", context, pos_start);
            classContext.symbolTable = new SymbolTable(context.symbolTable);
            Token[] attributes = new Token[0];
            attributes = attribute_name_toks.toArray(attributes);
            List<String> argNames = new ArrayList<>();
            List<String> argTypes = new ArrayList<>();
            int size = arg_name_toks.size();
            for (int i = 0; i < size; i++) {
                argNames.add((String) arg_name_toks.get(i).value);
                argTypes.add((String) arg_type_toks.get(i).value);
            }

            var dfts = inter.getDefaults(defaults, context);
            res.register(dfts.a);
            if (res.error != null) return res;

            CMethod make = (CMethod) new CMethod("<make>", null, classContext, make_node, argNames,
                    argTypes, false, false, false, "any", dfts.b, defaultCount)
                    .set_pos(pos_start, pos_end);
            size = methods.size();
            CMethod[] methods = new CMethod[size];
            for (int i = 0; i < size; i++) {
                Object mthd = res.register(this.methods.get(i).visit(inter, classContext));
                if (res.shouldReturn()) return res;
                methods[i] = (CMethod) mthd;
            }

            Obj classValue = new ClassPlate(name, attributes, make, methods)
                    .set_context(classContext).set_pos(pos_start, pos_end);
            context.symbolTable.define(name, classValue);

            return res.success(classValue);
        }
}
