package lemon.jpizza.compiler;

import lemon.jpizza.Constants;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.ValueArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Chunk implements Serializable {
    final List<Integer> code;
    public int[] codeArray;
    ValueArray constants;
    public List<FlatPosition> positions;
    final String source;

    public Chunk(String source) {
        this.code = new ArrayList<>();
        this.constants = new ValueArray();
        this.positions = new ArrayList<>(Collections.singletonList(new FlatPosition(0, 0, 0)));
        this.source = source;
    }

    public void write(int b, int index, int len) {
        code.add(b);

        FlatPosition last = positions.get(positions.size() - 1);
        if (last.index == index && last.len == len) {
            last.span++;
        }
        else {
            positions.add(new FlatPosition(index, len, 1));
        }
    }

    public FlatPosition getPosition(int offset) {
        int i = 0;
        for (FlatPosition pos : positions) {
            if (i + pos.span >= offset) {
                return pos;
            }
            i += pos.span;
        }
        return positions.get(positions.size() - 1);
    }

    public void compile() {
        codeArray = new int[code.size()];
        for (int i = 0; i < code.size(); i++) {
            codeArray[i] = code.get(i);
        }
        constants.compile();
    }

    public int getLine(int offset) {
        return Constants.indexToLine(source, getPosition(offset).index);
    }

    public int addConstant(Value value) {
        return constants.write(value);
    }

    public String source() {
        return source;
    }

    public ValueArray constants() {
        return constants;
    }
    public void constants(ValueArray constants) {
        this.constants = constants;
    }

}
