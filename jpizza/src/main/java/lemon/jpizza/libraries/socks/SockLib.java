package lemon.jpizza.libraries.socks;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.Pair;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.objects.primitives.Num;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.results.RTResult;

import java.io.*;
import java.net.ServerSocket;
import java.util.*;

@SuppressWarnings("unused")
public class SockLib extends Library {

    public SockLib(String name) { super(name, "sockets"); }

    public static void initialize() {
        initialize("sockets", SockLib.class, new HashMap<>(){{
            put("newServer", Collections.singletonList("port"));
            put("newClient", Arrays.asList("host", "port"));

            put("connect", Collections.singletonList("server"));

            put("serverSend", Arrays.asList("client", "msg"));
            put("serverSendBytes", Arrays.asList("client", "msg"));
            put("serverRecv", Collections.singletonList("client"));
            put("serverRecvBytes", Arrays.asList("client", "length"));
            put("serverRecvAllBytes", Collections.singletonList("client"));

            put("closeServerConnection", Collections.singletonList("client"));
            put("closeServer", Collections.singletonList("server"));

            put("clientSend", Arrays.asList("client", "msg"));
            put("clientSendBytes", Arrays.asList("client", "msg"));
            put("clientRecv", Collections.singletonList("client"));
            put("clientRecvBytes", Arrays.asList("client", "length"));
            put("clientRecvAllBytes", Collections.singletonList("client"));

            put("clientClose", Collections.singletonList("client"));
        }});
    }

    // Server Side

    static Map< Double, ServerSocket       > servers = new HashMap<>();
    static Map< ServerSocket,     List< ServerConn > > serverSocks = new HashMap<>();
    static Map< Double, ServerConn         > socks = new HashMap<>();

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_newServer(Context execCtx) {
        Obj p = ((Obj) execCtx.symbolTable.get("port")).number();
        if (p.jptype != Constants.JPType.Number || ((Num) p).floating) return new RTResult().failure(RTError.Type(
                p.get_start(), p.get_end(),
                "Expected integer",
                p.get_ctx()
        ));
        double port = ((Num) p).trueValue();
        if (1000 > port || port > 9999) return new RTResult().failure(RTError.Range(
                p.get_start(), p.get_end(),
                "Expected number between 1000 and 9999",
                p.get_ctx()
        ));
        double id = Math.random();

        ServerSocket sock;
        try {
            sock = new ServerSocket((int) port);
            servers.put(id, sock);
            serverSocks.put(sock, new ArrayList<>());
        } catch (IOException e) {
            return new RTResult().failure(RTError.Type(
                    pos_start, pos_end,
                    e.toString(),
                    context
            ));
        }

        return new RTResult().success(new Num(id));
    }

    public Pair< ServerSocket, RTError > getServer(Context execCtx) {
        Obj serv = (Obj) execCtx.symbolTable.get("server");
        if (serv.jptype != Constants.JPType.Number) return new Pair<>(null, RTError.Type(
                serv.get_start(), serv.get_end(),
                "Expected a number",
                serv.get_ctx()
        ));

        double id = ((Num) serv).trueValue();
        ServerSocket sock = servers.get(id);

        if (sock == null) return new Pair<>(null, RTError.InvalidArgument(
                serv.get_start(), serv.get_end(),
                "Invalid ID",
                serv.get_ctx()
        ));

        return new Pair<>(sock, null);
    }
    public Pair< ServerConn, RTError > getServerConn(Context execCtx) {
        Obj serv = (Obj) execCtx.symbolTable.get("client");
        if (serv.jptype != Constants.JPType.Number) return new Pair<>(null, RTError.Type(
                serv.get_start(), serv.get_end(),
                "Expected a number",
                serv.get_ctx()
        ));

        double id = ((Num) serv).trueValue();
        ServerConn conn = socks.get(id);

        if (conn == null) return new Pair<>(null, RTError.InvalidArgument(
                serv.get_start(), serv.get_end(),
                "Invalid ID",
                serv.get_ctx()
        ));

        return new Pair<>(conn, null);
    }

    public RTResult execute_connect(Context execCtx) {
        Pair< ServerSocket, RTError > s = getServer(execCtx);
        if (s.b != null) return new RTResult().failure(s.b);
        ServerSocket sock = s.a;

        double id = Math.random();
        ServerConn conn = new ServerConn(id, pos_start, pos_end, context);

        RTError e = conn.host(sock);
        if (e != null) return new RTResult().failure(e);

        e = conn.conn();
        if (e != null) return new RTResult().failure(e);

        socks.put(id, conn);
        serverSocks.get(sock).add(conn);
        return new RTResult().success(new Num(id));
    }

    public RTResult execute_serverSend(Context execCtx) {
        Pair< ServerConn, RTError > c = getServerConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ServerConn conn = c.a;

        Obj msg = (Obj) execCtx.symbolTable.get("msg");
        RTError e = conn.send(msg);

        if (e != null) return new RTResult().failure(e);

        return new RTResult().success(new Null());
    }

    public RTResult execute_serverSendBytes(Context execCtx) {
        Pair< ServerConn, RTError > c = getServerConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ServerConn conn = c.a;

        Obj msg = (Obj) execCtx.symbolTable.get("msg");
        RTError e = conn.sendBytes(msg);

        if (e != null) return new RTResult().failure(e);

        return new RTResult().success(new Null());
    }

