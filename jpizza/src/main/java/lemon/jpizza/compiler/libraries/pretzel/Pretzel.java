package lemon.jpizza.compiler.libraries.pretzel;

import com.sun.net.httpserver.HttpServer;
import lemon.jpizza.compiler.values.functions.JClosure;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

public class Pretzel extends JPExtension {
    private HttpServer server;

    @Override
    public String name() {
        return "pretzel";
    }

    public Pretzel(VM vm) {
        super(vm);
    }

    @Override
    public void setup() {
        func("init", args -> {
            String host = args[0].toString();
            int port = args[1].asNumber().intValue();

            try {
                server = HttpServer.create(new InetSocketAddress(host, port), 0);
            } catch (IOException e) {
                return Err("Host", e.getMessage());
            }

            return Ok;
        }, Arrays.asList("String", "num"));
        func("route", args -> {
            if (server == null) {
                return Err("Init", "Server not initialized");
            }

            String path = args[0].toString();
            JClosure handler = args[1].asClosure();

            server.createContext(path, new Handle(path, handler));

            return Ok;
        }, Arrays.asList("String", "function"));
        func("start", args -> {
            if (server == null) {
                return Err("Init", "Server not initialized");
            }

            server.start();

            return Ok;
        }, 0);
    }
}
