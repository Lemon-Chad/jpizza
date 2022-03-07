package lemon.jpizza.compiler;

import lemon.jpizza.Position;
import lemon.jpizza.Shell;
import lemon.jpizza.Token;
import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.types.GenericType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.types.objects.*;
import lemon.jpizza.compiler.values.functions.JFunc;
import lemon.jpizza.generators.Parser;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.definitions.*;
import lemon.jpizza.nodes.expressions.*;
import lemon.jpizza.nodes.operations.BinOpNode;
import lemon.jpizza.nodes.operations.UnaryOpNode;
import lemon.jpizza.nodes.values.*;
import lemon.jpizza.nodes.variables.AttrAccessNode;
import lemon.jpizza.nodes.variables.VarAccessNode;

import java.util.*;

public class TypeLookup {
    final Compiler compiler;
    final Map<String, Type> types;

    public TypeLookup(Compiler compiler) {
        this.compiler = compiler;
        this.types = new HashMap<>();
        // Builtin types
        types.put("int", Types.INT);
        types.put("float", Types.FLOAT);
        types.put("bool", Types.BOOL);
        types.put("String", Types.STRING);
        types.put("list", Types.LIST);
        types.put("dict", Types.DICT);
        types.put("void", Types.VOID);
        types.put("any", Types.ANY);
        types.put("bytearray", Types.BYTES);
        types.put("catcher", Types.RESULT);
    }

    public Type getType(String name) {
        if (types.containsKey(name)) {
            return types.get(name);
        }
        else if (compiler.enclosing != null) {
            return compiler.enclosing.typeHandler.getType(name);
        }
        else {
            return null;
        }
    }

    private class TypeParser {
        final List<String> data;
        int i;
        String currentToken;
        final Position start, end;

        public TypeParser(List<String> data, Position start, Position end) {
            this.data = data;
            this.i = -1;
            this.advance();

            this.start = start;
            this.end = end;
        }

        private void advance() {
            i++;
            if (i >= data.size()) {
                currentToken = null;
            }
            else {
                currentToken = data.get(i);
            }
        }

        private void fail(String reason) {
            compiler.error("TypeParser", reason, start, end);
        }

        private void consume(String token) {
            if (currentToken == null || !currentToken.equals(token)) {
                fail("Expected '" + token + "' but got '" + currentToken + "'");
            }
            advance();
        }

        private Type subType() {
            // Basic: HEADER
            // ex. int, float, String, CoolClass
            // Reference: [TYPE]
            // ex. [int], [float], [String], [CoolClass], [CoolClass<int>]
            // Generic: HEADER ( TYPEA, TYPEB, TYPEC, ... )
            // ex. CoolClass<int, float, String>, CoolClass<CoolClass<int>>
            // Function: ReturnType < T1, T2, ... Tn >
            // ex. int<int, float>
            String header = currentToken;
            Type type = getType(header);
            advance();
            if (header.equals("(")) {
                // Grouping
                type = subType();
                consume(")");
            }
            if (header.equals("[")) {
                // Reference
                Type refType = subType();
                consume("]");
                return new ReferenceType(refType);
            }
            if (type == null) {
                fail(String.format("Unknown type '%s'", header));
            }
            if (type instanceof ClassType) {
                ClassType classType = (ClassType) type;
                List<Type> genericTypes = new ArrayList<>();
                if ("(".equals(currentToken)) {
                    // Generic
                    do {
                        advance();
                        genericTypes.add(subType());
                    } while (",".equals(currentToken));
                    consume(")");
                }
                if (genericTypes.size() != classType.generics.length) {
                    fail(String.format("Expected %d generic types but got %d", classType.constructor.generics.length, genericTypes.size()));
                }
                type = new InstanceType(classType, genericTypes.toArray(new Type[0]));
            }

            if ("<".equals(currentToken)) {
                advance();
                // Function
                boolean varargs = false;
                boolean defaultArgs = false;
                int defaultCount = 0;
                List<Type> argTypes = new ArrayList<>();
                while (currentToken != null && !">".equals(currentToken)) {
                    if (currentToken.equals("*")) {
                        if (defaultArgs) {
                            fail("Varargs cannot mix with default arguments");
                        }
                        varargs = true;
                        advance();
                        break;
                    }
                    argTypes.add(subType());

                    if ("?".equals(currentToken)) {
                        defaultArgs = true;
                        advance();
                        defaultCount++;
                    } else if (defaultArgs) {
                        fail("Default arguments must be last");
                    }

                    if (",".equals(currentToken)) {
                        advance();
                    }
                }
                consume(">");
                type = new FuncType(type, argTypes.toArray(new Type[0]), new GenericType[0], varargs, defaultCount);
            }

            return type;
        }

