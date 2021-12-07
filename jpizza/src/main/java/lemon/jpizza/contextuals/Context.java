package lemon.jpizza.contextuals;

import lemon.jpizza.Position;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class Context implements Serializable {
    public String displayName;
    public final Context parent;
    public final Position parentEntryPos;
    public SymbolTable symbolTable;
    public boolean memoize = false;

    public Context(String displayName, Context parent, Position parentEntryPos) {
        this.displayName = displayName;
        this.parent = parent;
        this.parentEntryPos = parentEntryPos;
        symbolTable = null;
        if (parent != null) memoize = parent.memoize;
    }

    public void doMemoize() {
        memoize = true;
    }

    public Context getRoot() {
        if (parent == null)
            return this;
        return parent.getRoot();
    }

}
