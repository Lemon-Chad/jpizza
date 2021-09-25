package lemon.jpizza.objects.primitives;

import lemon.jpizza.Constants;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.nodes.values.NullNode;
import lemon.jpizza.objects.executables.Function;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.Value;
import lemon.jpizza.Pair;
import lemon.jpizza.Token;
import lemon.jpizza.Tokens;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Bytes extends Value {
    public Bytes(byte[] arr) {
        super(null);
        this.arr = arr;

        jptype = Constants.JPType.Bytes;
    }

    // Functions

    public Pair<Object, RTError> stored() {
        ByteArrayInputStream in = new ByteArrayInputStream(this.arr);
        try {
            ObjectInputStream is = new ObjectInputStream(in);
            return new Pair<>(is.readObject(), null);
        } catch (IOException | ClassNotFoundException e) {
            return new Pair<>(null, RTError.Internal(
                    pos_start, pos_end,
                    "Internal byte error: " + e.toString(),
                    context
            ));
        }
    }

    // Conversions

    public Obj function() {
        return new Function(null, new NullNode(new Token(Tokens.TT.KEYWORD, "null", pos_start, pos_end)),
                new ArrayList<>()).set_context(context).set_pos(pos_start, pos_end);
    }

    public Obj number() {
        return new Num(arr.length).set_context(context).set_pos(pos_start, pos_end);
    }

    public Obj alist() {
        List<Obj> bytes = new ArrayList<>();
        for (byte i : this.arr)
            bytes.add(new Num(i).set_context(context).set_pos(pos_start, pos_end));
        return new PList(bytes).set_context(context).set_pos(pos_start, pos_end);
    }

    public Obj dictionary() {
        return new Dict(new HashMap<>()).set_context(context).set_pos(pos_start, pos_end);
    }

    public Obj bool() { return new Bool(true).set_context(context).set_pos(pos_start, pos_end); }

    // Defaults

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < arr.length; i++)
            sb.append(arr[i]).append(", ");

        return "{ " + sb.toString() + "len=" + arr.length + " }";
    }

    public Obj copy() { return new Bytes(arr).set_context(context).set_pos(pos_start, pos_end); }

    public Obj type() { return new Str("bytearray").set_context(context).set_pos(pos_start, pos_end); }

}