    public RTResult execute_serverRecv(Context execCtx) {
        Pair< ServerConn, RTError > c = getServerConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ServerConn conn = c.a;

        return conn.receive();
    }

    public RTResult execute_serverRecvAllBytes(Context execCtx) {
        Pair< ServerConn, RTError > c = getServerConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ServerConn conn = c.a;

        return conn.receiveBytes();
    }

    public RTResult execute_serverRecvBytes(Context execCtx) {
        Pair< ServerConn, RTError > c = getServerConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ServerConn conn = c.a;

        RTResult res = new RTResult();
        Obj length = res.register(checkPosInt(execCtx.symbolTable.get("length")));
        if (res.error != null) return res;

        return conn.receiveBytes((int) ((Num) length).trueValue());
    }

    public RTResult execute_closeServerConnection(Context execCtx) {
        Pair< ServerConn, RTError > c = getServerConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ServerConn conn = c.a;

        RTError e = conn.close();
        if (e != null) return new RTResult().failure(e);
        socks.remove(conn.id);

        return new RTResult().success(new Null());
    }

    public RTResult execute_closeServer(Context execCtx) {
        Pair< ServerSocket, RTError > s = getServer(execCtx);
        if (s.b != null) return new RTResult().failure(s.b);
        ServerSocket sock = s.a;

        for (var conn : serverSocks.get(sock)) {
            RTError e = conn.close();
            if (e != null) return new RTResult().failure(e);
            socks.remove(conn.id);
        }
        try {
            sock.close();
        } catch (IOException e) {
                return new RTResult().failure(RTError.Internal(
                        pos_start, pos_end,
                        e.toString(),
                        context
                ));
        }

        return new RTResult().success(new Null());
    }

    // Client Side

    static Map< Double, ClientConn > clients = new HashMap<>();

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_newClient(Context execCtx) {
        Obj p = ((Obj) execCtx.symbolTable.get("port")).number();
        if (p.jptype != Constants.JPType.Number || ((Num) p).floating) return new RTResult().failure(RTError.Type(
                p.get_start(), p.get_end(),
                "Expected integer",
                p.get_ctx()
        ));
        double port = ((Num) p).trueValue();
        if (1000 > port || port > 9999) return new RTResult().failure(RTError.Range(
                p.get_start(), p.get_end(),
                "Expected number between 1000 and 9999",
                p.get_ctx()
        ));

        Obj h = ((Obj) execCtx.symbolTable.get("host")).astring();
        if (h.jptype != Constants.JPType.String) return new RTResult().failure(RTError.Type(
                h.get_start(), h.get_end(),
                "Expected sString",
                h.get_ctx()
        ));
        String host = ((Str) h).trueValue();

        double id = Math.random();

        ClientConn conn;
        conn = new ClientConn(id, pos_start, pos_end, context);

        RTError e = conn.conn(host, port);
        if (e != null) return new RTResult().failure(e);

        clients.put(id, conn);
        return new RTResult().success(new Num(id));
    }

    public Pair< ClientConn, RTError > getConn(Context execCtx) {
        Obj serv = (Obj) execCtx.symbolTable.get("client");
        if (serv.jptype != Constants.JPType.Number) return new Pair<>(null, RTError.Type(
                serv.get_start(), serv.get_end(),
                "Expected a number",
                serv.get_ctx()
        ));

        double id = ((Num) serv).trueValue();
        ClientConn conn = clients.get(id);

        if (conn == null) return new Pair<>(null, RTError.InvalidArgument(
                serv.get_start(), serv.get_end(),
                "Invalid ID",
                serv.get_ctx()
        ));

        return new Pair<>(conn, null);
    }

    public RTResult execute_clientSend(Context execCtx) {
        var c = getConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ClientConn conn = c.a;

        Obj msg = (Obj) execCtx.symbolTable.get("msg");

        RTError e = conn.send(msg);
        if (e != null) return new RTResult().failure(e);

        return new RTResult().success(new Null());
    }

    public RTResult execute_clientSendBytes(Context execCtx) {
        var c = getConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ClientConn conn = c.a;

        Obj msg = (Obj) execCtx.symbolTable.get("msg");

        RTError e = conn.sendBytes(msg);
        if (e != null) return new RTResult().failure(e);

        return new RTResult().success(new Null());
    }

    public RTResult execute_clientRecv(Context execCtx) {
        var c = getConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ClientConn conn = c.a;

        return conn.receive();
    }

    public RTResult execute_clientRecvAllBytes(Context execCtx) {
        var c = getConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ClientConn conn = c.a;

        return conn.receiveBytes();
    }

    public RTResult execute_clientRecvBytes(Context execCtx) {
        var c = getConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ClientConn conn = c.a;

        RTResult res = new RTResult();
        Obj length = res.register(checkPosInt(execCtx.symbolTable.get("length")));
        if (res.error != null) return res;

        return conn.receiveBytes((int) ((Num) length).trueValue());
    }

    public RTResult execute_clientClose(Context execCtx) {
        var c = getConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ClientConn conn = c.a;

        RTError e = conn.close();
        if (e != null) return new RTResult().failure(e);

        return new RTResult().success(new Null());
    }

}
