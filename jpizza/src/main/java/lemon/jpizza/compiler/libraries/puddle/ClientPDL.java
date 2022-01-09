package lemon.jpizza.compiler.libraries.puddle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientPDL {
    private final int id;
    private final OutputStream outputStream;
    private final InputStream inputStream;

    private ClientPDL(Socket socket) throws IOException {
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();

        id = hashCode();
        PDL.clients.put(id, this);
    }

    public static int Create(Socket socket) throws IOException {
        return new ClientPDL(socket).id;
    }

    public void write(byte[] data, int off, int len) throws IOException {
        outputStream.write(data, off, len);
    }

    public byte[] read(int off, int len) throws IOException {
        byte[] data = new byte[len];
        //noinspection ResultOfMethodCallIgnored
        inputStream.read(data, off, len);
        return data;
    }
}
