package lemon.jpizza.libraries.socks;

import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.Position;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.Socket;

public class ClientConn extends GenConn {
    Socket client;

    public ClientConn(@NotNull Position ps, @NotNull Position pe, Context ctx) {
        pos_start = ps; pos_end = pe;
        context = ctx;
    }

    public RTError conn(String host, double port) {
        try {
            client = new Socket(host, (int) port);
            out = new DataOutputStream(client.getOutputStream());
            in = new DataInputStream(client.getInputStream());
        } catch (IOException e) {
            return RTError.Internal(
                    pos_start, pos_end,
                    "IOException while connecting..",
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
