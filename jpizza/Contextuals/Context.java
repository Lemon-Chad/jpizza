package lemon.jpizza.Contextuals;

import lemon.jpizza.Position;

public class Context {
    public String displayName;
    public Context parent;
    public Position parentEntryPos;
    public SymbolTable symbolTable;

    public Context(String displayName, Context parent, Position parentEntryPos) {
        this.displayName = displayName;
        this.parent = parent;
        this.parentEntryPos = parentEntryPos;
        symbolTable = null;
    }

}
