package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClassPlate extends Value {
    String name;
    CMethod make;
    CMethod[] methods;
    Token[] attributes;

    public ClassPlate(String name, Token[] attributes, CMethod make, CMethod[] methods) {
        this.name = name;
        this.make = make;
        this.attributes = attributes;
        this.methods = methods;
        this.value = null;

        set_pos(); set_context();
    }

    // Functions

    public CMethod[] copyMethods() {
        int length = methods.length;
        CMethod[] newMethods = new CMethod[length];
        for (int i = 0; i < length; i++) newMethods[i] = (CMethod) methods[i].copy();
        return newMethods;
    }

    // Methods

    public RTResult execute(List<Obj> args, Interpreter parent) {
        RTResult res = new RTResult();
        Context classContext = new Context(name, context, pos_start);
        classContext.symbolTable = new SymbolTable(context.symbolTable);
        classContext.symbolTable.set("this", name);
        int length = attributes.length;
        for (int i = 0; i < length; i++) classContext.symbolTable.declareattr(attributes[i], classContext);
        CMethod make = (CMethod) this.make.copy();
        make.set_context(classContext);
        res.register(make.execute(args, parent));
        if (res.error != null) return res;
        CMethod[] methodCopies = copyMethods();
        length = methodCopies.length;
        for (int i = 0; i < length; i++) {
            CMethod method = methodCopies[i];
            if (method.bin) classContext.symbolTable.setbin(method.name, method);
            else {
                classContext.symbolTable.declareattr(method.nameTok, classContext);
                classContext.symbolTable.setattr((String) method.nameTok.value, method);
            } method.set_context(classContext);
        }
        return res.success(new ClassInstance(classContext).set_context(context).set_pos(pos_start, pos_end));
    }

    // Conversions

    public Value astring() { return new Str(name).set_context(context).set_pos(pos_start, pos_end); }
    public Value number() { return new Num(0).set_context(context).set_pos(pos_start, pos_end); }
    public Value dictionary() { return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end); }
    public Value alist() { return new PList(new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end); }
    public Value bool() { return new Bool(true).set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public boolean isAsync() { return false; }
    public Value copy() { return new ClassPlate(name, attributes, (CMethod) make.copy(), copyMethods())
            .set_context(context).set_pos(pos_start, pos_end); }
    public Value type() { return astring(); }
    public String toString() { return "<recipe-"+name+">"; }

}
