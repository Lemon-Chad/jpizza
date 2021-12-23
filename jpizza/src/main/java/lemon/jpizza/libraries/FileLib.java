package lemon.jpizza.libraries;

import lemon.jpizza.Constants;
import lemon.jpizza.contextuals.Context;
import lemon.jpizza.errors.RTError;
import lemon.jpizza.objects.executables.Library;
import lemon.jpizza.objects.Obj;
import lemon.jpizza.objects.primitives.*;
import lemon.jpizza.results.RTResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;

@SuppressWarnings("unused DuplicatedCode")
public class FileLib extends Library {

    public FileLib(String name) { super(name, "iofile"); }

    public static void initialize() {
        initialize("iofile", FileLib.class, new HashMap<>(){{
            put("readFile", Collections.singletonList("dir"));
            put("readSerial", Collections.singletonList("dir"));
            put("readBytes", Collections.singletonList("dir"));
            put("writeFile", Arrays.asList("dir", "val"));
            put("writeSerial", Arrays.asList("dir", "val"));
            put("fileExists", Collections.singletonList("dir"));
            put("makeDirs", Collections.singletonList("dir"));
            put("setCWD", Collections.singletonList("dir"));
            put("listDirContents", Collections.singletonList("dir"));
            put("getCWD", new ArrayList<>());
            put("isFileDirectory", Collections.singletonList("file"));
            put("deleteFile", Collections.singletonList("file"));
            put("deleteDir", Collections.singletonList("directory"));
        }});
    }

    public RTResult execute_deleteFile(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("file")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;
        if(!Files.exists(Path.of(dir))) {
            return new RTResult().failure(RTError.FileNotFound(value.get_start(), value.get_end(), "File not found", execCtx));
        }
        boolean isDirectory = Files.isDirectory(Path.of(dir));
        if(isDirectory) {
            try {
                FileUtils.deleteDirectory(new File(dir));
                return new RTResult().success(new Bool(true));
            }
            catch(IOException e) {
                return new RTResult().failure(RTError.Internal(value.get_start(), value.get_end(), "Java IOException " + e, execCtx));
            }
        }
        else {
            try {
                FileUtils.delete(new File(dir));
                return new RTResult().success(new Bool(true));
            }
            catch(IOException e) {
                return new RTResult().failure(RTError.Internal(value.get_start(), value.get_end(), "Java IOException " + e, execCtx));
            }
        }
    }

    private RTResult getdirectory(Obj value, Context ctx) {
        if (value.jptype != Constants.JPType.String) return new RTResult().failure(RTError.Type(
                value.get_start(), value.get_end(),
                "Expected String",
                ctx
        ));

        String dir = value.string;
        //noinspection RegExpRedundantEscape
        if (!dir.matches("^([A-Z]:|\\.|\\/|\\\\).*"))
            dir = System.getProperty("user.dir") + "/" + dir;
        return new RTResult().success(new Str(dir));
    }

    public RTResult execute_readFile(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;

        File file = new File(dir);
        if (!file.exists()) return res.failure(RTError.FileNotFound(
                value.get_start(), value.get_end(),
                "File does not exist",
                execCtx
        ));

        String out;
        try {
            out = Files.readString(Path.of(dir));
        } catch (IOException e) {
            return res.failure(RTError.Internal(
                    value.get_start(), value.get_end(),
                    "IOException occurred while reading.. " + e,
                    execCtx
            ));
        }

        return res.success(new Str(out));
    }

    public RTResult execute_readSerial(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;

        File file = new File(dir);
        if (!file.exists()) return res.failure(RTError.FileNotFound(
                value.get_start(), value.get_end(),
                "File does not exist",
                execCtx
        ));

        Obj out;
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            Object oit = ois.readObject();
            if (!(oit instanceof Obj))
                out = Constants.getFromValue(oit);
            else
                out = (Obj) oit;

            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            return res.failure(RTError.Internal(
                    value.get_start(), value.get_end(),
                    "IOException occurred while reading.. " + e,
                    execCtx
            ));
        }

        return res.success(out);
    }

    public RTResult execute_readBytes(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;

        File file = new File(dir);
        if (!file.exists()) return res.failure(RTError.FileNotFound(
                value.get_start(), value.get_end(),
                "File does not exist",
                execCtx
        ));

        Obj out;
        try {
            FileInputStream fis = new FileInputStream(file);

            byte[] bytes = fis.readAllBytes();
            out = new Bytes(bytes);

            fis.close();
        } catch (IOException e) {
            return res.failure(RTError.Internal(
                    value.get_start(), value.get_end(),
                    "IOException occurred while reading.. " + e,
                    execCtx
            ));
        }

        return res.success(out);
    }

    public RTResult execute_fileExists(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;

        return res.success(new Bool(Files.exists(Path.of(dir))));
    }

    public RTResult execute_makeDirs(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;

        File file = new File(dir);

        return res.success(new Bool(file.mkdirs()));
    }

    public RTResult execute_setCWD(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;
        File file = new File(dir);
        
        if (!file.exists() || !file.isDirectory()) return res.failure(RTError.PathNotFound(
                value.get_start(), value.get_end(),
                "Directory does not exist",
                execCtx
        ));

        System.setProperty("user.dir", dir);

        return res.success(new Null());
    }

    public RTResult execute_getCWD(Context execCtx) {
        return new RTResult().success(new Str(System.getProperty("user.dir")));
    }

    public RTResult execute_writeFile(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;

        Obj vtwo = ((Obj) execCtx.symbolTable.get("val")).astring();
        if (vtwo.jptype != Constants.JPType.String) return res.failure(RTError.Type(
                vtwo.get_start(), vtwo.get_end(),
                "Expected String",
                execCtx
        ));

        String val = vtwo.string;

        File file = new File(dir);
        boolean created;
        try {
            created = file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(val);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return res.failure(RTError.Internal(
                    value.get_start(), value.get_end(),
                    "IOException occurred while writing.. " + e,
                    execCtx
            ));
        }

        return res.success(new Bool(created));
    }

    public RTResult execute_writeSerial(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;

        Obj val = (Obj) execCtx.symbolTable.get("val");

        File file = new File(dir);
        boolean created;
        try {
            created = file.createNewFile();
            FileOutputStream fout = new FileOutputStream(file);
            if (val.jptype != Constants.JPType.Bytes) {
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                oos.writeObject(val);
                oos.close();
            }
            else
                fout.write(val.arr);
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
            return res.failure(RTError.Internal(
                    value.get_start(), value.get_end(),
                    "IOException occurred while writing.. " + e,
                    execCtx
            ));
        }

        return res.success(new Bool(created));
    }

    public RTResult execute_listDirContents(Context execCtx){
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;

        Path path = Paths.get(dir);
        if (!Files.exists(path) || !Files.isDirectory(path)) return res.failure(RTError.PathNotFound(
                value.get_start(), value.get_end(),
                "Path does not exist",
                execCtx
        ));
        String[] pathnames;
        try {
            pathnames = path.toFile().list();
        } catch (Exception e) {
            return res.failure(RTError.Internal(value.get_start(), value.get_end(), e.toString(), execCtx));
        }

        PList paths = new PList(new ArrayList<>());
        for (String pth: pathnames) {
            paths.append(new Str(pth));
        }

        return res.success(paths);
    }

    public  RTResult execute_isFileDirectory(Context execCtx){
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("file")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = d.string;
        return res.success(new Bool(Files.isDirectory(Path.of(dir))));
    }

}
