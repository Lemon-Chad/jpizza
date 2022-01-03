package lemon.jpizza.compiler.libraries;

import lemon.jpizza.Shell;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class JSystem extends JPExtension {
    @Override
    public String name() { return "sys"; }

    public JSystem(VM vm) {
        super(vm);
    }

    @NotNull
    private NativeResult processOut(Process pr) throws InterruptedException, IOException {
        pr.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            sb.append(line).append("\n");
        return Ok(sb.toString());
    }

    @Override
    public void setup() {
        // System Library
        // Quick Environment Variables
        func("os", (args) -> Ok(System.getProperty("os.name")), 0);
        func("home", (args) -> Ok(System.getProperty("user.home")), 0);

        // Execution
        func("execute", (args) -> {
            String cmd = args[0].asString();
            Runtime rt = Runtime.getRuntime();
            try {
                Process pr = rt.exec(cmd);
                return processOut(pr);
            } catch (Exception e) {
                return Err("Internal", e.getMessage());
            }
        }, List.of("String"));
        func("executeFloor", (args) -> {
            List<Value> args2 = args[0].asList();
            String[] cmd = new String[args2.size()];
            for (int i = 0; i < args2.size(); i++)
                cmd[i] = args[i].asString();

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.inheritIO();
            try {
                Process pr = pb.start();
                return processOut(pr);
            } catch (Exception e) {
                return Err("Internal", e.getMessage());
            }
        }, List.of("list"));

        // IO
        func("disableOut", (args) -> {
            Shell.logger.disableLogging();
            return Ok;
        }, 0);
        func("enableOut", (args) -> {
            Shell.logger.enableLogging();
            return Ok;
        }, 0);

        // VM Info
        var("jpv", vm.version);

        // Environment Variables
        func("envVarExists", (args) -> Ok(System.getenv(args[0].asString()) != null), List.of("String"));
        func("getEnvVar", (args) -> {
            String name = args[0].asString();
            String value = System.getenv(name);
            if (value == null)
                return Err("Scope", "Environment variable '" + name + "' does not exist");
            return Ok(value);
        }, List.of("String"));
        func("setEnvVar", (args) -> {
            String name = args[0].asString();
            String value = args[1].asString();
            System.setProperty(name, value);
            return Ok;
        }, List.of("String", "String"));

        // System Properties
        func("propExists", (args) -> Ok(System.getProperty(args[0].asString()) != null), List.of("String"));
        func("getProp", (args) -> {
            String name = args[0].asString();
            String value = System.getProperty(name);
            if (value == null)
                return Err("Scope", "System property '" + name + "' does not exist");
            return Ok(value);
        }, List.of("String"));
        func("setProp", (args) -> {
            String name = args[0].asString();
            String value = args[1].asString();
            System.setProperty(name, value);
            return Ok;
        }, List.of("String", "String"));

        // System
        func("exit", (args) -> {
            int code = args[0].asNumber().intValue();
            System.exit(code);
            return Ok;
        }, List.of("num"));
    }
}
