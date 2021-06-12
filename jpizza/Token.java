package lemon.jpizza;

public class Token {
    public String type;
    public Object value;
    public Position pos_start;
    public Position pos_end;

    public Token(String type, Object value, Position pos_start, Position pos_end) {
        this.type = type;
        this.value = value;

        if (pos_start != null) {
            this.pos_start = pos_start.copy();
            this.pos_end = pos_end != null ? pos_end.copy() : pos_start.copy().advance();
        }
    }

    public Token(String type) {
        this.type = type;
        this.value = null;

        this.pos_start = null;
        this.pos_end = null;
    }

    public Token(String type, Object value) {
        this.type = type;
        this.value = value;

        this.pos_start = null;
        this.pos_end = null;
    }

    public Token(String type, Position start_pos) {
        this.type = type;
        this.value = null;

        this.pos_start = start_pos.copy();
        this.pos_end = start_pos.copy().advance();
    }

    public Token(String type, Position start_pos, Position end_pos) {
        this.type = type;
        this.value = null;

        this.pos_start = start_pos.copy();
        this.pos_end = end_pos.copy();
    }

    public boolean matches(String type, Object value) {
        return this.type.equals(type) && this.value.equals(value);
    }

    public String toString() {
        return value != null ? String.format(
                "%s:%s",
                type, value
        ) : type;
    }
}
