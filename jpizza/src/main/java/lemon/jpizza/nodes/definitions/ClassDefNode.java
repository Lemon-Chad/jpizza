package lemon.jpizza.nodes.definitions;

import lemon.jpizza.JPType;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.Position;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.List;

public class ClassDefNode extends Node {
    public final Token class_name_tok;
    public final List<AttrDeclareNode> attributes;
    public final List<Token> arg_name_toks;
    public final Node make_node;
    public final List<MethDefNode> methods;
    public final List<Token> arg_type_toks;
    public final List<Token> generic_toks;
    public final List<Node> defaults;
    public final int defaultCount;
    public final Token parentToken;
    public final String argname;
    public final String kwargname;

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
        jptype = JPType.ClassDef;
    }

    @Override
    public Node optimize() {
        List<AttrDeclareNode> optimizedAttributes = new ArrayList<>();
        for (AttrDeclareNode attr : attributes) {
            optimizedAttributes.add((AttrDeclareNode) attr.optimize());
        }
        Node optimizedMake = make_node.optimize();
        List<MethDefNode> optimizedMethods = new ArrayList<>();
        for (MethDefNode method : methods) {
            optimizedMethods.add((MethDefNode) method.optimize());
        }
        List<Node> optimizedDefaults = new ArrayList<>();
        for (Node default_ : defaults) {
            optimizedDefaults.add(default_.optimize());
        }
        return new ClassDefNode(class_name_tok, optimizedAttributes, arg_name_toks, arg_type_toks,
                optimizedMake, optimizedMethods, pos_end, optimizedDefaults, defaults.size(),
                parentToken, generic_toks, argname, kwargname);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> children = new ArrayList<>(attributes);
        children.add(make_node);
        children.addAll(methods);
        return children;
    }

    @Override
    public String visualize() {
        return "class " + class_name_tok.value;
    }
}