        public Type resolve() {
            Type type = subType();
            if (currentToken != null) {
                fail("Unexpected token '" + currentToken + "'");
            }
            return type;
        }
    }

    public Type resolve(Token type) {
        return resolve((List<String>) type.value, type.pos_start, type.pos_end);
    }

    public Type resolve(List<String> type, Position start, Position end) {
        return new TypeParser(type, start, end).resolve();
    }

    public Type resolve(Node statement) {
        switch (statement.jptype) {
            case Use:
            case Null:
            case Pass:
            case Break:
            case Continue:
            case Body:
            case Extend:
            case Destruct:
            case DynAssign:
            case Let:
            case Drop:
            case Throw:
            case Assert:
            case Switch:
            case Pattern:
                return Types.VOID;

            case Cast:
                return resolve(((CastNode) statement).type);

            case BinOp:
                return resolve((BinOpNode) statement);
            case UnaryOp:
                return resolve((UnaryOpNode) statement);

            case Import:
                return resolve((ImportNode) statement);

            case Decorator:
                return resolve((DecoratorNode) statement);
            case FuncDef:
                return resolve((FuncDefNode) statement);
            case Call:
                return resolve((CallNode) statement);
            case Return:
                return resolve((ReturnNode) statement);
            case Spread:
                return Types.SPREAD;

            case Number: {
                NumberNode node = (NumberNode) statement;
                return (long) node.val == node.val ? Types.INT : Types.FLOAT;
            }
            case String:
                return Types.STRING;
            case Boolean:
                return Types.BOOL;
            case List:
                return Types.LIST;
            case Dict:
                return Types.DICT;
            case Bytes:
                return Types.BYTES;
            case Scope:

                // This is an if statement
            case Query:
                return Types.ANY;

            case Enum:
                return resolve((EnumNode) statement);
            case ClassDef:
                return resolve((ClassDefNode) statement);
            case Claccess: {
                ClaccessNode node = (ClaccessNode) statement;
                Type clazz = resolve(node.class_tok);
                String attr = node.attr_name_tok.value.toString();
                Type attrType = clazz.access(attr);
                // If they are memory similar, then it is "this"
                if (clazz == compiler.enclosingType) {
                    attrType = compiler.accessEnclosed(attr);
                }
                if (attrType == null) {
                    compiler.error("Attribute", String.format("'%s' object has no attribute '%s'", clazz, attr), node.pos_start, node.pos_end);
                }
                return attrType;
            }
            case AttrAssign: {
                AttrAssignNode node = (AttrAssignNode) statement;
                Type newAttr = resolve(node.value_node);
                String attr = node.var_name_tok.value.toString();
                Type oldAttr = compiler.accessEnclosed(attr);
                if (oldAttr == null) {
                    compiler.error("Attribute", String.format("'%s' object has no attribute '%s'", compiler.enclosingType, attr), node.pos_start, node.pos_end);
                }
                else if (!oldAttr.equals(newAttr) && oldAttr != Types.ANY) {
                    compiler.error("Type", String.format("Cannot assign '%s' to '%s'", newAttr, oldAttr), node.pos_start, node.pos_end);
                }
                return Types.VOID;
            }
            case AttrAccess: {
                AttrAccessNode node = (AttrAccessNode) statement;
                String attr = node.var_name_tok.value.toString();
                Type type = compiler.accessEnclosed(attr);
                if (type == null) {
                    compiler.error("Attribute", String.format("'%s' object has no attribute '%s'", compiler.enclosingType, attr), node.pos_start, node.pos_end);
                }
                return type;
            }

            case VarAccess:
                return resolve((VarAccessNode) statement);
            case VarAssign:
                return resolve((VarAssignNode) statement);

            case While:
                return resolve((WhileNode) statement);
            case For:
                return resolve((ForNode) statement);
            case Iter:
                return resolve((IterNode) statement);

            case Ref:
                return resolve((RefNode) statement);
            case Deref:
                return resolve((DerefNode) statement);

            default:
                throw new RuntimeException("Unknown statement type: " + statement.jptype);
        }
    }

