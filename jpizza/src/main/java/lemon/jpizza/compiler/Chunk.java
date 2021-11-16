package lemon.jpizza.compiler;

public class Chunk {
    public enum OpCode {
        Return,
        Constant,

        Negate,
        Increment,
        Decrement,

        Add,
        Subtract,
        Multiply,
        Divide,
        Power,
        Modulo,
    }
}
