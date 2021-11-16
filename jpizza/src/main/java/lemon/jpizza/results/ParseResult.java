package lemon.jpizza.results;

import lemon.jpizza.errors.Error;

public class ParseResult<T> {
    public T node = null;
    public Error error = null;
    public int advanceCount = 0;
    public int toReverseCount = 0;

    public void registerAdvancement() { advanceCount++; }

    public<U> U register(ParseResult<U> res) {
        advanceCount += res.advanceCount;
        if (res.error != null) {
            error = res.error;
        } return res.node;
    }

    public<U> U try_register(ParseResult<U> res) {
        if (res.error != null) {
            error = res.error;
            toReverseCount += res.advanceCount;
            return null;
        } return register(res);
    }

    public ParseResult<T> success(T node) {
        this.node = node;
        return this;
    }

    public ParseResult<T> failure(Error error) {
        if (this.error == null || advanceCount == 0) {
            this.error = error;
        } return this;
    }


}
