package lemon.jpizza.Libraries.HTTPretzel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import lemon.jpizza.Constants;
import lemon.jpizza.Errors.Error;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Generators.Interpreter;
import lemon.jpizza.Objects.Executables.BaseFunction;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Dict;
import lemon.jpizza.Objects.Primitives.Num;
import lemon.jpizza.Objects.Primitives.PList;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Pair;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JHandle implements HttpHandler {

    Function handle;
    RTResult res = new RTResult();

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
            res.failure(new RTError(
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
        if ("GET".equals(exchange.getRequestMethod())) {
            try {
                handleGetRequest(exchange, dat);
            } catch (IOException e) {
                res.failure(new RTError(
                        handle.pos_start, handle.pos_end,
                        "IOException occurred... " + e.toString(),
                        handle.context
                ));
                safeError(exchange, exchange.getResponseBody());
                return;
            }
        } else if ("POST".equals(exchange.getRequestMethod())) {
            try {
                handlePostRequest(exchange, dat);
            } catch (IOException e) {
                res.failure(new RTError(
                        handle.pos_start, handle.pos_end,
                        "IOException occurred... " + e.toString(),
                        handle.context
                ));
                safeError(exchange, exchange.getResponseBody());
                return;
            }
        }
        try {
            handleResponse(exchange, dat);
        } catch (IOException e) {
            res.failure(new RTError(
                    handle.pos_start, handle.pos_end,
                    "IOException occurred... " + e.toString(),
                    handle.context
            ));
            safeError(exchange, exchange.getResponseBody());
        }
    }

    private void safeError(HttpExchange exchange, OutputStream os) {
        try {
            logError(exchange, exchange.getResponseBody());
        } catch (IOException g) {
            Shell.logger.outln(g.toString());
        }
    }

    private void logError(HttpExchange exchange, OutputStream os) throws IOException {
        Shell.logger.fail(res.error.asString());

        exchange.sendResponseHeaders(404, res.error.details.length());
        os.write(res.error.details.getBytes());

        os.flush();
        os.close();
    }

    private void handleResponse(HttpExchange exchange, Dict dat) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();

        Obj response = res.register(handle.execute(Collections.singletonList(dat), new ArrayList<>(),
                new Interpreter()));
        res.register(Library.checkType(response, "dictionary", Constants.JPType.Dict));

        if (res.error != null) {
            logError(exchange, outputStream);
            return;
        }

        Dict data = (Dict) response;

        Pair<Obj, RTError> pr;
        pr = data.get(new Str("code"));
        if (pr.b != null) {
            res.failure(pr.b);
            logError(exchange, outputStream);
            return;
        }
        Obj code = pr.a;
        pr = data.get(new Str("header"));
        if (pr.b != null) {
            res.failure(pr.b);
            logError(exchange, outputStream);
            return;
        }
        Obj header = pr.a;

        if (code.jptype != Constants.JPType.Number) {
            res.failure(new RTError(
                    code.get_start(), code.get_end(),
                    "Expected number",
                    code.context
            ));
            logError(exchange, outputStream);
            return;
        }
        if (header.jptype != Constants.JPType.String) {
            res.failure(new RTError(
                    code.get_start(), code.get_end(),
                    "Expected String",
                    code.context
            ));
            logError(exchange, outputStream);
            return;
        }

        double cd = ((Num) code).trueValue();
        String hdr = ((Str) header).trueValue();

        exchange.sendResponseHeaders((int) cd, hdr.length());
        outputStream.write(hdr.getBytes());

        // this line is a must
        outputStream.flush();
        outputStream.close();
    }

    private void handleGetRequest(HttpExchange exchange, Dict dat) throws IOException {
        dat.set(new Str("uri"), new Str(exchange.getRequestURI().toString()));

        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);

        String body = br.readLine();
        dat.set(new Str("body"), new Str(String.valueOf(body)));
    }
    
    private void handlePostRequest(HttpExchange exchange, Dict dat) throws IOException {
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
