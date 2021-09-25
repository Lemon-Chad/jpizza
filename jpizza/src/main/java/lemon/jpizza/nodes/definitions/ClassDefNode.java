package lemon.jpizza.nodes.definitions;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.executables.CMethod;
import lemon.jpizza.objects.executables.ClassPlate;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.List;

public class ClassDefNode extends Node {
    public Token class_name_tok;
    public List<AttrDeclareNode> attributes;
    public List<Token> arg_name_toks;
    public Node make_node;
    public List<MethDefNode> methods;
    public List<Token> arg_type_toks;
    public List<Token> generic_toks;
    public List<Node> defaults;
    public int defaultCount;
    public Token parentToken;
    public String argname, kwargname;

    public ClassDefNode(Token class_name_tok, List<AttrDeclareNode> attributes, List<Token> arg_name_toks,
                        List<Token> arg_type_toks, Node make_node, List<MethDefNode> methods, Position pos_end,
                        List<Node> defaults, int defaultCount, Token pTK, List<Token> generics, String argname,
                        String kwargname) {
        this.class_name_tok = class_name_tok;
        this.defaultCount = defaultCount;
        this.generic_toks = generics;
        this.defaults = defaults;
        this.attributes = attributes;
        this.make_node = make_node;
        this.arg_name_toks = arg_name_toks;
        this.arg_type_toks = arg_type_toks;
        this.methods = methods;
        this.pos_end = pos_end;
        this.pos_start = class_name_tok.pos_start;
        this.argname = argname;
        this.kwargname = kwargname;

        parentToken = pTK;
        jptype = Constants.JPType.ClassDef;
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult visit(Interpreter inter, Context context) {
            RTResult res = new RTResult();

            String name = (String) class_name_tok.value;
            Context classContext = new Context("<"+name+"-context>", context, pos_start);
            classContext.symbolTable = new SymbolTable(context.symbolTable);

            AttrDeclareNode[] attributes = new AttrDeclareNode[this.attributes.size()];
            for (int i = 0; i < this.attributes.size(); i++) {
                res.register(this.attributes.get(i).visit(inter, context));
                if (res.error != null) return res;
                attributes[i] = this.attributes.get(i);
            }

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
                    argTypes, false, false, false, "null", dfts.b, defaultCount,
                    generic_toks, false, false, argname, kwargname)
                    .set_pos(pos_start, pos_end);
            size = methods.size();
            CMethod[] methods = new CMethod[size];
            for (int i = 0; i < size; i++) {
                Object mthd = res.register(this.methods.get(i).visit(inter, classContext));
                if (res.shouldReturn()) return res;
                methods[i] = (CMethod) mthd;
            }

            ClassPlate parent = null;
            if (parentToken != null) {
                Obj p = (Obj) context.symbolTable.get(parentToken.value.toString());
                if (p == null || p.jptype != Constants.JPType.ClassPlate) return res.failure(RTError.Scope(
                        parentToken.pos_start, parentToken.pos_end,
                        "Parent does not exist",
                        context
                ));
                parent = (ClassPlate) p;
            }

            Obj classValue = new ClassPlate(name, attributes, make, methods, parent)
                    .set_context(classContext).set_pos(pos_start, pos_end);
            context.symbolTable.define(name, classValue);

            return res.success(classValue);
        }
}
