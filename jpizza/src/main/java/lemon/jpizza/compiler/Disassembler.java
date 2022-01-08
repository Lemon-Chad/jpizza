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
        switch (instruction) {
            case OpCode.Return: return simpleInstruction("OP_RETURN", offset);
            case OpCode.Pop: return simpleInstruction("OP_POP", offset);

            case OpCode.Extend: return constantInstruction("OP_EXTEND", chunk, offset);

            case OpCode.PatternVars: return constantInstruction("OP_PATTERN_VARS", chunk, offset);

            case OpCode.Pattern: {
                int args = chunk.code.get(offset + 1);
                Shell.logger.debug(String.format("%-16s %04d%n", "OP_PATTERN", args));
                return offset + 2 + args;
            }

            case OpCode.IncrNullErr: return simpleInstruction("OP_INCR_NULL_ERR", offset);

            case OpCode.Header: {
                int constant = chunk.code.get(offset + 1);
                int args = chunk.code.get(offset + 2);
                Shell.logger.debug(String.format("%-16s %04d %04d%n", "OP_HEADER", constant, args));
                return offset + 3 + args;
            }

            case OpCode.Destruct: {
                int count = chunk.code.get(offset + 1);
                if (count == -1) {
                    Shell.logger.debug(String.format("%-16s %-16s%n", "OP_DESTRUCT", "GLOB"));
                    return offset + 2;
                }
                Shell.logger.debug(String.format("%-16s %04d%n", "OP_DESTRUCT", count));
                return offset + 2 + count;
            }

            case OpCode.Constant: return constantInstruction("OP_CONSTANT", chunk, offset);
            case OpCode.SetGlobal: return constantInstruction("OP_SET_GLOBAL", chunk, offset);
            case OpCode.GetGlobal: return constantInstruction("OP_GET_GLOBAL", chunk, offset);
            case OpCode.DefineGlobal: return declInstruction("OP_DEFINE_GLOBAL", chunk, offset, false);

            case OpCode.Add: return simpleInstruction("OP_ADD", offset);
            case OpCode.Subtract: return simpleInstruction("OP_SUBTRACT", offset);
            case OpCode.Multiply: return simpleInstruction("OP_MULTIPLY", offset);
            case OpCode.Divide: return simpleInstruction("OP_DIVIDE", offset);
            case OpCode.Power: return simpleInstruction("OP_POWER", offset);
            case OpCode.Modulo: return simpleInstruction("OP_MODULO", offset);

            case OpCode.Negate: return simpleInstruction("OP_NEGATE", offset);
            case OpCode.Not: return simpleInstruction("OP_NOT", offset);
            case OpCode.Increment: return simpleInstruction("OP_INCREMENT", offset);
            case OpCode.Decrement: return simpleInstruction("OP_DECREMENT", offset);

            case OpCode.Equal: return simpleInstruction("OP_EQUAL", offset);
            case OpCode.GreaterThan: return simpleInstruction("OP_GREATER_THAN", offset);
            case OpCode.LessThan: return simpleInstruction("OP_LESS_THAN", offset);

            case OpCode.SetLocal: return byteInstruction("OP_SET_LOCAL", chunk, offset);
            case OpCode.GetLocal: return byteInstruction("OP_GET_LOCAL", chunk, offset);
            case OpCode.DefineLocal: return declInstruction("OP_DEFINE_LOCAL", chunk, offset, true);

            case OpCode.MakeVar: {
                int arg = chunk.code.get(offset + 1);
                String type = chunk.constants.values.get(chunk.code.get(offset + 2)).asString();
                String constant = chunk.code.get(offset + 3) == 1 ? "CONSTANT" : "MUTABLE";
                Shell.logger.debug(String.format("%-16s %-16s %04d '%s'%n", constant, "OP_MAKE_VAR", arg, type));
                return offset + 4;
            }

            case OpCode.Throw: return simpleInstruction("OP_THROW", offset);
            case OpCode.Assert: return simpleInstruction("OP_ASSERT", offset);

            case OpCode.Copy: return simpleInstruction("OP_COPY", offset);

            case OpCode.MakeArray: return byteInstruction("OP_MAKE_ARRAY", chunk, offset);
            case OpCode.MakeMap: return byteInstruction("OP_MAKE_MAP", chunk, offset);

            case OpCode.Enum: {
                int constant = chunk.code.get(offset + 1);
                int isPublic = chunk.code.get(offset + 2);
                Shell.logger.debug(String.format("%-16s %-16s %-16s %n", "OP_ENUM", isPublic == 1 ? "PUBLIC" : "", chunk.constants.values.get(constant)));
                return offset + 3;
            }

            case OpCode.GetUpvalue: return byteInstruction("OP_GET_UPVALUE", chunk, offset);
            case OpCode.SetUpvalue: return byteInstruction("OP_SET_UPVALUE", chunk, offset);

            case OpCode.FromBytes: return simpleInstruction("OP_FROM_BYTES", offset);
            case OpCode.ToBytes: return simpleInstruction("OP_TO_BYTES", offset);

            case OpCode.Deref: return simpleInstruction("OP_DEREF", offset);
            case OpCode.Ref: return simpleInstruction("OP_REF", offset);
            case OpCode.SetRef: return simpleInstruction("OP_SET_REF", offset);

            case OpCode.Jump: return jumpInstruction("OP_JUMP", 1, chunk, offset);
            case OpCode.JumpIfFalse: return jumpInstruction("OP_JUMP_IF_FALSE", 1, chunk, offset);
            case OpCode.JumpIfTrue: return jumpInstruction("OP_JUMP_IF_TRUE", 1, chunk, offset);
            
            case OpCode.Loop: return jumpInstruction("OP_LOOP", -1, chunk, offset);

            case OpCode.Method:
                constantInstruction("OP_METHOD", chunk, offset);
                return offset + 5;

            case OpCode.Access: return constantInstruction("OP_ACCESS", chunk, offset);

            case OpCode.StartCache: return simpleInstruction("OP_START_CACHE", offset);
            case OpCode.CollectLoop: return simpleInstruction("OP_COLLECT_LOOP", offset);
            case OpCode.FlushLoop: return simpleInstruction("OP_FLUSH_LOOP", offset);

            case OpCode.For: return forInstruction(chunk, offset);

            case OpCode.Class: {
                int end = constantInstruction("OP_CLASS", chunk, offset);
                int attributeCount = chunk.code.get(offset + 2);
                // CONSTANT ISSTATIC ISPRIVATE TYPE
                int totalAttributeOffset = 1 + attributeCount * 4;
                return end + totalAttributeOffset;
            }

            case OpCode.Call: {
                int argCount = chunk.code.get(offset + 1);
                int kwargCount = chunk.code.get(offset + 2);
                int genericCount = chunk.code.get(offset + 3);
                Shell.logger.debug(String.format("%-16s %04d %04d %04d%n", "OP_CALL", argCount, kwargCount, genericCount));
                return offset + 4 + kwargCount + genericCount;
            }
            case OpCode.Closure: {
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
                            offset - 2, isLocal == 1 ? "local" : isLocal == 2 ? "global" : "upvalue", index));
                }

                return offset;
            }

            case OpCode.Null: return simpleInstruction("OP_NULL", offset);

            case OpCode.SetAttr: return byteInstruction("OP_SET_ATTR", chunk, offset);
            case OpCode.GetAttr: return byteInstruction("OP_GET_ATTR", chunk, offset);

            case OpCode.Import: {
                int fromConstant = chunk.code.get(offset + 1);
                int asConstant = chunk.code.get(offset + 2);
                Shell.logger.debug(String.format("%-16s %-16s as %-16s%n", "OP_IMPORT", chunk.constants.values.get(fromConstant), chunk.constants.values.get(asConstant)));
                return offset + 3;
            }

            case OpCode.BitAnd: return simpleInstruction("OP_BIT_AND", offset);
            case OpCode.BitOr: return simpleInstruction("OP_BIT_OR", offset);
            case OpCode.BitXor: return simpleInstruction("OP_BIT_XOR", offset);
            case OpCode.BitCompl: return simpleInstruction("OP_BIT_NOT", offset);
            case OpCode.LeftShift: return simpleInstruction("OP_BIT_SHIFT_LEFT", offset);
            case OpCode.RightShift: return simpleInstruction("OP_BIT_SHIFT_RIGHT", offset);
            case OpCode.SignRightShift: return simpleInstruction("OP_BIT_SHIFT_RIGHT_SIGNED", offset);

            case OpCode.DropGlobal: return constantInstruction("OP_DROP_GLOBAL", chunk, offset);
            case OpCode.DropLocal: return byteInstruction("OP_DROP_LOCAL", chunk, offset);
            case OpCode.DropUpvalue: return byteInstruction("OP_DROP_UPVALUE", chunk, offset);

            case OpCode.Spread: return simpleInstruction("OP_SPREAD", offset);

            case OpCode.Get: return simpleInstruction("OP_GET", offset);
            case OpCode.Index: return simpleInstruction("OP_INDEX", offset);

            case OpCode.Iter: {
                int slot = chunk.code.get(offset + 1);
                int slot2 = chunk.code.get(offset + 2);
                int jump = chunk.code.get(offset + 3);

                Shell.logger.debug(String.format("%-16s %04d %04d %04d -> %04d\n", "OP_ITER", slot, slot2, offset, offset + jump));

                return offset + 4;
            }

            case OpCode.NullErr: return byteInstruction("OP_NULL_ERR", chunk, offset);

            case OpCode.Chain: return simpleInstruction("OP_CHAIN", offset);

            default:
                Shell.logger.debug(String.format("Unknown opcode %d%n", instruction));
                return offset + 1;
        }
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
        int localOffset = !isLocal ? 1 : 0;
        String type = chunk.constants.values.get(chunk.code.get(offset + 1 + localOffset)).asString();
        String constant = chunk.code.get(offset + 2 + localOffset) == 1 ? "CONSTANT" : "MUTABLE";
        boolean hasRange = chunk.code.get(offset + 3 + localOffset) == 1;

        if (isLocal)
            Shell.logger.debug(String.format("%-16s %-16s '%s'%n", constant, name, type));
        else
            Shell.logger.debug(String.format("%-16s %-16s %04d '%s' : '%s'%n", constant, name, arg,
                    chunk.constants.values.get(arg), type));
        return offset + 4 + localOffset + (hasRange ? 2 : 0);
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
