package lemon.jpizza.compiler;

import lemon.jpizza.Shell;
import lemon.jpizza.compiler.values.functions.JFunc;

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

            case OpCode.Header -> {
                int constant = chunk.code.get(offset + 1);
                int args = chunk.code.get(offset + 2);
                Shell.logger.debug(String.format("%-16s %04d %04d%n", "OP_HEADER", constant, args));
                yield offset + 3 + args;
            }

            case OpCode.Destruct -> {
                int count = chunk.code.get(offset + 1);
                if (count == -1) {
                    Shell.logger.debug(String.format("%-16s %-16s%n", "OP_DESTRUCT", "GLOB"));
                    yield offset + 2;
                }
                Shell.logger.debug(String.format("%-16s %04d%n", "OP_DESTRUCT", count));
                yield offset + 2 + count;
            }

            case OpCode.Constant -> constantInstruction("OP_CONSTANT", chunk, offset);
            case OpCode.SetGlobal -> constantInstruction("OP_SET_GLOBAL", chunk, offset);
            case OpCode.GetGlobal -> constantInstruction("OP_GET_GLOBAL", chunk, offset);
            case OpCode.DefineGlobal -> declInstruction("OP_DEFINE_GLOBAL", chunk, offset, false);

            case OpCode.PushTraceback -> byteInstruction("OP_PUSH_TRACEBACK", chunk, offset);

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
            case OpCode.DefineLocal -> declInstruction("OP_DEFINE_LOCAL", chunk, offset, true);

            case OpCode.MakeVar -> {
                int arg = chunk.code.get(offset + 1);
                String type = chunk.constants.values.get(chunk.code.get(offset + 2)).asString();
                String constant = chunk.code.get(offset + 3) == 1 ? "CONSTANT" : "MUTABLE";
                Shell.logger.debug(String.format("%-16s %-16s %04d '%s'%n", constant, "OP_MAKE_VAR", arg, type));
                yield offset + 4;
            }

            case OpCode.Throw -> simpleInstruction("OP_THROW", offset);
            case OpCode.Assert -> simpleInstruction("OP_ASSERT", offset);

            case OpCode.Copy -> simpleInstruction("OP_COPY", offset);

            case OpCode.MakeArray -> byteInstruction("OP_MAKE_ARRAY", chunk, offset);
            case OpCode.MakeMap -> byteInstruction("OP_MAKE_MAP", chunk, offset);

            case OpCode.Enum -> constantInstruction("OP_ENUM", chunk, offset);

            case OpCode.GetUpvalue -> byteInstruction("OP_GET_UPVALUE", chunk, offset);
            case OpCode.SetUpvalue -> byteInstruction("OP_SET_UPVALUE", chunk, offset);

            case OpCode.FromBytes -> simpleInstruction("OP_FROM_BYTES", offset);
            case OpCode.ToBytes -> simpleInstruction("OP_TO_BYTES", offset);

            case OpCode.Deref -> simpleInstruction("OP_DEREF", offset);
            case OpCode.Ref -> simpleInstruction("OP_REF", offset);
            case OpCode.SetRef -> simpleInstruction("OP_SET_REF", offset);

            case OpCode.Jump -> jumpInstruction("OP_JUMP", 1, chunk, offset);
            case OpCode.JumpIfFalse -> jumpInstruction("OP_JUMP_IF_FALSE", 1, chunk, offset);
            case OpCode.JumpIfTrue -> jumpInstruction("OP_JUMP_IF_TRUE", 1, chunk, offset);
            
            case OpCode.Loop -> jumpInstruction("OP_LOOP", -1, chunk, offset);

            case OpCode.Method -> {
                constantInstruction("OP_METHOD", chunk, offset);
                yield offset + 5;
            }

            case OpCode.Access -> constantInstruction("OP_ACCESS", chunk, offset);

            case OpCode.StartCache -> simpleInstruction("OP_START_CACHE", offset);
            case OpCode.CollectLoop -> simpleInstruction("OP_COLLECT_LOOP", offset);
            case OpCode.FlushLoop -> simpleInstruction("OP_FLUSH_LOOP", offset);

            case OpCode.For -> forInstruction(chunk, offset);

            case OpCode.Class -> {
                int end = constantInstruction("OP_CLASS", chunk, offset);
                int attributeCount = chunk.code.get(offset + 2);
                // CONSTANT ISSTATIC ISPRIVATE TYPE
                int totalAttributeOffset = 1 + attributeCount * 4;
                yield end + totalAttributeOffset;
            }

            case OpCode.Call -> {
                int argCount = chunk.code.get(offset + 1);
                int kwargCount = chunk.code.get(offset + 2);
                int genericCount = chunk.code.get(offset + 3);
                Shell.logger.debug(String.format("%-16s %04d %04d %04d%n", "OP_CALL", argCount, kwargCount, genericCount));
                yield offset + 4;
            }
            case OpCode.Closure -> {
                offset++;
                int constant = chunk.code.get(offset++);
                int defaults = chunk.code.get(offset++);
                Shell.logger.debug(String.format("%-16s %04d %04d ", "OP_CLOSURE", constant, defaults));
                Shell.logger.debug(chunk.constants.values.get(constant));
                Shell.logger.debug("\n");

                JFunc func = chunk.constants.values.get(constant).asFunc();
                for (int i = 0; i < func.upvalueCount; i++) {
                    int isLocal = chunk.code.get(offset++);
                    int index = chunk.code.get(offset++);
                    Shell.logger.debug(String.format("%04d      |                     %s %d\n",
                            offset - 2, isLocal == 1 ? "local" : "upvalue", index));
                }

                yield offset;
            }

            case OpCode.Null -> simpleInstruction("OP_NULL", offset);

            case OpCode.SetAttr -> byteInstruction("OP_SET_ATTR", chunk, offset);
            case OpCode.GetAttr -> byteInstruction("OP_GET_ATTR", chunk, offset);

            case OpCode.Import -> constantInstruction("OP_IMPORT", chunk, offset);

            case OpCode.BitAnd -> simpleInstruction("OP_BIT_AND", offset);
            case OpCode.BitOr -> simpleInstruction("OP_BIT_OR", offset);
            case OpCode.BitXor -> simpleInstruction("OP_BIT_XOR", offset);
            case OpCode.BitCompl -> simpleInstruction("OP_BIT_NOT", offset);
            case OpCode.LeftShift -> simpleInstruction("OP_BIT_SHIFT_LEFT", offset);
            case OpCode.RightShift -> simpleInstruction("OP_BIT_SHIFT_RIGHT", offset);
            case OpCode.SignRightShift -> simpleInstruction("OP_BIT_SHIFT_RIGHT_SIGNED", offset);

            case OpCode.DropGlobal -> constantInstruction("OP_DROP_GLOBAL", chunk, offset);
            case OpCode.DropLocal -> byteInstruction("OP_DROP_LOCAL", chunk, offset);
            case OpCode.DropUpvalue -> byteInstruction("OP_DROP_UPVALUE", chunk, offset);

            case OpCode.Spread -> simpleInstruction("OP_SPREAD", offset);

            case OpCode.Get -> simpleInstruction("OP_GET", offset);
            case OpCode.Index -> simpleInstruction("OP_INDEX", offset);

            case OpCode.Iter -> {
                int slot = chunk.code.get(offset + 1);
                int slot2 = chunk.code.get(offset + 2);
                int jump = chunk.code.get(offset + 3);

                Shell.logger.debug(String.format("%-16s %04d %04d %04d -> %04d\n", "OP_ITER", slot, slot2, offset, offset + jump));

                yield offset + 4;
            }

            case OpCode.NullErr -> byteInstruction("OP_NULL_ERR", chunk, offset);

            case OpCode.Chain -> simpleInstruction("OP_CHAIN", offset);

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
        Shell.logger.debug(String.format("%-16s %04d '", name, constant));
        Shell.logger.debug(chunk.constants.values.get(constant));
        Shell.logger.debug("'\n");
        return offset + 2;
    }
    
    static int byteInstruction(String name, Chunk chunk, int offset) {
        int local = chunk.code.get(offset + 1);
        Shell.logger.debug(String.format("%-16s %04d%n", name, local));
        return offset + 2;
    }

    static int declInstruction(String name, Chunk chunk, int offset, boolean isLocal) {
        int arg = !isLocal ? chunk.code.get(offset + 1) : 0;
        String type = chunk.constants.values.get(chunk.code.get(offset + 1 + (!isLocal ? 1 : 0))).asString();
        String constant = chunk.code.get(offset + 2 + (!isLocal ? 1 : 0)) == 1 ? "CONSTANT" : "MUTABLE";
        if (isLocal)
            Shell.logger.debug(String.format("%-16s %-16s '%s'%n", constant, name, type));
        else
            Shell.logger.debug(String.format("%-16s %-16s %04d '%s' : '%s'%n", constant, name, arg,
                    chunk.constants.values.get(arg), type));
        return offset + 3 + (!isLocal ? 1 : 0);
    }
    
    static int jumpInstruction(String name, int sign, Chunk chunk, int offset) {
        int jump = sign * chunk.code.get(offset + 1);

        Shell.logger.debug(String.format("%-16s %04d -> %04d%n", name, offset, offset + 2 + jump));
        
        return offset + 2;
    }

    static int forInstruction(Chunk chunk, int offset) {
        int constant = chunk.code.get(offset + 1);
        int jump = chunk.code.get(offset + 2);

        Shell.logger.debug(String.format("%-16s %04d '%s' %04d -> %04d%n", "OP_FOR", constant, chunk.constants.values.get(constant), offset + 3, offset + 3 + jump));
        return offset + 3;
    }
}
