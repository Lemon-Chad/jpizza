package lemon.jpizza.libraries.httpretzel;

import com.sun.net.httpserver.HttpServer;
import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.executables.Function;
import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Null;
import lemon.jpizza.results.RTResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

@SuppressWarnings("unused")
public class HTTPretzel extends Library {
    static boolean running = false;
    static HttpServer server;

    public HTTPretzel(String name) {
        super(name, "pretzel");
    }

    public static void initialize() {
        initialize("pretzel", HTTPretzel.class, new HashMap<>(){{
            put("init", Arrays.asList("host", "addr"));
            put("route", Arrays.asList("route", "func"));
            put("start", new ArrayList<>());
        }});
    }

    public RTResult inited() {
        if (server == null) return new RTResult().failure(RTError.Init(
                pos_start, pos_end,
                "Server not initialized",
                context
        ));
        return new RTResult();
    }

    public RTResult execute_init(Context execCtx) {
        RTResult res = new RTResult();

        Obj hst = res.register(checkType(execCtx.symbolTable.get("host"), "String", Constants.JPType.String));
        Obj adr = res.register(checkPosInt(execCtx.symbolTable.get("addr")));

        if (res.error != null) return res;

        double addr = adr.number;
        String host = hst.string;

        try {
            server = HttpServer.create(new InetSocketAddress(host, (int) addr), 0);
        } catch (IOException e) {
            return res.failure(RTError.Internal(
                    pos_start, pos_end,
                    String.format("An IOException occurred..\n%s", e),
                    execCtx
            ));
        }

        return res.success(new Null());
    }

    public RTResult execute_route(Context execCtx) {
        RTResult res = new RTResult();
        res.register(inited());
        if (res.error != null) return res;

        Obj rte = res.register(checkType(execCtx.symbolTable.get("route"), "String", Constants.JPType.String));
        Obj fnc = res.register(checkFunction(execCtx.symbolTable.get("func")));
        if (res.error != null) return res;

        server.createContext(rte.string, new JHandle((Function) fnc));

        return res.success(new Null());
    }

    public RTResult execute_start(Context execCtx) {
        RTResult res = new RTResult();
        res.register(inited());
        if (res.error != null) return res;

        server.start();
        return res.success(new Null());
    }

}
