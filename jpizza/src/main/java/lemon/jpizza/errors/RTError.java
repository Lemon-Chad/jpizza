package lemon.jpizza.errors;

import lemon.jpizza.Constants;
import lemon.jpizza.Shell;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.Position;

public class RTError extends Error {
    final Context context;

    public RTError(String name, Position pos_start, Position pos_end, String details, Context context) {
        super(pos_start, pos_end, name, details);
        this.context = context;
    }

    public String asString() {
        if (pos_start == null || pos_end == null) return String.format("%s: %s", error_name, details);
        return String.format(
                    "%s\n%s Error (Runtime): %s\nFile %s, line %s\n%s\n",
                    generateTraceback(),
                    error_name, details,
                    pos_start.fn, pos_start.ln + 1,
                    Constants.stringWithArrows(pos_start.ftext, pos_start, pos_end)
                );
    }

    public static RTError Type(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Type", pos_start, pos_end, details, context);
    }

    public static RTError Released(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Released", pos_start, pos_end, details, context);
    }

    public static RTError Unresolved(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Unresolved", pos_start, pos_end, details, context);
    }

    public static RTError Publicity(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Publicity", pos_start, pos_end, details, context);
    }

    public interface ErrorMethod {
        RTError build(Position pos_start, Position pos_end, String details, Context context);
    }

    public interface ErrorDetails {
        RTError build(Position pos_start, Position pos_end, Context context);
    }

    public static ErrorDetails makeDetails(ErrorMethod method, String details) {
        return (Position pos_start, Position pos_end, Context context) -> method.build(pos_start, pos_end, details, context);
    }

    public static RTError OutOfBounds(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Out of Bounds", pos_start, pos_end, details, context);
    }

    public static RTError Range(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Range", pos_start, pos_end, details, context);
    }

    public static RTError Internal(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Internal", pos_start, pos_end, details, context);
    }

    public static RTError FileNotFound(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Imaginary File", pos_start, pos_end, details, context);
    }

    public static RTError PathNotFound(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Imaginary Path", pos_start, pos_end, details, context);
    }

    public static RTError Init(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Initialization", pos_start, pos_end, details, context);
    }

    public static RTError InvalidArgument(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Invalid Argument", pos_start, pos_end, details, context);
    }

    public static RTError Scope(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Scope", pos_start, pos_end, details, context);
    }

    public static RTError GenericCount(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Generic Count", pos_start, pos_end, details, context);
    }

    public static RTError IllegalOperation(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Illegal Operation", pos_start, pos_end, details, context);
    }

    public static RTError Assertion(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Assertion", pos_start, pos_end, details, context);
    }

    public static RTError ArgumentCount(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Argument Count", pos_start, pos_end, details, context);
    }

    public static RTError Conversion(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Type Conversion", pos_start, pos_end, details, context);
    }

    public static RTError Interrupted(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Interrupted Process", pos_start, pos_end, details, context);
    }

    public static RTError Const(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Constant Reassignment", pos_start, pos_end, details, context);
    }

    public static RTError MalformedData(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Malformed Data", pos_start, pos_end, details, context);
    }

    public static RTError Formatting(Position pos_start, Position pos_end, String details, Context context) {
        return new RTError("Formatting", pos_start, pos_end, details, context);
    }

    public String generateTraceback() {
        String result = "";
        Position pos = pos_start.copy();
        Context ctx = context;

        String arrow = Shell.fileEncoding.equals("UTF-8") ? "╰──>" : "--->";

        while (ctx != null) {
            if (pos != null)
                result = String.format(
                            "  %s  File %s, line %s, in %s\n%s",
                            arrow,
                            pos.fn, pos.ln + 1, ctx.displayName,
                            result
                        );
            else
                result = String.format("\t%s\n%s", ctx.displayName, result);
            pos = ctx.parentEntryPos; ctx = ctx.parent;
        }

        return String.format("Traceback (most recent call last):\n%s", result);
    }

}
