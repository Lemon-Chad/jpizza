package lemon.jpizza.Libraries.Socks;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Bytes;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerConn extends GenConn {
    ServerSocket inner;
    Socket client;
    DataOutputStream out;
    DataInputStream in;

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
