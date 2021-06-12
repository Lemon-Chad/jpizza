package lemon.jpizza.Contextuals;

import lemon.jpizza.Nodes.Variables.AttrNode;
import lemon.jpizza.Nodes.Variables.VarNode;
import lemon.jpizza.Objects.Executables.CMethod;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Token;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    public Map<String, VarNode> symbols;
    Map<String, AttrNode> attributes;
    Map<String, CMethod> bins;
    SymbolTable parent;

    public SymbolTable() {
        parent = null;
        symbols = new HashMap<>();
        attributes = new HashMap<>();
        bins = new HashMap<>();
    }
    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
        symbols = new HashMap<>();
        attributes = new HashMap<>();
        bins = new HashMap<>();
    }

    public Object get(String name) {
        VarNode value = symbols.get(name);
        if (value == null && parent != null)
            return parent.get(name);
        return value != null ? value.value_node : null;
    }
    public boolean locked(String name) {
        VarNode value = symbols.get(name);
        if (value == null && parent != null)
            return parent.locked(name);
        return value != null && value.locked;
    }
    public void remove(String name) {
        VarNode value = symbols.get(name);
        if (value == null) {
            if (parent != null) {
                parent.remove(name);
            } return;
        }
        symbols.remove(name);
    }
    public String set(String name, Object value, boolean locked) {
        if (symbols.containsKey(name) && symbols.get(name).locked) {
            return "Baked variable already defined";
        }
        symbols.put(name, new VarNode(value, locked));
        return null;
    }
    public String set(String name, Object value) { return set(name, value, false); }

    public void declareattr(Token name_tok, Context context) {
        attributes.put(name_tok.value.toString(), new AttrNode(new Null().set_pos(name_tok.pos_start).set_context(context)));
    }
    public Object getattr(String name) {
        if (attributes.containsKey(name)) { return attributes.get(name).value_node; }
        return parent != null ? parent.getattr(name) : null;
    }
    public void setattr(String name, Object value) {
        if (attributes.containsKey(name)) {
            attributes.put(name, new AttrNode(value));
        } else if (parent != null) {
            parent.setattr(name, value);
        }
    }

    public void setbin(String name, CMethod method) { bins.put(name, method); }
    public CMethod getbin(String name) {
        if (bins.containsKey(name)) return bins.get(name);
        return parent != null ? parent.getbin(name) : null;
    }
}
