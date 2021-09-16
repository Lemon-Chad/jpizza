package lemon.jpizza.Libraries;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Pair;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import static java.time.temporal.ChronoUnit.SECONDS;

@SuppressWarnings("unused")
public class HTTPLIB extends Library {

    public HTTPLIB(String name) { super(name); }

    public static void initialize() {
        initialize("httpx", HTTPLIB.class, new HashMap<>(){{
            put("getRequest", Arrays.asList("url", "params"));
            put("deleteRequest", Arrays.asList("url", "params"));
            put("postRequest", Arrays.asList("url", "params", "body"));
            put("putRequest", Arrays.asList("url", "params", "body"));
            put("patchRequest", Arrays.asList("url", "params", "body"));
            put("optionsRequest", Arrays.asList("url", "params", "body"));
            put("connectRequest", Arrays.asList("url", "params", "body"));
            put("traceRequest", Arrays.asList("url", "params", "body"));
            put("headRequest", Arrays.asList("url", "params", "body"));
        }});
    }

    /*
    Resources
    - https://www.baeldung.com/java-http-request
    - https://www.baeldung.com/java-9-http-client
     */

    public Pair< Map<String, String>, RTError > getParams(Context execCtx) {
        Obj params = ((Obj) execCtx.symbolTable.get("params")).dictionary();

        Pair< Map<String, String>, RTError > errX = new Pair<>(null, new RTError(
                params.get_start(), params.get_end(),
                "Expected dict<string, string>",
                params.get_ctx()
        ));

        if (params.jptype != Constants.JPType.Dict) return errX;

        Map<Obj, Obj> mp = ((Dict) params).trueValue();
        Map<String, String> args = new HashMap<>();
        Obj v;
        for (Obj k : mp.keySet()) {
            v = mp.get(k).astring();
            k = k.astring();

            if (k.jptype != Constants.JPType.String || v.jptype != Constants.JPType.String) return errX;

            args.put(((Str) k).trueValue(), ((Str) v).trueValue());
        }

        return new Pair<>(args, null);
    }

