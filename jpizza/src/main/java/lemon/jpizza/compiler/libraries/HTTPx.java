package lemon.jpizza.compiler.libraries;

import lemon.jpizza.Pair;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JNative;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class HTTPx extends JPExtension {
    @Override
    public String name() {
        return "httpx";
    }

    public HTTPx(VM vm) {
        super(vm);
    }

    private HttpURLConnection getConn(String url, Map<String, String> headers, String method) throws IOException {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod(method);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        conn.setConnectTimeout((int) Duration.of(10, ChronoUnit.SECONDS).getSeconds());
        conn.setReadTimeout((int) Duration.of(10, ChronoUnit.SECONDS).getSeconds());
        return conn;
    }

    private HttpURLConnection getConn(String url, Map<String, String> headers, String method, String body) throws IOException {
        HttpURLConnection conn = getConn(url, headers, method);
        conn.setDoOutput(true);
        conn.getOutputStream().write(body.getBytes());
        return conn;
    }

    private NativeResult handleConn(HttpURLConnection conn) {
        int status;
        String body;
        try {
            status = conn.getResponseCode();
            body = conn.getResponseMessage();
        } catch (IOException e) {
            return Err("Connection", e.getMessage());
        }
        Map<Value, Value> headersMap = new HashMap<>(conn.getHeaderFields().size());
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
            String key = entry.getKey();
            List<Value> values = new ArrayList<>();
            if (entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    values.add(new Value(value));
                }
            }
            headersMap.put(new Value(key), new Value(values));
        }

        Map<Value, Value> res = new HashMap<>();
        res.put(new Value("code"), new Value(status));
        res.put(new Value("body"), new Value(body));
        res.put(new Value("headers"), new Value(headersMap));

        return Ok(res);
    }

    private JNative.Method request(String method) {
        return args -> {
            String url = args[0].asString();

            Map<Value, Value> headers = args[1].asMap();
            Map<String, String> headerMap = new HashMap<>(headers.size());
            for (Map.Entry<Value, Value> entry : headers.entrySet()) {
                headerMap.put(entry.getKey().asString(), entry.getValue().asString());
            }

            HttpURLConnection conn;
            try {
                if (args.length == 3) {
                    conn = getConn(url, headerMap, method, args[2].asString());
                } else {
                    conn = getConn(url, headerMap, method);
                }
            } catch (IOException e) {
                return Err("Connection", e.getMessage());
            }

            return handleConn(conn);
        };
    }

    private void request(String method, boolean body) {
        String name = method.toLowerCase() + "Request";
        JNative.Method m = request(method);
        List<String> argTypes = new ArrayList<>();
        argTypes.add("String");
        argTypes.add("dict");
        if (body) {
            argTypes.add("String");
        }
        func(name, m, argTypes);
    }

    @Override
    public void setup() {
        request("GET", false);
        request("POST", true);
        request("PUT", true);
        request("DELETE", false);
        request("PATCH", true);
        request("HEAD", false);
        request("OPTIONS", false);
        request("TRACE", false);
        request("CONNECT", false);
    }
}
