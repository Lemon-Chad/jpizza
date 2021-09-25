package lemon.jpizza.libraries.socks;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Bytes;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.Position;
import lemon.jpizza.results.RTResult;

import java.io.*;

public class GenConn {
    DataOutputStream out;
    DataInputStream in;

    Position pos_start, pos_end;
    Context context;

    public RTError sendBytes(Obj data) {
        if (data.jptype != Constants.JPType.Bytes) return RTError.Type(
                data.get_start(), data.get_end(),
                "Expected bytearray",
                data.get_ctx()
        );
        byte[] msg = data.arr;

        try {
            out.writeInt(msg.length);
            out.write(msg);
        } catch (IOException e) {
            return RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    context
            );
        }

        return null;
    }

    public RTError send(Obj data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        byte[] msg;

        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(data);
            oos.flush();
            msg = bos.toByteArray();

            out.writeInt(msg.length);
            out.write(msg);
        } catch (IOException e) {
            return RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    context
            );
        }

        return null;
    }

    public RTResult receive() {
        try {
            int length = in.readInt();
            Obj res = new Null();
            if (length > 0) {
                byte [] message = new byte[length];
                in.readFully(message, 0, message.length);

                ByteArrayInputStream bis = new ByteArrayInputStream(message);
                ObjectInputStream ois = new ObjectInputStream(bis);

                Object obj = ois.readObject();
                if (!(obj instanceof Obj)) return new RTResult().failure(RTError.MalformedData(
                        pos_start, pos_end,
                        "Invalid data recieved..",
                        context
                ));

                res = (Obj) obj;
            }
            return new RTResult().success(res);
        } catch (IOException e) {
            return new RTResult().failure(RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    context
            ));
        } catch (ClassNotFoundException e) {
            return new RTResult().failure(RTError.MalformedData(
                    pos_start, pos_end,
                    "Invalid data recieved..",
                    context
            ));
        }
    }

    public RTResult receiveBytes() {
        try {
            return receiveBytes(in.readInt());
        } catch (IOException e) {
            return new RTResult().failure(RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    context
            ));
        }
    }

    public RTResult receiveBytes(int length) {
        try {
            Obj res = new Bytes(new byte[0]);
            if (length > 0) {
                byte [] message = new byte[length];
                in.readFully(message, 0, message.length);

                res = new Bytes(message);
            }
            return new RTResult().success(res);
        } catch (IOException e) {
            return new RTResult().failure(RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    context
            ));
        }
    }

}
