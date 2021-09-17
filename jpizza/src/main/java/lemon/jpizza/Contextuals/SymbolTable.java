package lemon.jpizza.Contextuals;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;
import lemon.jpizza.Nodes.Variables.AttrNode;
import lemon.jpizza.Nodes.Variables.VarNode;
import lemon.jpizza.Objects.Executables.CMethod;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.Num;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Token;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable implements Serializable {
    public Map<String, VarNode> symbols = new HashMap<>();
    public Map<String, String> types = new HashMap<>();
    Map<String, AttrNode> attributes = new HashMap<>();
    Map<String, CMethod> bins = new HashMap<>();
    Map<String, Node> dyns = new HashMap<>();
    SymbolTable parent;

    public SymbolTable() {
        parent = null;
    }
    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
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

    public String define(String name, Object value, boolean locked, String type, Integer min, Integer max) {
        if (symbols.containsKey(name) && symbols.get(name).locked)
            return "Baked variable already defined";
        if (min != null || max != null)
            type = "num";
        if (!type.equals("any")) {
            Obj t = ((Obj) value).type().astring();
            String provided;
            if (t.jptype != Constants.JPType.String)
                return "Type is not a string";
            else if (!type.equals(provided = ((Str) t).trueValue()))
                return "Got type " + provided + ", expected type " + type;
        }
        if (min != null || max != null) {
            double v = ((Num) value).trueValue();
            if ((max != null && v > max) || (min != null && v < min))
                return "Number not in range";
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

    public String set(String name, Obj value, boolean locked) {
        VarNode curr = symbols.get(name);
        if (curr != null) {
            String expect = types.get(name);
            if (curr.locked)
                return "Baked variable already defined";
            else if (curr.min != null || curr.max != null) {
                double v = ((Num) value).trueValue();
                if ((curr.max != null && v > curr.max) || (curr.min != null && v < curr.min))
                    return "Number not in range";
            }
            else if (!expect.equals("any")) {
                Obj type = value.type().astring();
                String t;
                if (type.jptype != Constants.JPType.String)
                    return "Type is not a string";
                else if (!expect.equals(t = ((Str) type).trueValue()))
                    return "Got type " + t + ", expected type " + expect;
            }

            VarNode vn = new VarNode(value, locked).setRange(curr.min, curr.max);
            symbols.replace(name, vn);
            return null;
        }
        else if (attributes.containsKey(name)) {
            setattr(name, value);
            return null;
        }
        else if (parent != null) {
            return parent.set(name, value, locked);
        } else
            return "Variable not defined";
    }

    public void setDyn(String name, Node value) {
        dyns.put(name, value);
    }

    public void set(String name, Obj value) {
        set(name, value, false);
    }

    public void declareattr(Token name_tok, Context context) {
        String tokenVal = name_tok.value.toString();
        attributes.put(tokenVal, new AttrNode(new Null().set_pos(name_tok.pos_start).set_context(context)));
    }
    public Object getattr(String name) {
        if (attributes.containsKey(name)) { return attributes.get(name).value_node; }
        return parent != null ? parent.getattr(name) : null;
    }
    public void setattr(String name, Object value) {
        if (attributes.containsKey(name)) {
            attributes.replace(name, new AttrNode(value));
        } else if (parent != null) {
            parent.setattr(name, value);
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
