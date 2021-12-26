package lemon.jpizza.libraries.pdl;

import lemon.jpizza.JPType;
import lemon.jpizza.Pair;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.Error;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.primitives.Num;
import lemon.jpizza.results.RTResult;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

@SuppressWarnings("unused")
public class SafeSocks extends Library {
    public SafeSocks(String name) {
        super(name, "pdl");
    }

    public static void initialize() {
        initialize("pdl", SafeSocks.class, new HashMap<>(){{
            put("write", Arrays.asList("id", "bytes", "offset", "len"));
            put("read", Arrays.asList("id", "offset", "len"));
            put("connect", Arrays.asList("host", "port"));
            put("host", Collections.singletonList("port"));
            put("accept", Collections.singletonList("id"));
        }});
    }

    public static final Map<Integer, ServerPuddle> serverCodes = new HashMap<>();
    public static final Map<Integer, SocketPuddle> clientCodes = new HashMap<>();

    public RTResult execute_host(Context execCtx) {
        RTResult res = new RTResult();
        Obj port = res.register(checkPosInt(execCtx.symbolTable.get("port")));
        if (res.error != null) return res;

        try {
            int id = ServerPuddle.create(Double.valueOf(port.number).intValue());
            return res.success(new Num(id));
        } catch (Exception e) {
            return res.failure(RTError.Internal(
                    get_start(), get_end(),
                    e.toString(),
                    execCtx
            ));
        }
    }

    public RTResult execute_accept(Context execCtx) {
        RTResult res = new RTResult();
        Obj id = res.register(checkInt(execCtx.symbolTable.get("id")));
        if (res.error != null) return res;

        if (!serverCodes.containsKey(Double.valueOf(id.number).intValue())) return res.failure(RTError.Scope(
                get_start(), get_end(),
                "Server does not exist",
                execCtx
        ));
        ServerPuddle puddle = serverCodes.get(Double.valueOf(id.number).intValue());

        Pair<SocketPuddle, RTError> pair = puddle.accept(get_start(), get_end(), execCtx);
        if (pair.b != null) return res.failure(pair.b);
        return res.success(new Num(pair.a.id));
    }

    public RTResult execute_connect(Context execCtx) {
        RTResult res = new RTResult();
        Obj port = res.register(checkPosInt(execCtx.symbolTable.get("port")));
        if (res.error != null) return res;

        String host = execCtx.symbolTable.get("host").toString();
        try {
            Socket sock = new Socket(host, Double.valueOf(port.number).intValue());
            int id = SocketPuddle.create(sock);
            return res.success(new Num(id));
        } catch (IOException e) {
            return res.failure(RTError.Internal(
                    get_start(), get_end(),
                    e.toString(),
                    execCtx
            ));
        }
    }

    static class SocketData {
        public final int offset;
        public final int len;
        public final SocketPuddle puddle;
        public SocketData(int offset, int len, SocketPuddle puddle) {
            this.offset = offset;
            this.len = len;
            this.puddle = puddle;
        }
    }

    public Pair<SocketData, Error> data(Context execCtx) {
        RTResult res = new RTResult();

        Obj num = res.register(checkInt(execCtx.symbolTable.get("id")));
        if (res.error != null) return new Pair<>(null, res.error);

        int id = Double.valueOf(num.number).intValue();
        if (!clientCodes.containsKey(id)) return new Pair<>(null, RTError.Scope(
                num.get_start(), num.get_end(),
                "Socket does not exist",
                context
        ));
        SocketPuddle puddle = clientCodes.get(id);

        Obj offs = res.register(checkInt(execCtx.symbolTable.get("offset")));
        if (res.error != null) return new Pair<>(null, res.error);
        Obj len = res.register(checkInt(execCtx.symbolTable.get("len")));
        if (res.error != null) return new Pair<>(null, res.error);

        return new Pair<>(new SocketData(Double.valueOf(offs.number).intValue(), Double.valueOf(len.number).intValue(), puddle), null);
    }

    public RTResult execute_write(Context execCtx) {
        RTResult res = new RTResult();

        Pair<SocketData, Error> pair = data(execCtx);
        if (pair.b != null) return res.failure(pair.b);
        SocketData data = pair.a;

        Obj bytes = res.register(checkType(execCtx.symbolTable.get("bytes"), "bytearray", JPType.Bytes));
        if (res.error != null) return res;
        return data.puddle.write(bytes, data.offset, data.len, pos_start, pos_end, context);
    }

    public RTResult execute_read(Context execCtx) {
        RTResult res = new RTResult();

        Pair<SocketData, Error> pair = data(execCtx);
        if (pair.b != null) return res.failure(pair.b);
        SocketData data = pair.a;

        return data.puddle.read(data.offset, data.len, pos_start, pos_end, context);
    }

}
