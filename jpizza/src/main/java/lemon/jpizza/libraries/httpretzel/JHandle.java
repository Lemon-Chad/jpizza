package lemon.jpizza.libraries.httpretzel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lemon.jpizza.Constants;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.generators.Interpreter;
import lemon.jpizza.objects.executables.Function;
import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.Dict;
import lemon.jpizza.objects.primitives.PList;
import lemon.jpizza.objects.primitives.Str;
import lemon.jpizza.Pair;
import lemon.jpizza.results.RTResult;
import lemon.jpizza.Shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JHandle implements HttpHandler {

    final Function handle;
    final RTResult res = new RTResult();

    public JHandle(Function handle) {
        this.handle = handle;
    }

    /*
    handle("RequestMethod", {data});
    i.e
    handle("GET", {
        "uri": "xyz",
        "headers": "abc"
    });
    handle("POST", {
        "body": "abc",
        "uri": "xyz",
        "headers": "ijk"
    });

    Return with a type of {
        "code": 123,
        "response: "abc"
     }
     */

    @Override
    public void handle(HttpExchange exchange) {
        try {
            _handle(exchange);
        } catch (Exception e) {
            res.failure(RTError.Internal(
                    handle.pos_start, handle.pos_end,
                    "Exception occurred... " + e.toString(),
                    handle.context
            ));
            safeError(exchange, exchange.getResponseBody());
        }
    }
    public void _handle(HttpExchange exchange) {
        Dict dat = new Dict(new HashMap<>());
        dat.set(new Str("method"), new Str(exchange.getRequestMethod()));
        try {
            handleRequest(exchange, dat);
            handleResponse(exchange, dat);
        } catch (IOException e) {
            res.failure(RTError.Internal(
                    handle.pos_start, handle.pos_end,
                    "IOException occurred... " + e.toString(),
                    handle.context
            ));
            safeError(exchange, exchange.getResponseBody());
        }
    }

    private void safeError(HttpExchange exchange, OutputStream os) {
        try {
            logError(exchange, os);
        } catch (IOException g) {
            Shell.logger.warn(g.toString());
        }
    }

    private void logError(HttpExchange exchange, OutputStream os) throws IOException {
        Shell.logger.warn(res.error.asString());

        exchange.sendResponseHeaders(404, res.error.details.length());
        os.write(res.error.details.getBytes());

        os.flush();
        os.close();
    }

    private void handleResponse(HttpExchange exchange, Dict dat) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();

        Obj response = res.register(handle.execute(Collections.singletonList(dat), new ArrayList<>(), new HashMap<>(),
                new Interpreter()));
        if (res.error != null) {
            logError(exchange, outputStream);
            return;
        }
        res.register(Library.checkType(response, "dictionary", Constants.JPType.Dict));

        if (res.error != null) {
            logError(exchange, outputStream);
            return;
        }

        Pair<Obj, RTError> pr;
        pr = response.get(new Str("code"));
        if (pr.b != null) {
            res.failure(pr.b);
            logError(exchange, outputStream);
            return;
        }
        Obj code = pr.a;
        pr = response.get(new Str("header"));
        if (pr.b != null) {
            res.failure(pr.b);
            logError(exchange, outputStream);
            return;
        }
        Obj header = pr.a;

        if (code.jptype != Constants.JPType.Number) {
            res.failure(RTError.Type(
                    code.get_start(), code.get_end(),
                    "Expected number",
                    code.context
            ));
            logError(exchange, outputStream);
            return;
        }
        if (header.jptype != Constants.JPType.String) {
            res.failure(RTError.Type(
                    code.get_start(), code.get_end(),
                    "Expected String",
                    code.context
            ));
            logError(exchange, outputStream);
            return;
        }

        double cd = code.number;
        String hdr = header.string;

        exchange.sendResponseHeaders((int) cd, hdr.length());
        outputStream.write(hdr.getBytes());

        // this line is a must
        outputStream.flush();
        outputStream.close();
    }
    
    private void handleRequest(HttpExchange exchange, Dict dat) throws IOException {
        dat.set(new Str("uri"), new Str(exchange.getRequestURI().toString()));

        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        dat.set(new Str("body"), new Str(String.valueOf(br.readLine())));

        HashMap<Obj, Obj> hdrs = new HashMap<>();
        for (Map.Entry<String, List<String>> entry: exchange.getRequestHeaders().entrySet()) {
            List<Obj> lst = new ArrayList<>();
            for (String s: entry.getValue()) lst.add(new Str(s));

            hdrs.put(new Str(entry.getKey()), new PList(lst));
        }
        dat.set(new Str("headers"), new Dict(hdrs));
    }


}
