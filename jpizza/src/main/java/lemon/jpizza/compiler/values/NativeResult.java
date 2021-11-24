package lemon.jpizza.compiler.values;

public class NativeResult {
    final Value value;
    final boolean isException;
    final String exceptionName;
    final String exceptionMessage;

    private NativeResult(Value value) {
        this.value = value;

        this.isException = false;
        this.exceptionMessage = null;
        this.exceptionName = null;
    }

    private NativeResult(String name, String reason) {
        this.value = null;

        this.isException = true;
        this.exceptionMessage = name;
        this.exceptionName = reason;
    }

    public static NativeResult Err(String name, String reason) {
        return new NativeResult(name, reason);
    }

    public static NativeResult Ok(Value value) {
        return new NativeResult(value);
    }

    public static NativeResult Ok() {
        return new NativeResult(new Value());
    }

    public boolean ok() {
        return !isException;
    }

    public Value value() {
        return value;
    }

    public String name() {
        return exceptionName;
    }

    public String reason() {
        return exceptionMessage;
    }
}

