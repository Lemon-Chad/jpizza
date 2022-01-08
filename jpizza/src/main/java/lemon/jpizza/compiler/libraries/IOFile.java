package lemon.jpizza.compiler.libraries;

import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static lemon.jpizza.Constants.readString;

public class IOFile extends JPExtension {
    @Override
    public String name() { return "iofile"; }

    public IOFile(VM vm) {
        super(vm);
    }

    private String dir(Value val) {
        String dir = val.asString();
        //noinspection RegExpRedundantEscape
        if (!dir.matches("^([A-Z]:|\\.|\\/|\\\\).*"))
            dir = System.getProperty("user.dir") + "/" + dir;
        return dir;
    }

    @Override
    public void setup() {
        // String Data
        func("readFile", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            if (!file.exists())
                return Err("Imaginary File", "File not found");

            try {
                return Ok(readString(Paths.get(path)));
            } catch (IOException e) {
                return Err("Internal", "Could not load file (" + e.getMessage() + ")");
            }
        }, Collections.singletonList("String"));
        func("writeFile", (args) -> {
            String path = dir(args[0]);
            String data = args[1].asString();
            File file = new File(path);
            try {
                boolean created = file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.write(data);
                writer.close();
                return Ok(created);
            } catch (IOException e) {
                return Err("Internal", "Could not write file (" + e.getMessage() + ")");
            }
        }, Arrays.asList("String", "any"));

        // File Creation
        func("fileExists", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            return Ok(file.exists());
        }, Collections.singletonList("String"));
        func("makeDirs", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            return Ok(file.mkdirs());
        }, Collections.singletonList("String"));
        func("deleteFile", (args) -> {
            String path = dir(args[0]);
            if (!Files.exists(Paths.get(path))) {
                return Err("Imaginary File", "File not found");
            }
            boolean isDir = Files.isDirectory(Paths.get(path));
            if (isDir) {
                try {
                    FileUtils.deleteDirectory(new File(path));
                    return Ok(true);
                } catch (IOException e) {
                    return Err("Internal", "Could not delete directory (" + e.getMessage() + ")");
                }
            }
            else {
                try {
                    FileUtils.delete(new File(path));
                    return Ok(true);
                } catch (IOException e) {
                    return Err("Internal", "Could not delete file (" + e.getMessage() + ")");
                }
            }
        }, Collections.singletonList("String"));

        // Directories
        func("listDirContents", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            if (!file.exists() || !file.isDirectory())
                return Err("Imaginary Path", "Path not found");

            String[] files = file.list();
            List<String> list = new ArrayList<>(Arrays.asList(files));
            return Ok(list);
        }, Collections.singletonList("String"));
        func("isDirectory", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            return Ok(file.isDirectory());
        }, Collections.singletonList("String"));

        // Working Directory
        func("getCWD", (args) -> Ok(System.getProperty("user.dir")), 0);
        func("setCWD", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            if (!file.exists())
                return Err("Imaginary Path", "Path not found");
            if (!file.isDirectory())
                return Err("Imaginary Path", "Path is not a directory");
            System.setProperty("user.dir", path);
            return Ok;
        }, Collections.singletonList("String"));

        // Serialization
        func("readSerial", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            if (!file.exists())
                return Err("Imaginary File", "File not found");

            Value out;
            try {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                Object obj = ois.readObject();
                if (obj instanceof Value) {
                    out = (Value) obj;
                }
                else {
                    out = Value.fromObject(obj);
                }

                ois.close();
                fis.close();
            } catch (IOException | ClassNotFoundException e) {
                return Err("Internal", "Could not load file (" + e.getMessage() + ")");
            }

            return Ok(out);
        }, Collections.singletonList("String"));
        func("readBytes", (args) -> {
            String path = dir(args[0]);
            File file = new File(path);
            if (!file.exists())
                return Err("Imaginary File", "File not found");

            byte[] bytes;
            try {
                bytes = Files.readAllBytes(Paths.get(path));
            } catch (IOException e) {
                return Err("Internal", "Could not load file (" + e.getMessage() + ")");
            }

            return Ok(bytes);
        }, Collections.singletonList("String"));
        func("writeSerial", (args) -> {
            String path = dir(args[0]);
            Value obj = args[1];
            File file = new File(path);
            try {
                boolean created = file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                if (obj.isBytes) {
                    fos.write(obj.asBytes());
                }
                else {
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(obj.asObject());
                    oos.close();
                }
                fos.close();
                return Ok(created);
            } catch (IOException e) {
                return Err("Internal", "Could not write file (" + e.getMessage() + ")");
            }
        }, Arrays.asList("String", "any"));
    }
}
