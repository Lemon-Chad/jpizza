package lemon.jpizza.compiler.libraries.pretzel;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import lemon.jpizza.Shell;
import lemon.jpizza.compiler.values.functions.JClosure;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.NativeResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Handle implements HttpHandler {
    private final String route;
    private final JClosure handle;

    public Handle(String route, JClosure handle) {
        this.route = route;
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
        "response": "abc"
     }
     */

    private void logError(HttpExchange exchange, OutputStream outputStream, IOException e) throws IOException {
        logError(exchange, outputStream, "Error handling request (" + route + "): " + e.getMessage());
    }

    private void logError(HttpExchange exchange, OutputStream outputStream, NativeResult result) throws IOException {
        logError(exchange, outputStream, result.reason());
    }

    private void logError(HttpExchange exchange, OutputStream outputStream, String response) throws IOException {
        Shell.logger.warn(response);
        exchange.sendResponseHeaders(404, response.length());
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
    }

    private void handleError(HttpExchange exchange, IOException e) {
        try {
            logError(exchange, exchange.getResponseBody(), e);
        } catch (IOException g) {
            Shell.logger.warn("Error logging error (" + route + "): " + g.getMessage());
        }
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            unsafeHandle(exchange);
        } catch (IOException e) {
            handleError(exchange, e);
        }
    }

    private void unsafeHandle(HttpExchange exchange) throws IOException {
        Map<Value, Value> data = new HashMap<>();
        data.put(new Value("method"), new Value(exchange.getRequestMethod()));
        handleRequest(exchange, data);
        handleResponse(exchange, data);
    }

    private void handleResponse(HttpExchange exchange, Map<Value, Value> data) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();

        NativeResult result = VM.Run(handle, new Value[]{ new Value(data) });
        if (!result.ok()) {
            logError(exchange, outputStream, result);
            return;
        }
        Value val = result.value();
        if (!val.isMap) {
            logError(exchange, outputStream, "Response must be a map");
        }

        Map<Value, Value> response = val.asMap();

        Value code = null;
        Value header = null;

        for (Map.Entry<Value, Value> entry : response.entrySet()) {
            if (entry.getKey().isString && entry.getKey().asString().equals("code")) {
                code = entry.getValue();
            }
            else if (entry.getKey().isString && entry.getKey().asString().equals("header")) {
                header = entry.getValue();
            }
        }

        if (code == null || !code.isNumber) {
            logError(exchange, outputStream, "Invalid response: missing code");
            return;
        }

        if (header == null || !header.isString) {
            logError(exchange, outputStream, "Invalid response: missing header");
            return;
        }

        int codeInt = code.asNumber().intValue();
        String headerString = header.asString();

        exchange.sendResponseHeaders(codeInt, headerString.length());
        outputStream.write(headerString.getBytes());

        outputStream.flush();
        outputStream.close();
    }

    private void handleRequest(HttpExchange exchange, Map<Value, Value> data) throws IOException {
        data.put(new Value("uri"), new Value(exchange.getRequestURI().toString()));

        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody());
        BufferedReader br = new BufferedReader(reader);
        data.put(new Value("body"), new Value(br.readLine()));

        Map<Value, Value> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
            List<Value> values = new ArrayList<>();
            for (String value : entry.getValue()) {
                values.add(new Value(value));
            }
            headers.put(new Value(entry.getKey()), new Value(values));
        }
        data.put(new Value("headers"), new Value(headers));
    }

}
