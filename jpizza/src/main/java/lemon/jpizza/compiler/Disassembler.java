package lemon.jpizza.compiler;

import lemon.jpizza.Shell;

public class Disassembler {
    public static void disassembleChunk(Chunk chunk, String string) {
        Shell.logger.debug(String.format("== %s ==%n", string));
        int offset = 0;

        while (offset < chunk.code.size()) {
            offset = disassembleInstruction(chunk, offset);
        }
    }

    public static int disassembleInstruction(Chunk chunk, int offset) {
        Shell.logger.debug(String.format("%04d ", offset));
        if (offset > 0 && chunk.getLine(offset) == chunk.getLine(offset - 1)) {
            Shell.logger.debug("   | ");
        }
        else {
            Shell.logger.debug(String.format("%4d ", chunk.getLine(offset)));
        }

        int instruction = chunk.code.get(offset);
        return switch (instruction) {
            case OpCode.Return -> simpleInstruction("OP_RETURN", offset);
            case OpCode.Pop -> simpleInstruction("OP_POP", offset);

            case OpCode.Constant -> constantInstruction("OP_CONSTANT", chunk, offset);
            case OpCode.SetGlobal -> constantInstruction("OP_SET_GLOBAL", chunk, offset);
            case OpCode.GetGlobal -> constantInstruction("OP_GET_GLOBAL", chunk, offset);
            case OpCode.DefineGlobal -> constantInstruction("OP_DEFINE_GLOBAL", chunk, offset);

            case OpCode.PushTraceback -> constantInstruction("OP_PUSH_TRACEBACK", chunk, offset);
            case OpCode.PopTraceback -> simpleInstruction("OP_POP_TRACEBACK", offset);

            case OpCode.Add -> simpleInstruction("OP_ADD", offset);
            case OpCode.Subtract -> simpleInstruction("OP_SUBTRACT", offset);
            case OpCode.Multiply -> simpleInstruction("OP_MULTIPLY", offset);
            case OpCode.Divide -> simpleInstruction("OP_DIVIDE", offset);
            case OpCode.Power -> simpleInstruction("OP_POWER", offset);
            case OpCode.Modulo -> simpleInstruction("OP_MODULO", offset);

            case OpCode.Negate -> simpleInstruction("OP_NEGATE", offset);
            case OpCode.Not -> simpleInstruction("OP_NOT", offset);
            case OpCode.Increment -> simpleInstruction("OP_INCREMENT", offset);
            case OpCode.Decrement -> simpleInstruction("OP_DECREMENT", offset);

            case OpCode.Equal -> simpleInstruction("OP_EQUAL", offset);
            case OpCode.GreaterThan -> simpleInstruction("OP_GREATER_THAN", offset);
            case OpCode.LessThan -> simpleInstruction("OP_LESS_THAN", offset);

            case OpCode.SetLocal -> byteInstruction("OP_SET_LOCAL", chunk, offset);
            case OpCode.GetLocal -> byteInstruction("OP_GET_LOCAL", chunk, offset);
            
            case OpCode.Jump -> jumpInstruction("OP_JUMP", 1, chunk, offset);
            case OpCode.JumpIfFalse -> jumpInstruction("OP_JUMP_IF_FALSE", 1, chunk, offset);
            case OpCode.JumpIfTrue -> jumpInstruction("OP_JUMP_IF_TRUE", 1, chunk, offset);
            
            case OpCode.Loop -> jumpInstruction("OP_LOOP", -1, chunk, offset);

            case OpCode.StartCache -> simpleInstruction("OP_START_CACHE", offset);
            case OpCode.CollectLoop -> simpleInstruction("OP_COLLECT_LOOP", offset);
            case OpCode.FlushLoop -> simpleInstruction("OP_FLUSH_LOOP", offset);

            case OpCode.For -> forInstruction(chunk, offset);

            default -> {
                Shell.logger.debug(String.format("Unknown opcode %d%n", instruction));
                yield offset + 1;
            }
        };
    }
    
    static int simpleInstruction(String name, int offset) {
        Shell.logger.debug(String.format("%-16s%n", name));
        return offset + 1;
    }
    
    static int constantInstruction(String name, Chunk chunk, int offset) {
        int constant = chunk.code.get(offset + 1);
        Shell.logger.debug(String.format("%-16s %4d '%s'%n", name, constant, chunk.constants.values.get(constant)));
        return offset + 2;
    }
    
    static int byteInstruction(String name, Chunk chunk, int offset) {
        int local = chunk.code.get(offset + 1);
        Shell.logger.debug(String.format("%-16s %4d%n", name, local));
        return offset + 2;
    }
    
    static int jumpInstruction(String name, int sign, Chunk chunk, int offset) {
        int jump = sign * chunk.code.get(offset + 1);

        Shell.logger.debug(String.format("%-16s %4d -> %04d%n", name, offset, offset + 2 + jump));
        
        return offset + 2;
    }

    static int forInstruction(Chunk chunk, int offset) {
        int constant = chunk.code.get(offset + 1);
        int jump = chunk.code.get(offset + 2);

        Shell.logger.debug(String.format("%-16s %4d '%s' %04d -> %04d%n", "OP_FOR", constant, chunk.constants.values.get(constant), offset + 3, offset + 3 + jump));
        return offset + 3;
    }
}