    private Type resolve(BinOpNode statement) {
        Type left = resolve(statement.left_node);
        Type right = resolve(statement.right_node);
        Type result = left.isCompatible(statement.op_tok, right);
        if (result == null) {
            compiler.error("Type", String.format("Cannot apply '%s' to '%s' and '%s'", statement.op_tok, left, right), statement.pos_start, statement.pos_end);
        }
        return result;
    }

    private Type resolve(UnaryOpNode statement) {
        Type right = resolve(statement.node);
        Type result = right.isCompatible(statement.op_tok);
        if (result == null) {
            compiler.error("Type", String.format("Cannot apply '%s' to '%s'", statement.op_tok, right), statement.pos_start, statement.pos_end);
        }
        return result;
    }

    private Type resolve(ImportNode node) {
        String fn = node.file_name_tok.asString();
        JFunc imp = compiler.getImport(node);
        Type type;
        // If it's null and hasn't crashed, must be a builtin library
        if (imp == null) {
            type = Shell.libraries.get(fn);
        }
        else {
            type = new NamespaceType(imp.chunk.globals);
        }
        return type;
    }

    private Type resolve(DecoratorNode node) {
        Type decorator = resolve(node.decorator);
        Type decorated = resolve(node.decorated);
        Type result = decorator.call(new Type[]{ decorated }, new Type[0]);
        if (!decorated.equals(result)) {
            compiler.error("Decorator", "Decorator must return the decorated function", node.pos_start, node.pos_end);
        }
        return result;
    }

    private Type resolve(FuncDefNode node) {
        // Insert generic types into type map
        GenericType[] generics = new GenericType[node.generic_toks.size()];
        for (int i = 0; i < node.generic_toks.size(); i++) {
            String generic = node.generic_toks.get(i).value.toString();
            generics[i] = new GenericType(generic);
            types.put(generic, generics[i]);
        }

        Type returnType = resolve(node.returnType, node.pos_start, node.pos_end);
        Type[] argTypes = new Type[node.arg_type_toks.size()];
        for (int i = 0; i < node.arg_type_toks.size(); i++) {
            Token argTypeTok = node.arg_type_toks.get(i);
            argTypes[i] = resolve(argTypeTok);
        }

        // Remove generic types from type map
        for (GenericType generic : generics) {
            types.remove(generic.name);
        }

        return new FuncType(returnType, argTypes, generics, node.argname != null, node.defaultCount);
    }

    private Type resolve(CallNode node) {
        Type func = resolve(node.nodeToCall);
        Type[] argTypes = new Type[node.argNodes.size()];
        for (int i = 0; i < node.argNodes.size(); i++) {
            argTypes[i] = resolve(node.argNodes.get(i));
        }
        Type[] generics = new Type[node.generics.size()];
        for (int i = 0; i < node.generics.size(); i++) {
            generics[i] = resolve(node.generics.get(i));
        }
        Type result = func.call(argTypes, generics);
        if (result == null) {
            compiler.error("Call", "Cannot call function with the given arguments", node.pos_start, node.pos_end);
        }
        return result;
    }

    private Type resolve(ReturnNode node) {
        Type result;
        if (node.nodeToReturn == null) {
            result = Types.VOID;
        } else {
            result = resolve(node.nodeToReturn);
        }
        if (!compiler.funcType.returnType.equals(result)) {
            compiler.error("Return", "Return type must be the same as the function's return type", node.pos_start, node.pos_end);
        }
        return Types.VOID;
    }

    private Type resolve(EnumNode node) {
        EnumChildType[] children = new EnumChildType[node.children.size()];
        for (int i = 0; i < node.children.size(); i++) {
            Parser.EnumChild child = node.children.get(i);
            String[] properties = child.params().toArray(new String[0]);
            Type[] propertyTypes = new Type[child.types().size()];
            for (int j = 0; j < propertyTypes.length; j++) {
                Type type = resolve(child.types().get(j), child.token().pos_start, child.token().pos_end);
                propertyTypes[j] = type;
            }
            GenericType[] generics = new GenericType[child.generics().size()];
            for (int j = 0; j < generics.length; j++) {
                generics[j] = new GenericType(child.generics().get(j));
            }
            children[i] = new EnumChildType(child.token().value.toString(), propertyTypes, generics, properties);
        }
        Type type = new EnumType(node.tok.value.toString(), children);
        types.put(node.tok.value.toString(), type);
        return type;
    }

