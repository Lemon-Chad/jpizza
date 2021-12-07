package lemon.jpizza.libraries.socks;

import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.Position;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerConn extends GenConn {
    ServerSocket inner;
    Socket client;

    final double id;

    public ServerConn(double iydee, @NotNull Position ps, @NotNull Position pe, Context ctx) {
        id = iydee;
        pos_start = ps; pos_end = pe;
        context = ctx;
    }

    public void host(ServerSocket sock) {
        inner = sock;
    }

    public RTError conn() {
        try {
            client = inner.accept();
            out = new DataOutputStream(client.getOutputStream());
            in = new DataInputStream(client.getInputStream());
        } catch (IOException e) {
            return RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    context
            );
        }
        return null;
    }

    public RTError close() {
        try {
            client.close();
        } catch (IOException e) {
            return RTError.Internal(
                    pos_start, pos_end,
                    e.toString(),
                    context
            );
        } return null;
    }

}
