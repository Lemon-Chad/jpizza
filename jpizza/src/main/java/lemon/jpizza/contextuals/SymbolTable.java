package lemon.jpizza.contextuals;

import lemon.jpizza.Constants;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.nodes.Node;
import lemon.jpizza.nodes.variables.AttrNode;
import lemon.jpizza.nodes.variables.VarNode;
import lemon.jpizza.objects.executables.CMethod;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.Token;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable implements Serializable {
    final Map<String, VarNode> symbols = new HashMap<>();
    final Map<String, String> types = new HashMap<>();
    final Map<String, String> attrtypes = new HashMap<>();
    final List<String> privates = new ArrayList<>();
    final Map<String, AttrNode> attributes = new HashMap<>();
    final Map<String, CMethod> bins = new HashMap<>();
    final Map<String, Node> dyns = new HashMap<>();
    final SymbolTable parent;

    public SymbolTable() {
        parent = null;
    }
    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
    }

    public Map<String, AttrNode> attributes() {
        return attributes;
    }

    public Object get(String name) {
        VarNode value = symbols.get(name);
        if (value == null) {
            if (attributes.containsKey(name)) {
                return attributes.get(name).value_node;
            } else if (parent != null) {
                return parent.get(name);
            }
        }
        return value != null ? value.value_node : null;
    }

    public Node getDyn(String name) {
        Node value = dyns.get(name);
        if (value == null && parent != null)
            return parent.getDyn(name);
        return value;
    }

    public boolean isDyn(String name) {
        return !symbols.containsKey(name) && dyns.containsKey(name);
    }

    public void remove(String name) {
        if (!symbols.containsKey(name)) {
            if (parent != null) {
                parent.remove(name);
            } return;
        }
        symbols.remove(name);
    }

    public RTError.ErrorDetails define(String name, Object value, boolean locked, String type, Integer min, Integer max) {
        if (symbols.containsKey(name) && symbols.get(name).locked)
            return RTError.makeDetails(RTError::Const, "Baked variable already defined");
        if (min != null || max != null)
            type = "num";
        if (!type.equals("any")) {
            Obj t = ((Obj) value).type().astring();
            String provided;
            if (t.jptype != Constants.JPType.String)
                return RTError.makeDetails(RTError::Type, "Type is not a string");
            else if (!type.equals(provided = t.string))
                return RTError.makeDetails(RTError::Type, "Got type " + provided + ", expected type " + type);
        }
        if (min != null || max != null) {
            double v = ((Obj) value).number;
            if ((max != null && v > max) || (min != null && v < min))
                return RTError.makeDetails(RTError::Range, "Number not in range");
        }
        symbols.put(name, new VarNode(value, locked).setRange(min, max));
        types.put(name, type);
        return null;
    }
    public void define(String name, Object value, String type) {
        define(name, value, false, type, null, null);
    }

    public void define(String name, Object value) {
        define(name, value, "any");
    }

    public RTError.ErrorDetails set(String name, Obj value, boolean locked) {
        VarNode curr = symbols.get(name);
        if (curr != null) {
            String expect = types.get(name);
            if (curr.locked)
                return RTError.makeDetails(RTError::Const, "Baked variable already defined");
            else if (curr.min != null || curr.max != null) {
                double v = value.number;
                if ((curr.max != null && v > curr.max) || (curr.min != null && v < curr.min))
                    return RTError.makeDetails(RTError::Range, "Number not in range");
            }
            else if (!expect.equals("any")) {
                Obj type = value.type().astring();
                String t;
                if (type.jptype != Constants.JPType.String)
                    return RTError.makeDetails(RTError::Type, "Type is not a string");
                else if (!expect.equals(t = type.string))
                    return RTError.makeDetails(RTError::Type, "Got type " + t + ", expected type " + expect);
            }

            VarNode vn = new VarNode(value, locked).setRange(curr.min, curr.max);
            symbols.replace(name, vn);
            return null;
        }
        else if (attributes.containsKey(name)) {
            return setattr(name, value);
        }
        else if (parent != null) {
            return parent.set(name, value, locked);
        } else
            return RTError.makeDetails(RTError::Scope, "Variable not defined");
    }

    public void setDyn(String name, Node value) {
        dyns.put(name, value);
    }

    public void set(String name, Obj value) {
        set(name, value, false);
    }

    public void declareattr(Token name_tok, Context context) {
        declareattr(name_tok, context, new Null());
    }

    public void makeprivate(String name) {
        privates.add(name);
    }
    public boolean isprivate(String name) {
        return privates.contains(name);
    }

    public void declareattr(Token name_tok, Context context, Obj value) {
        assert name_tok.value != null;
        String tokenVal = name_tok.value.toString();
        attributes.put(tokenVal, new AttrNode(value.set_pos(name_tok.pos_start, name_tok.pos_end).set_context(context)));
        attrtypes.put(tokenVal, "any");
    }
    public Object getattr(String name) {
        if (attributes.containsKey(name)) { return attributes.get(name).value_node; }
        return parent != null ? parent.getattr(name) : null;
    }
    public RTError.ErrorDetails setattr(String name, Object value) {
        if (attributes.containsKey(name)) {
            String type = attrtypes.get(name);
            if (value instanceof Obj) {
                String otype = ((Obj) value).type().toString();
                if (!type.equals("any") && !otype.equals(type))
                    return RTError.makeDetails(RTError::Type, String.format("Expected %s, got %s", type, otype));
            }
            attributes.replace(name, new AttrNode(value));
            return null;
        } else if (parent != null) {
            return parent.setattr(name, value);
        } else {
            return RTError.makeDetails(RTError::Scope, "Undefined attribute");
        }
    }
    public void setattrtype(String name, String type) {
        if (attributes.containsKey(name)) {
            attrtypes.put(name, type);
        } else if (parent != null) {
            parent.setattrtype(name, type);
        }
    }

    public void setbin(String name, CMethod method) {
        bins.put(name, method);
    }
    public CMethod getbin(String name) {
        if (bins.containsKey(name)) return bins.get(name);
        return parent != null ? parent.getbin(name) : null;
    }
}
