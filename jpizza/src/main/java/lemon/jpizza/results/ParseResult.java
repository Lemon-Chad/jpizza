package lemon.jpizza.results;

import lemon.jpizza.errors.Error;

public class ParseResult {
    public Object node = null;
    public Error error = null;
    public int advanceCount = 0;
    public int toReverseCount = 0;

    public void registerAdvancement() { advanceCount++; }

    public Object register(ParseResult res) {
        advanceCount += res.advanceCount;
        if (res.error != null) {
            error = res.error;
        } return res.node;
    }

    public Object try_register(ParseResult res) {
        if (res.error != null) {
            toReverseCount += res.advanceCount;
            return null;
        } return register(res);
    }

    public ParseResult success(Object node) {
        this.node = node;
        return this;
    }

    public ParseResult failure(Error error) {
        if (this.error == null || advanceCount == 0) {
            this.error = error;
        } return this;
    }


}
