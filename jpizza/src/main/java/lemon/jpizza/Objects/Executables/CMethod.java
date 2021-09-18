package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.List;
import java.util.Map;

public class CMethod extends Function {
    boolean bin;
    boolean isstatic;
    public boolean isprivate;
    Token nameTok;

    public CMethod(String name, Token nameTok, Context context, Node bodyNode, List<String> argNames,
                   List<String> argTypes, boolean bin, boolean async, boolean autoreturn, String returnType,
                   List<Obj> defaults, int defaultCount, List<Token> generics, boolean stat, boolean priv) {
        super(name, bodyNode, argNames, argTypes, async, autoreturn, returnType, defaults, defaultCount, generics);
        this.nameTok = nameTok;
        this.context = new Context(this.name, context, this.pos_start);
        this.context.symbolTable = new SymbolTable(context.symbolTable);
        this.bin = bin;
        this.isstatic = stat;
        this.isprivate = priv;
        jptype = Constants.JPType.CMethod;
    }

    // Functions

    // Methods

    @Override
    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        return super.execute(args, generics, kwargs, parent);
    }


    // Conversions

    // Default

    public Obj copy() { return new CMethod(name, nameTok, context, bodyNode, argNames, argTypes,
            bin, async, autoreturn, returnType, defaults, defaultCount, generics, isstatic, isprivate).setCatch(catcher)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("<class-method>").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "<"+context.displayName+"-method-"+name+">"; }

}
