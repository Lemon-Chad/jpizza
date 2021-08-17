package lemon.jpizza.Libraries;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Objects.Primitives.Bytes;
import lemon.jpizza.Pair;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.Num;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Position;
import lemon.jpizza.Results.RTResult;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class SockLib extends Library {

    public SockLib(String name) { super(name); }

    // Server Side

    @SuppressWarnings("DuplicatedCode")
    static class ServerConn {
        ServerSocket inner;
        Socket client;
        DataOutputStream out;
        DataInputStream in;

        Position pos_start, pos_end;
        Context context;

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

        public RTError sendBytes(Obj data) {
            if (data.jptype != Constants.JPType.Bytes) return new RTError(
                    data.get_start(), data.get_end(),
                    "Expected bytearray",
                    data.get_ctx()
            );
            byte[] msg = ((Bytes) data).arr;

            try {
                out.writeInt(msg.length);
                out.write(msg);
            } catch (IOException e) {
                return new RTError(
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
                return new RTError(
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
                    if (!(obj instanceof Obj)) return new RTResult().failure(new RTError(
                            pos_start, pos_end,
                            "Invalid data recieved..",
                            context
                    ));

                    res = (Obj) obj;
                }
                return new RTResult().success(res);
            } catch (IOException e) {
                return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        e.toString(),
                        context
                ));
            } catch (ClassNotFoundException e) {
                return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        "Invalid data recieved..",
                        context
                ));
            }
        }

        public RTResult receiveBytes() {
            try {
                int length = in.readInt();
                Obj res = new Bytes(new byte[0]);
                if (length > 0) {
                    byte [] message = new byte[length];
                    in.readFully(message, 0, message.length);

                    res = new Bytes(message);
                }
                return new RTResult().success(res);
            } catch (IOException e) {
                return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        e.toString(),
                        context
                ));
            }
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

    static Map< Double, ServerSocket       > servers = new HashMap<>();
    static Map< ServerSocket,     List< ServerConn > > serverSocks = new HashMap<>();
    static Map< Double, ServerConn         > socks = new HashMap<>();

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_newServer(Context execCtx) {
        Obj p = ((Obj) execCtx.symbolTable.get("port")).number();
        if (p.jptype != Constants.JPType.Number || ((Num) p).floating) return new RTResult().failure(new RTError(
                p.get_start(), p.get_end(),
                "Expected integer",
                p.get_ctx()
        ));
        double port = ((Num) p).trueValue();
        if (1000 > port || port > 9999) return new RTResult().failure(new RTError(
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
            return new RTResult().failure(new RTError(
                    pos_start, pos_end,
                    e.toString(),
                    context
            ));
        }

        return new RTResult().success(new Num(id));
    }

    public Pair< ServerSocket, RTError > getServer(Context execCtx) {
        Obj serv = (Obj) execCtx.symbolTable.get("server");
        if (serv.jptype != Constants.JPType.Number) return new Pair<>(null, new RTError(
                serv.get_start(), serv.get_end(),
                "Expected a number",
                serv.get_ctx()
        ));

        double id = ((Num) serv).trueValue();
        ServerSocket sock = servers.get(id);

        if (sock == null) return new Pair<>(null, new RTError(
                serv.get_start(), serv.get_end(),
                "Invalid ID",
                serv.get_ctx()
        ));

        return new Pair<>(sock, null);
    }
    public Pair< ServerConn, RTError > getServerConn(Context execCtx) {
        Obj serv = (Obj) execCtx.symbolTable.get("client");
        if (serv.jptype != Constants.JPType.Number) return new Pair<>(null, new RTError(
                serv.get_start(), serv.get_end(),
                "Expected a number",
                serv.get_ctx()
        ));

        double id = ((Num) serv).trueValue();
        ServerConn conn = socks.get(id);

        if (conn == null) return new Pair<>(null, new RTError(
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

    public RTResult execute_serverRecvBytes(Context execCtx) {
        Pair< ServerConn, RTError > c = getServerConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ServerConn conn = c.a;

        return conn.receiveBytes();
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
                return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        e.toString(),
                        context
                ));
        }

        return new RTResult().success(new Null());
    }

    // Client Side

    @SuppressWarnings("DuplicatedCode")
    static class ClientConn {
        Socket client;
        DataOutputStream out;
        DataInputStream in;

        Position pos_start, pos_end;
        Context context;

        double id;

        public ClientConn(double iydee, Position ps, Position pe, Context ctx) {
            id = iydee;
            pos_start = ps; pos_end = pe;
            context = ctx;
        }

        public RTError conn(String host, double port) {
            try {
                client = new Socket(host, (int) port);
                out = new DataOutputStream(client.getOutputStream());
                in = new DataInputStream(client.getInputStream());
            } catch (IOException e) {
                return new RTError(
                        pos_start, pos_end,
                        "IOException while connecting..",
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
                return new RTError(
                        pos_start, pos_end,
                        e.toString(),
                        context
                );
            }

            return null;
        }

        public RTError sendBytes(Obj data) {
            if (data.jptype != Constants.JPType.Bytes) return new RTError(
                    data.get_start(), data.get_end(),
                    "Expected bytearray",
                    data.get_ctx()
            );
            byte[] msg = ((Bytes) data).arr;

            try {
                out.writeInt(msg.length);
                out.write(msg);
            } catch (IOException e) {
                return new RTError(
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
                    if (!(obj instanceof Obj)) return new RTResult().failure(new RTError(
                            pos_start, pos_end,
                            "Invalid data recieved..",
                            context
                    ));

                    res = (Obj) obj;
                }
                return new RTResult().success(res);
            } catch (IOException e) {
                return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        e.toString(),
                        context
                ));
            } catch (ClassNotFoundException e) {
                return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        "Invalid data recieved..",
                        context
                ));
            }
        }

        public RTResult receiveBytes() {
            try {
                int length = in.readInt();
                Obj res = new Bytes(new byte[0]);
                if (length > 0) {
                    byte [] message = new byte[length];
                    in.readFully(message, 0, message.length);

                    res = new Bytes(message);
                }
                return new RTResult().success(res);
            } catch (IOException e) {
                return new RTResult().failure(new RTError(
                        pos_start, pos_end,
                        e.toString(),
                        context
                ));
            }
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

    static Map< Double, ClientConn > clients = new HashMap<>();

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_newClient(Context execCtx) {
        Obj p = ((Obj) execCtx.symbolTable.get("port")).number();
        if (p.jptype != Constants.JPType.Number || ((Num) p).floating) return new RTResult().failure(new RTError(
                p.get_start(), p.get_end(),
                "Expected integer",
                p.get_ctx()
        ));
        double port = ((Num) p).trueValue();
        if (1000 > port || port > 9999) return new RTResult().failure(new RTError(
                p.get_start(), p.get_end(),
                "Expected number between 1000 and 9999",
                p.get_ctx()
        ));

        Obj h = ((Obj) execCtx.symbolTable.get("host")).astring();
        if (h.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                h.get_start(), h.get_end(),
                "Expected string",
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
        if (serv.jptype != Constants.JPType.Number) return new Pair<>(null, new RTError(
                serv.get_start(), serv.get_end(),
                "Expected a number",
                serv.get_ctx()
        ));

        double id = ((Num) serv).trueValue();
        ClientConn conn = clients.get(id);

        if (conn == null) return new Pair<>(null, new RTError(
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

    public RTResult execute_clientRecvBytes(Context execCtx) {
        var c = getConn(execCtx);
        if (c.b != null) return new RTResult().failure(c.b);
        ClientConn conn = c.a;

        return conn.receiveBytes();
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
