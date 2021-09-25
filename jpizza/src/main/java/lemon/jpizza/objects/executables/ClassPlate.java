package lemon.jpizza.objects.executables;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.contextuals.SymbolTable;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.nodes.definitions.AttrDeclareNode;
import lemon.jpizza.nodes.values.ListNode;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.*;
import lemon.jpizza.objects.Value;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Token;

import java.util.*;

public class ClassPlate extends Value {
    final String name;
    public final CMethod make;
    final CMethod[] methods;
    final AttrDeclareNode[] attributes;
    final Map<String, CMethod> staticMap;
    final Map<String, AttrDeclareNode> staticAttrs;

    final ClassPlate parent;
    final List<String> methodNames;

    public ClassPlate(String name, AttrDeclareNode[] attributes, CMethod make, CMethod[] methods, ClassPlate parent) {
        this.name = name;
        this.parent = parent;
        this.make = make;
        this.attributes = attributes;
        this.methods = methods;
        this.value = null;

        staticMap = new HashMap<>();
        staticAttrs = new HashMap<>();
        methodNames = new ArrayList<>();
        CMethod meth;
        for (int i = 0; i < methods.length; i++) {
            meth = methods[i];
            methodNames.add(meth.name);
            if (meth.isstatic)
                staticMap.put(meth.name, meth);
        }

        for (int i = 0; i < attributes.length; i++)
            if (attributes[i].isstatic) staticAttrs.put(attributes[i].name, attributes[i]);

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

    public AttrDeclareNode[] getAttributes() {
        List<String> attrNames = new ArrayList<>();
        List<AttrDeclareNode> attributes = new ArrayList<>();

        for (AttrDeclareNode attr : this.attributes) {
            attributes.add(attr);
            attrNames.add(attr.name);
        }

        if (parent != null)
            for (AttrDeclareNode attr : parent.getAttributes())
                if (!attrNames.contains(attr.name))
                    attributes.add(attr);

        return attributes.toArray(new AttrDeclareNode[0]);
    }

    // Methods

    public RTResult access(Obj o) {
        if (o.jptype != Constants.JPType.String) return new RTResult().failure(RTError.Type(
                o.get_start(), o.get_end(),
                "Expected String",
                o.get_ctx()
        ));
        String other = o.string;
        CMethod c = staticMap.get(other);
        AttrDeclareNode x = staticAttrs.get(other);
        if (c != null && !c.isprivate)
            return new RTResult().success(c);
        else if (x != null && !x.isprivate)
            return new RTResult().success(x.value);
        else return new RTResult().failure(RTError.Scope(
                    o.get_start(), o.get_end(),
                    "(Public) Static attribute does not exist",
                    o.get_ctx()
            ));
    }

    public RTResult execute(List<Obj> args, List<Token> generics, Map<String, Obj> kwargs, Interpreter parent) {
        RTResult res = new RTResult();

        Context classContext = new Context(name, context, pos_start);
        classContext.symbolTable = new SymbolTable(context.symbolTable);

        AttrDeclareNode[] attributes = getAttributes();
        int length = attributes.length;
        AttrDeclareNode curr;
        for (int i = 0; i < length; i++) {
            curr = attributes[i];
            if (curr.value != null)
                classContext.symbolTable.declareattr(curr.attrToken, classContext, curr.value);
            else
                classContext.symbolTable.declareattr(curr.attrToken, classContext);

            if (curr.isprivate)
                classContext.symbolTable.makeprivate(curr.name);

            if (curr.type != null)
                classContext.symbolTable.setattrtype(curr.name, curr.type);
        }

        CMethod make = (CMethod) getMake().copy();
        make.set_context(classContext).set_pos(pos_start, pos_end);

        res.register(make.execute(args, generics, kwargs, parent));
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
                classContext.symbolTable.declareattr(method.nameTok, classContext, method);
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
