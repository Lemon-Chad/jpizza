package lemon.jpizza.Objects.Executables;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Contextuals.SymbolTable;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Nodes.Values.ListNode;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Objects.Value;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Token;

import java.util.*;

public class ClassPlate extends Value {
    String name;
    public CMethod make;
    CMethod[] methods;
    Map<String, CMethod> staticMap;
    Token[] attributes;
    ClassPlate parent;
    List<String> methodNames;

    public ClassPlate(String name, Token[] attributes, CMethod make, CMethod[] methods, ClassPlate parent) {
        this.name = name;
        this.parent = parent;
        this.make = make;
        this.attributes = attributes;
        this.methods = methods;
        this.value = null;

        staticMap = new HashMap<>();
        methodNames = new ArrayList<>();
        CMethod meth;
        for (int i = 0; i < methods.length; i++) {
            meth = methods[i];
            methodNames.add(meth.name);
            if (meth.isstatic)
                staticMap.put(meth.name, meth);
        }

        set_pos(); set_context();
        jptype = Constants.JPType.ClassPlate;
    }

    // Functions

    public CMethod[] copyMethods() {
        CMethod[] methods = getMethods();
        int length = methods.length;
        CMethod[] newMethods = new CMethod[length];
        for (int i = 0; i < length; i++) newMethods[i] = (CMethod) methods[i].copy();
        return newMethods;
    }

    // Functions

    public CMethod[] getMethods() {
        List<CMethod> methods = new ArrayList<>(Arrays.asList(this.methods));

        if (parent != null)
            for (CMethod method : parent.getMethods())
                if (!methodNames.contains(method.name))
                    methods.add(method);

        return methods.toArray(new CMethod[0]);
    }

    public CMethod getMake() {
        CMethod make = this.make;
        if (this.parent != null && ((ListNode) make.bodyNode).elements.size() == 0)
            make = this.parent.make;
        return make;
    }

    public Token[] getAttributes() {
        List<String> attrNames = new ArrayList<>();
        List<Token> attributes = new ArrayList<>();

        for (Token attr : this.attributes) {
            attributes.add(attr);
            attrNames.add(attr.value.toString());
        }

        if (parent != null)
            for (Token attr : parent.getAttributes())
                if (!attrNames.contains(attr.value.toString()))
                    attributes.add(attr);

        return attributes.toArray(new Token[0]);
    }

    // Methods

    public RTResult access(Obj o) {
        if (o.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                o.get_start(), o.get_end(),
                "Expected string",
                o.get_ctx()
        ));
        String other = ((Str) o).trueValue();
        CMethod c = staticMap.get(other);
        if (c != null)
            return new RTResult().success(c);
        else return new RTResult().failure(new RTError(
                    o.get_start(), o.get_end(),
                    "Static attribute does not exist",
                    o.get_ctx()
            ));
    }

    public RTResult execute(List<Obj> args, List<Token> generics, Interpreter parent) {
        RTResult res = new RTResult();

        Context classContext = new Context(name, context, pos_start);
        classContext.symbolTable = new SymbolTable(context.symbolTable);

        Token[] attributes = getAttributes();
        int length = attributes.length;
        for (int i = 0; i < length; i++)
            classContext.symbolTable.declareattr(attributes[i], classContext);

        CMethod make = (CMethod) getMake().copy();
        make.set_context(classContext).set_pos(pos_start, pos_end);

        res.register(make.execute(args, generics, parent));
        if (res.error != null) return res;

        CMethod[] methodCopies = copyMethods();
        methodIterate(classContext, methodCopies);

        return res.success(new ClassInstance(classContext).set_context(context).set_pos(pos_start, pos_end));
    }

    private void methodIterate(Context classContext, CMethod[] methodCopies) {
        int length;
        length = methodCopies.length;
        for (int i = 0; i < length; i++) {
            CMethod method = methodCopies[i];

            if (method.bin) classContext.symbolTable.setbin(method.name, method);
            else {
                classContext.symbolTable.declareattr(method.nameTok, classContext);
                classContext.symbolTable.setattr((String) method.nameTok.value, method);
            } method.set_context(classContext);
        }
    }

    // Conversions

    public Obj astring() { return new Str(name).set_context(context).set_pos(pos_start, pos_end); }
    public Obj number() { return new Num(0).set_context(context).set_pos(pos_start, pos_end); }
    public Obj dictionary() { return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj alist() { return new PList(new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end); }
    public Obj bool() { return new Bool(true).set_context(context).set_pos(pos_start, pos_end); }
    public Obj bytes() { return new Bytes(new byte[0]).set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public boolean isAsync() { return false; }
    public Obj copy() { return new ClassPlate(name, attributes, (CMethod) make.copy(), copyMethods(), parent)
            .set_context(context).set_pos(pos_start, pos_end); }
    public Obj type() { return new Str("recipe").set_context(context).set_pos(pos_start, pos_end); }
    public String toString() { return "<recipe-"+name+">"; }

}
