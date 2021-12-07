package lemon.jpizza;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

public class Token implements Serializable {
    public final Tokens.TT type;
    public final Object value;
    public Position pos_start;
    public Position pos_end;

    public Token(Tokens.TT type, Object value, @NotNull Position pos_start, @NotNull Position pos_end) {
        this.type = type;
        this.value = value;

        if (pos_start != null) {
            this.pos_start = pos_start.copy();
            this.pos_end = pos_end != null ? pos_end.copy() : pos_start.copy().advance();
        }
    }

    public Token(Tokens.TT type, @NotNull Position start_pos) {
        this.type = type;
        this.value = null;

        this.pos_start = start_pos.copy();
        this.pos_end = start_pos.copy().advance();
    }

    public Token(Tokens.TT type, @NotNull Position start_pos, @NotNull Position end_pos) {
        this.type = type;
        this.value = null;

        this.pos_start = start_pos.copy();
        this.pos_end = end_pos.copy();
    }

    public boolean matches(Tokens.TT type, Object value) {
        return this.type.equals(type) && (this.value == null || this.value.equals(value));
    }

    public String toString() {
        return value != null ? String.format(
                "%s:%s",
                type, value
        ) : String.valueOf(type);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Token)) return false;

        Token other = (Token) o;
        if (value == null) return other.type == type && other.value == null;
        return other.type == type && value.equals(other.value);
    }
}
