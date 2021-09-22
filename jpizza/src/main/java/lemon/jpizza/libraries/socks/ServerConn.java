package lemon.jpizza.libraries.socks;

import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.Position;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerConn extends GenConn {
    ServerSocket inner;
    Socket client;

    double id;

    public ServerConn(double iydee, Position ps, Position pe, Context ctx) {
        id = iydee;
        pos_start = ps; pos_end = pe;
        context = ctx;
    }

    public RTError host(ServerSocket sock) {
        inner = sock;
        return null;
    }

    public RTError conn() {
        try {
            client = inner.accept();
            out = new DataOutputStream(client.getOutputStream());
            in = new DataInputStream(client.getInputStream());
        } catch (IOException e) {
            return new RTError(
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
            return new RTError(
                    pos_start, pos_end,
                    e.toString(),
                    context
            );
        } return null;
    }

}
