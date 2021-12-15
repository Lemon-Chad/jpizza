package lemon.jpizza.compiler.values.functions;

import lemon.jpizza.Pair;
import lemon.jpizza.compiler.values.Value;

public class Result {
    Pair<String, String> error = null;
    Value val;

    public Result(String message, String reason) {
        error = new Pair<>(message, reason);
        val = new Value();
    }

    public Result(Value val) {
        this.val = val;
    }

    public boolean isError() {
        return error != null;
    }

    public String getErrorMessage() {
        return error.a;
    }

    public String getErrorReason() {
        return error.b;
    }

    public Value getValue() {
        return val;
    }

}