    public Pair< Pair< Map<String, String>, HttpRequest.Builder >, RTError > getBuilder(Context execCtx, String url, Obj urlObj) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return new Pair<>(null, new RTError(
                    urlObj.get_start(), urlObj.get_end(),
                    "URI Syntax Error",
                    urlObj.get_ctx()
            ));
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);

        var parerr = getParams(execCtx);
        if (parerr.b != null) return new Pair<>(null, parerr.b);
        var params = parerr.a;

        for (String k : params.keySet())
            builder = builder.header(k, params.get(k));
        return new Pair<>(new Pair<>(params, builder), null);
    }

    public RTResult buildRequest(HttpRequest request, Map<String, String> params) {
        HttpResponse<String> response;
        try {
            response = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .proxy(ProxySelector.getDefault())
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (IOException | InterruptedException e) {
            return new RTResult().failure(new RTError(
                    pos_start, pos_end,
                    e.toString(),
                    context
            ));
        }

        var headers = response.headers().map();

        Dict ret = new Dict(new HashMap<>());
        Dict heads = new Dict(new HashMap<>());

        List<String> v;
        List<Obj> vO;
        for (String k : params.keySet()) {
            v = headers.get(k);
            vO = new ArrayList<>();

            if (v != null)
                for (String i : v)
                    vO.add(new Str(i));

            heads.set(new Str(k), new PList(vO));
        }

        ret.set(new Str("code"), new Num(response.statusCode()));
        ret.set(new Str("body"), new Str(response.body()));
        ret.set(new Str("headers"), heads);

        return new RTResult().success(ret);
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_getRequest(Context execCtx) {
        Obj urlObj = ((Obj) execCtx.symbolTable.get("url")).astring();
        if (urlObj.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                urlObj.get_start(), urlObj.get_end(),
                "Expected URL string",
                urlObj.get_ctx()
        ));

        String url = ((Str) urlObj).trueValue();
        var buildData = getBuilder(execCtx, url, urlObj);
        if (buildData.b != null) return new RTResult().failure(buildData.b);

        Map<String, String> params = buildData.a.a;
        HttpRequest.Builder builder = buildData.a.b;

        HttpRequest request = builder
                                .timeout(Duration.of(10, SECONDS))
                                .GET().build();

        return buildRequest(request, params);
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_deleteRequest(Context execCtx) {
        Obj urlObj = ((Obj) execCtx.symbolTable.get("url")).astring();
        if (urlObj.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                urlObj.get_start(), urlObj.get_end(),
                "Expected URL string",
                urlObj.get_ctx()
        ));

        String url = ((Str) urlObj).trueValue();
        var buildData = getBuilder(execCtx, url, urlObj);
        if (buildData.b != null) return new RTResult().failure(buildData.b);

        Map<String, String> params = buildData.a.a;
        HttpRequest.Builder builder = buildData.a.b;

        HttpRequest request = builder
                .timeout(Duration.of(10, SECONDS))
                .DELETE().build();

        return buildRequest(request, params);
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_postRequest(Context execCtx) {
        Obj urlObj = ((Obj) execCtx.symbolTable.get("url")).astring();
        if (urlObj.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                urlObj.get_start(), urlObj.get_end(),
                "Expected URL string",
                urlObj.get_ctx()
        ));

        Obj bodyObj = ((Obj) execCtx.symbolTable.get("body")).astring();
        if (bodyObj.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                urlObj.get_start(), urlObj.get_end(),
                "Expected body string",
                urlObj.get_ctx()
        ));

        String url = ((Str) urlObj).trueValue();
        String body = ((Str) bodyObj).trueValue();

        var buildData = getBuilder(execCtx, url, urlObj);
        if (buildData.b != null) return new RTResult().failure(buildData.b);

        Map<String, String> params = buildData.a.a;
        HttpRequest.Builder builder = buildData.a.b;

        HttpRequest request = builder
                                .timeout(Duration.of(10, SECONDS))
                                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        return buildRequest(request, params);
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult otherRequest(String method, Context execCtx) {
        Obj urlObj = ((Obj) execCtx.symbolTable.get("url")).astring();
        if (urlObj.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                urlObj.get_start(), urlObj.get_end(),
                "Expected URL string",
                urlObj.get_ctx()
        ));

        Obj bodyObj = ((Obj) execCtx.symbolTable.get("body")).astring();
        if (bodyObj.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                urlObj.get_start(), urlObj.get_end(),
                "Expected body string",
                urlObj.get_ctx()
        ));

        String url = ((Str) urlObj).trueValue();
        String body = ((Str) bodyObj).trueValue();

        var buildData = getBuilder(execCtx, url, urlObj);
        if (buildData.b != null) return new RTResult().failure(buildData.b);

        Map<String, String> params = buildData.a.a;
        HttpRequest.Builder builder = buildData.a.b;

        HttpRequest request = builder
                .timeout(Duration.of(10, SECONDS))
                .method(method, HttpRequest.BodyPublishers.ofString(body)).build();

        return buildRequest(request, params);
    }

    public RTResult execute_patchRequest(Context execCtx) {
        return otherRequest("PATCH", execCtx);
    }

    public RTResult execute_traceRequest(Context execCtx) {
        return otherRequest("TRACE", execCtx);
    }

    public RTResult execute_optionsRequest(Context execCtx) {
        return otherRequest("OPTIONS", execCtx);
    }

    public RTResult execute_connectRequest(Context execCtx) {
        return otherRequest("CONNECT", execCtx);
    }

    public RTResult execute_headRequest(Context execCtx) {
        return otherRequest("HEAD", execCtx);
    }

    @SuppressWarnings("DuplicatedCode")
    public RTResult execute_putRequest(Context execCtx) {
        Obj urlObj = ((Obj) execCtx.symbolTable.get("url")).astring();
        if (urlObj.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                urlObj.get_start(), urlObj.get_end(),
                "Expected URL string",
                urlObj.get_ctx()
        ));

        Obj bodyObj = ((Obj) execCtx.symbolTable.get("body")).astring();
        if (bodyObj.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                urlObj.get_start(), urlObj.get_end(),
                "Expected body string",
                urlObj.get_ctx()
        ));

        String url = ((Str) urlObj).trueValue();
        String body = ((Str) bodyObj).trueValue();

        var buildData = getBuilder(execCtx, url, urlObj);
        if (buildData.b != null) return new RTResult().failure(buildData.b);

        Map<String, String> params = buildData.a.a;
        HttpRequest.Builder builder = buildData.a.b;

        HttpRequest request = builder
                .timeout(Duration.of(10, SECONDS))
                .PUT(HttpRequest.BodyPublishers.ofString(body)).build();

        return buildRequest(request, params);
    }

}
