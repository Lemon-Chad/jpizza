package lemon.jpizza.Libraries;

import lemon.jpizza.Constants;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.Bool;
import lemon.jpizza.Objects.Primitives.Bytes;
import lemon.jpizza.Objects.Primitives.Null;
import lemon.jpizza.Objects.Primitives.Str;
import lemon.jpizza.Results.RTResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("unused DuplicatedCode")
public class FileLib extends Library {

    public FileLib(String name) { super(name); }

    private RTResult getdirectory(Obj value, Context ctx) {
        if (value.jptype != Constants.JPType.String) return new RTResult().failure(new RTError(
                value.pos_start, value.pos_end,
                "Expected str",
                ctx
        ));

        String dir = ((Str) value).trueValue();
        if (!dir.matches("^([A-Z]:|\\.).*"))
            dir = System.getProperty("user.dir") + "\\" + dir;
        return new RTResult().success(new Str(dir));
    }

    public RTResult execute_readFile(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = ((Str) d).trueValue();

        File file = new File(dir);
        if (!file.exists()) return res.failure(new RTError(
                value.pos_start, value.pos_end,
                "File does not exist",
                execCtx
        ));

        String out;
        try {
            out = Files.readString(Path.of(dir));
        } catch (IOException e) {
            return res.failure(new RTError(
                    value.pos_start, value.pos_end,
                    "IOException occurred while reading..",
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
        String dir = ((Str) d).trueValue();

        File file = new File(dir);
        if (!file.exists()) return res.failure(new RTError(
                value.pos_start, value.pos_end,
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
            return res.failure(new RTError(
                    value.pos_start, value.pos_end,
                    "IOException occurred while reading..",
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
        String dir = ((Str) d).trueValue();

        return res.success(new Bool(Files.exists(Path.of(dir))));
    }

    public RTResult execute_makeDirs(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = ((Str) d).trueValue();

        File file = new File(dir);

        return res.success(new Bool(file.mkdirs()));
    }

    public RTResult execute_setCWD(Context execCtx) {
        RTResult res = new RTResult();
        Obj value = ((Obj) execCtx.symbolTable.get("dir")).astring();
        Obj d = res.register(getdirectory(value, execCtx));
        if (res.error != null) return res;
        String dir = ((Str) d).trueValue();
        File file = new File(dir);
        
        if (!file.exists() || !file.isDirectory()) return res.failure(new RTError(
                value.pos_start, value.pos_end,
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
        String dir = ((Str) d).trueValue();

        Obj vtwo = ((Obj) execCtx.symbolTable.get("val")).astring();
        if (vtwo.jptype != Constants.JPType.String) return res.failure(new RTError(
                vtwo.pos_start, vtwo.pos_end,
                "Expected str",
                execCtx
        ));

        String val = ((Str) vtwo).trueValue();

        File file = new File(dir);
        boolean created;
        try {
            created = file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(val);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return res.failure(new RTError(
                    value.pos_start, value.pos_end,
                    "IOException occurred while writing..",
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
        String dir = ((Str) d).trueValue();

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
            } else
                fout.write(((Bytes) val).arr);
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
            return res.failure(new RTError(
                    value.pos_start, value.pos_end,
                    "IOException occurred while writing..",
                    execCtx
            ));
        }

        return res.success(new Bool(created));
    }

}
