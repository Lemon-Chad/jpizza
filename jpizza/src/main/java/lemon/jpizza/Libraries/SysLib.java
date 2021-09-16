package lemon.jpizza.Libraries;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@SuppressWarnings("unused")
public class SysLib extends Library {

    public SysLib(String name) { super(name); }

    public RTResult execute_os(Context execCtx) {
        String os = System.getProperty("os.name");
        return new RTResult().success(new Str(os));
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

    public RTResult execute_jpv(Context execCtx) {
        return new RTResult().success(new Str("1.1.6"));
    }

    public RTResult execute_enableOut(Context execCtx) {
        Shell.logger.enableLogging();
        return new RTResult().success(new Null());
    }

}
