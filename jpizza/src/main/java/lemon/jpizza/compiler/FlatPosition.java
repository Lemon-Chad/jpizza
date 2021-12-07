package lemon.jpizza.compiler;

import java.io.Serializable;

public class FlatPosition implements Serializable {
    // The starting index of the token in the source file
    public final int index;
    // The number of characters after the index
    public final int len;
    // How many bytes this instruction takes
    public int span;

    public FlatPosition(int index, int len, int span) {
        this.index = index;
        this.len = len;
        this.span = span;
    }

    public String toString() {
        return "(" + index + "," + len + ")";
    }
}
