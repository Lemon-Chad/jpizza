package lemon.jpizza.objects.executables;

import lemon.jpizza.JPType;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.List;
import java.util.Map;

public class CMethod extends Function {
    final boolean bin;
    final boolean isstatic;
    public final boolean isprivate;
    final Token nameTok;

    public CMethod(String name, Token nameTok, Context context, Node bodyNode, List<String> argNames,
                   List<List<String>> argTypes, boolean bin, boolean async, boolean autoreturn, List<String> returnType,
                   List<Obj> defaults, int defaultCount, List<Token> generics, boolean stat, boolean priv,
                   String argname, String kwargname) {
        super(name, bodyNode, argNames, argTypes, async, autoreturn, returnType, defaults, defaultCount, generics);
        this.nameTok = nameTok;
        this.context = new Context(this.name, context, this.pos_start);
        this.context.symbolTable = new SymbolTable(context.symbolTable);
        this.bin = bin;
        this.isstatic = stat;
        this.isprivate = priv;
        this.argname = argname;
        this.kwargname = kwargname;
        jptype = JPType.CMethod;
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
            bin, async, autoreturn, returnType, defaults, defaultCount, generics, isstatic, isprivate, argname,
            kwargname).setCatch(catcher)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("<class-method>").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "<"+context.displayName+"-method-"+name+">"; }

}
