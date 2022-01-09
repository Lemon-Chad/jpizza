package lemon.jpizza.compiler.libraries.puddle;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerPDL {
    private final ServerSocket inner;
    private final int id;

    private ServerPDL(int port) throws IOException {
        inner = new ServerSocket(port);
        id = hashCode();
        PDL.servers.put(id, this);
    }

    public static int Create(int port) throws IOException {
        return new ServerPDL(port).id;
    }

    public int accept() throws IOException {
        return ClientPDL.Create(inner.accept());
    }

}
