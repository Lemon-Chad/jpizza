package lemon.jpizza.compiler.libraries.puddle;

import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class PDL extends JPExtension {
    public static Map<Integer, ServerPDL> servers = new HashMap<>();
    public static Map<Integer, ClientPDL> clients = new HashMap<>();

    @Override
    public String name() {
        return "pdl";
    }

    public PDL(VM vm) {
        super(vm);
    }

    interface IOMethod {
        NativeResult call(Value[] args) throws IOException;
    }

    private void iofunc(String name, IOMethod method, Type returnType, Type... argTypes) {
        func(name, args -> {
            try {
                return method.call(args);
            } catch (IOException e) {
                return Err("Connection", e.getMessage());
            }
        }, returnType, argTypes);
    }

    interface ClientMethod {
        NativeResult call(ClientPDL client) throws IOException;
    }

    NativeResult asClient(int id, ClientMethod method) throws IOException {
        ClientPDL client = clients.get(id);
        if (client == null) {
            return Err("Client", "No such client");
        }
        return method.call(client);
    }

    interface ServerMethod {
        NativeResult call(ServerPDL server) throws IOException;
    }

    NativeResult asServer(int id, ServerMethod method) throws IOException {
        ServerPDL server = servers.get(id);
        if (server == null) {
            return Err("Server", "No such server");
        }
        return method.call(server);
    }

    @Override
    public void setup() {
        // Client
        iofunc("connect", args -> {
            String host = args[0].toString();
            int port = args[1].asNumber().intValue();
            Socket sock = new Socket(host, port);
            int id = ClientPDL.Create(sock);
            return Ok(id);
        }, Types.INT, Types.STRING, Types.INT);
        iofunc("write", args -> {
            int id = args[0].asNumber().intValue();
            byte[] data = args[1].asBytes();
            int offset = args[2].asNumber().intValue();
            int length = args[3].asNumber().intValue();
            return asClient(id, client -> {
                client.write(data, offset, length);
                return Ok;
            });
        }, Types.VOID, Types.INT, Types.BYTES, Types.INT, Types.INT);
        iofunc("read", args -> {
            int id = args[0].asNumber().intValue();
            int offset = args[1].asNumber().intValue();
            int length = args[2].asNumber().intValue();
            return asClient(id, client -> Ok(client.read(offset, length)));
        }, Types.BYTES, Types.INT, Types.INT, Types.INT);

        // Server
        iofunc("host", args -> {
            int port = args[0].asNumber().intValue();
            return Ok(ServerPDL.Create(port));
        }, Types.INT, Types.INT);
        iofunc("accept", args -> {
            int id = args[0].asNumber().intValue();
            return asServer(id, server -> Ok(server.accept()));
        }, Types.INT, Types.INT);
    }
}
