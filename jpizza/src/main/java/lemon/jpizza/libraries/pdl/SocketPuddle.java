package lemon.jpizza.libraries.pdl;

import lemon.jpizza.Position;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Bytes;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketPuddle {
    final Socket inner;
    public int id;
    OutputStream outputStream;
    InputStream inputStream;

    public SocketPuddle(Socket socket) throws IOException {
        inner = socket;
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();

        id = hashCode();
        SafeSocks.clientCodes.put(id, this);
    }

    public static int create(Socket socket) throws IOException {
        return new SocketPuddle(socket).id;
    }

    public RTResult write(Obj bytes, int off, int len,
                          Position pos_start, Position pos_end, Context ctx) {
        try {
            outputStream.write(bytes.arr, off, len);
        } catch (IOException e) {
            return new RTResult().failure(RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    ctx
            ));
        }
        return new RTResult().success(new Null());
    }

    public RTResult read(int off, int len,
                         Position pos_start, Position pos_end, Context ctx) {
        byte[] bytes = new byte[len];

        try {
            //noinspection ResultOfMethodCallIgnored
            inputStream.read(bytes, off, len);
        } catch (IOException e) {
            return new RTResult().failure(RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    ctx
            ));
        }

        return new RTResult().success(new Bytes(bytes)
                                            .set_pos(pos_start, pos_end)
                                            .set_context(ctx));
    }

    public RTResult close(Position pos_start, Position pos_end, Context context) {
        try {
            inner.close();
            SafeSocks.clientCodes.remove(id);
        } catch (IOException e) {
            return new RTResult().failure(RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    context
            ));
        } return new RTResult().success(new Null());
    }
}