    private Type resolve(ClassDefNode node) {
        String name = node.class_name_tok.value.toString();
        ClassType parent = null;
        if (node.parentToken != null) {
            Type UNOwen = getType(node.parentToken.value.toString());
            if (UNOwen == null) {
                compiler.error("Class", "Parent class must be defined before it is used", node.parentToken.pos_start, node.parentToken.pos_end);
            }
            if (!(UNOwen instanceof ClassType)) {
                compiler.error("Class", "Parent must be a class", node.parentToken.pos_start, node.parentToken.pos_end);
            }
            parent = (ClassType) UNOwen;
        }

        Position constructorStart = node.make_node.pos_start;
        Position constructorEnd = node.make_node.pos_end;
        FuncDefNode constructorNode = new FuncDefNode(
                new Token(TokenType.Identifier, "<make>", constructorStart, constructorEnd),
                node.arg_name_toks,
                node.arg_type_toks,
                node.make_node,
                false,
                false,
                Collections.singletonList("void"),
                node.defaults,
                node.defaultCount,
                node.generic_toks,
                node.argname,
                node.kwargname
        );

        GenericType[] generics = new GenericType[constructorNode.generic_toks.size()];
        for (int i = 0; i < constructorNode.generic_toks.size(); i++) {
            generics[i] = new GenericType(constructorNode.generic_toks.get(i).value.toString());
        }

        ClassType type = new ClassType(name, parent, null, new HashMap<>(), new HashSet<>(), new HashMap<>(), new HashMap<>(), generics);
        types.put(name, type);

        FuncType constructor = (FuncType) resolve(constructorNode);
        Map<String, Type> fields = new HashMap<>();
        Set<String> privates = new HashSet<>();
        Map<String, Type> staticFields = new HashMap<>();
        Map<String, Type> operators = new HashMap<>();

        for (AttrDeclareNode attr : node.attributes) {
            if (attr.isprivate) {
                privates.add(attr.name);
            }
            Type attrType = resolve(attr.type, attr.pos_start, attr.pos_end);
            if (attr.isstatic) {
                staticFields.put(attr.name, attrType);
            }
            else {
                fields.put(attr.name, attrType);
            }
        }
        for (MethDefNode meth : node.methods) {
            String funcName = meth.var_name_tok.value.toString();
            if (meth.priv) {
                privates.add(funcName);
            }
            Type methType = resolve(meth.asFuncDef());
            if (meth.stat) {
                staticFields.put(funcName, methType);
            }
            else if (meth.bin) {
                operators.put(funcName, methType);
            }
            else {
                fields.put(funcName, methType);
            }
        }

        type.fields.putAll(fields);
        type.fields.putAll(operators);
        type.staticFields.putAll(staticFields);
        type.operators.putAll(operators);
        type.privates.addAll(privates);
        type.constructor = constructor;

        return type;
    }

    private Type resolve(VarAccessNode node) {
        return compiler.variableType(node.var_name_tok.value.toString(), node.var_name_tok.pos_start, node.var_name_tok.pos_end);
    }

    private Type resolve(VarAssignNode node) {
        if (node.defining) {
            return Types.VOID;
        }
        else {
            return resolve(node.value_node);
        }
    }

    private Type resolve(WhileNode node) {
        return node.retnull ? Types.VOID : Types.LIST;
    }

    private Type resolve(ForNode node) {
        return node.retnull ? Types.VOID : Types.LIST;
    }

    private Type resolve(IterNode node) {
        return node.retnull ? Types.VOID : Types.LIST;
    }

    private Type resolve(RefNode node) {
        return new ReferenceType(resolve(node.inner));
    }

    private Type resolve(DerefNode node) {
        Type dereferencing = resolve(node.ref);
        if (!(dereferencing instanceof ReferenceType)) {
            compiler.error("Deref", "Cannot dereference non-reference type", node.ref.pos_start, node.ref.pos_end);
        }
        return ((ReferenceType) dereferencing).ref;
    }
}
