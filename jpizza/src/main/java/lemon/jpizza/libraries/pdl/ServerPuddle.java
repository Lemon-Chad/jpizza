package lemon.jpizza.libraries.pdl;

import lemon.jpizza.Pair;
import lemon.jpizza.Position;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerPuddle {
    ServerSocket inner;
    public int id;

    public ServerPuddle(int port) throws IOException {
        inner = new ServerSocket(port);

        id = hashCode();
        SafeSocks.serverCodes.put(id, this);
    }

    public static int create(int port) throws IOException {
        return new ServerPuddle(port).id;
    }

    public Pair<SocketPuddle, RTError> accept(Position pos_start, Position pos_end, Context context) {
        try {
            return new Pair<>(new SocketPuddle(inner.accept()), null);
        } catch (IOException e) {
            return new Pair<>(null, RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    context
            ));
        }
    }

}
