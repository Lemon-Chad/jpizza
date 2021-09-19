package lemon.jpizza.Libraries;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Primitives.Bool;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

@SuppressWarnings("unused")
public class SysLib extends Library {

    public SysLib(String name) { super(name, "sys"); }

    public static void initialize() {
        initialize("sys", SysLib.class, new HashMap<>(){{
            put("os", new ArrayList<>());
            put("disableOut", new ArrayList<>());
            put("enableOut", new ArrayList<>());
            put("jpv", new ArrayList<>());
            put("execute", Collections.singletonList("cmd"));
            put("getEnvVar", Collections.singletonList("variableName"));
            put("envVarExists", Collections.singletonList("variableName"));
            put("getProp", Collections.singletonList("prop"));
            put("setProp", Arrays.asList("prop", "propVal"));
            put("home", new ArrayList<>());
        }});
    }

    public RTResult execute_os(Context execCtx) {
        String os = System.getProperty("os.name");
        return new RTResult().success(new Str(os));
    }

    public RTResult execute_home(Context execCtx) {
        String home = System.getProperty("user.home");
        return new RTResult().success(new Str(home));
    }

    public RTResult execute_execute(Context execCtx) throws IOException, InterruptedException {
        Runtime run = Runtime.getRuntime();

        Process pr = run.exec(execCtx.symbolTable.get("cmd").toString());
        pr.waitFor();

        BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));

        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = buf.readLine()) != null)
            sb.append(line).append("\n");

        return new RTResult().success(new Str(sb.toString()));
    }

    public RTResult execute_disableOut(Context execCtx) {
        Shell.logger.disableLogging();
        return new RTResult().success(new Null());
    }
    public RTResult execute_enableOut(Context execCtx) {
        Shell.logger.enableLogging();
        return new RTResult().success(new Null());
    }

    public RTResult execute_jpv(Context execCtx) {
        return new RTResult().success(new Str("1.1.6"));
    }

    public RTResult execute_envVarExists(Context execCtx) {
        String envVar = System.getenv(execCtx.symbolTable.get("variableName").toString());
        if (envVar == null){
            return new RTResult().success(new Bool(false));
        }else{
            return new RTResult().success(new Bool(true));
        }
    }
    public RTResult execute_getEnvVar(Context execCtx) {
        String envVar = System.getenv(execCtx.symbolTable.get("variableName").toString());
        if (envVar == null){
            return new RTResult().failure(new RTError(pos_start, pos_end, "Variable Does Not Exist", context));
        }else{
            return new RTResult().success(new Str(envVar));
        }
    }

    public RTResult execute_getProp(Context execCtx) {
        String envVar = System.getProperty(execCtx.symbolTable.get("prop").toString());
        if (envVar == null){
            return new RTResult().failure(new RTError(pos_start, pos_end, "Property Does Not Exist", context));
        }else{
            return new RTResult().success(new Str(envVar));
        }
    }
    public RTResult execute_setProp(Context execCtx) {
        System.setProperty(execCtx.symbolTable.get("prop").toString(), execCtx.symbolTable.get("propVal").toString());
        return new RTResult().success(new Null());
    }

}
