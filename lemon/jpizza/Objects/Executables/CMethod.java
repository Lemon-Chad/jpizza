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

public class CMethod extends Function {
    boolean bin;
    Token nameTok;

    public CMethod(String name, Token nameTok, Context context, Node bodyNode, List<String> argNames,
                   List<String> argTypes, boolean bin, boolean async, boolean autoreturn) {
        super(name, bodyNode, argNames, argTypes, async, autoreturn);
        this.nameTok = nameTok;
        this.context = new Context(this.name, context, this.pos_start);
        this.context.symbolTable = new SymbolTable(context.symbolTable);
        this.bin = bin;
        jptype = Constants.JPType.CMethod;
    }

    // Functions

    // Methods

    @Override
    public RTResult execute(List<Obj> args, Interpreter parent) {
        return super.execute(args, parent);
    }


    // Conversions

    // Default

    public Obj copy() { return new CMethod(name, nameTok, context, bodyNode, argNames, argTypes, bin, async, autoreturn)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("<class-method>").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "<"+context.displayName+"-method-"+name+">"; }

}
