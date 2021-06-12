package lemon.jpizza;

public class Position {
    public int idx;
    public int ln;
    public int col;
    public String fn;
    public String ftext;
    public Position(int idx, int ln, int col, String fn, String ftext) {
        this.idx = idx;
        this.ln = ln;
        this.col = col;
        this.fn = fn;
        this.ftext = ftext;
    }

    public Position advance(char current_char) {
        idx++;
        col++;

        if (current_char == Constants.BREAK) {
            ln++;
            col = 0;
        }

        return this;
    }

    public Position advance() {
        idx++;
        col++;
        return this;
    }

    public Position copy() {
        return new Position(idx, ln, col, fn, ftext);
    }
}
