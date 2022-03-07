package lemon.jpizza.compiler.libraries;

import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JNative;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
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

            BufferedReader br;
            if (100 <= conn.getResponseCode() && conn.getResponseCode() <= 399) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            }
            else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            body = sb.toString();
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
                }
                else {
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
        Type[] argTypes = new Type[body ? 3 : 2];
        argTypes[0] = Types.STRING;
        argTypes[1] = Types.DICT;
        if (body) {
            argTypes[2] = Types.STRING;
        }
        func(name, m, Types.DICT, argTypes);
    }

    @Override
    public void setup() {
        request("GET", false);
        request("DELETE", false);
        request("HEAD", false);
        request("OPTIONS", false);
        request("TRACE", false);
        request("CONNECT", false);
        request("POST", true);
        request("PUT", true);
        request("PATCH", true);
    }
}
