package lemon.jpizza;

import java.io.Serializable;

public class Position implements Serializable {
    public int idx;
    public int ln;
    public int col;
    public int tcol = -1;
    public int tidx;
    public String fn;
    public String ftext;

    public Position(int idx, int ln, int col, String fn, String ftext) {
        this.idx = idx;
        this.ln = ln;
        this.col = col;
        this.fn = fn;
        this.ftext = ftext;
    }

    public Position setT(int tcol, int tidx) {
        this.tcol = tcol;
        this.tidx = tidx;
        return this;
    }

    public void advance(char current_char) {
        idx++;
        col++;

        if (current_char != '\t') {
            tcol++;
            tidx++;
        }

        if (current_char == Constants.splitter) {
            ln++;
            col = 0;
            tcol = 0;
        }

    }

    public Position advance() {
        idx++;
        col++;
        return this;
    }

    public Position copy() {
        return new Position(idx, ln, col, fn, ftext).setT(tcol, tidx);
    }
}
