package lemon.jpizza.Libraries;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Double;
import lemon.jpizza.Errors.Error;
import lemon.jpizza.Errors.RTError;
import lemon.jpizza.Objects.Executables.BaseFunction;
import lemon.jpizza.Objects.Executables.Function;
import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Objects.Primitives.*;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class BuiltInFunction extends Library {
    private final Scanner scanner = new Scanner(System.in);
    private final Random random = new Random();
    private final Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    public BuiltInFunction(String name) { super(name); }

    public RTResult execute_println(Context execCtx) {
        System.out.println(((Obj) execCtx.symbolTable.get("value")).astring());
        return new RTResult().success(new Null());
    }

    public RTResult execute_print(Context execCtx) {
        System.out.print(((Obj) execCtx.symbolTable.get("value")).astring());
        return new RTResult().success(new Null());
    }

    public RTResult execute_type(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).type());
    }

    public RTResult execute_floating(Context execCtx) {
        Obj num = ((Obj) execCtx.symbolTable.get("value")).number();
        if (!(num instanceof Num)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        return new RTResult().success(new Bool(((Num) num).floating()));
    }

    public RTResult execute_random(Context _execCtx) {
        return new RTResult().success(new Num((float) Math.random()));
    }

    public RTResult execute_clear(Context _execCtx) {
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c",
                        "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException ignored) {}
        return new RTResult().success(new Null());
    }

    public RTResult execute_round(Context execCtx) {
        Obj num = ((Obj) execCtx.symbolTable.get("value")).number();
        if (!(num instanceof Num)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        return new RTResult().success(new Num(Math.round(
                ((Num) num).trueValue()
        )));
    }

    @SuppressWarnings("unchecked")
    public RTResult execute_foreach(Context execCtx) {
        RTResult res = new RTResult();
        PList list = (PList)((Obj) execCtx.symbolTable.get("list")).alist();
        BaseFunction func = (BaseFunction) ((Obj) execCtx.symbolTable.get("func")).function();
        List<Obj> newList = new ArrayList<>();
        List<Obj> preList = (List<Obj>) list.value;
        int size = preList.size();
        for (int i = 0; i < size; i++) {
            Obj after = (Obj) res.register(func.execute(Collections.singletonList(preList.get(i))));
            if (res.error != null) return res;
            newList.add(after);
        }
        return res.success(new PList(newList));
    }

    public RTResult execute_floor(Context execCtx) {
        Obj num = ((Obj) execCtx.symbolTable.get("value")).number();
        if (!(num instanceof Num)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        return new RTResult().success(new Num((float) Math.floor(
                ((Num) num).trueValue()
        )));
    }

    public RTResult execute_ceil(Context execCtx) {
        Obj num = ((Obj) execCtx.symbolTable.get("value")).number();
        if (!(num instanceof Num)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        return new RTResult().success(new Num((float) Math.ceil(
                ((Num) num).trueValue()
        )));
    }

    public RTResult execute_abs(Context execCtx) {
        Obj num = ((Obj) execCtx.symbolTable.get("value")).number();
        if (!(num instanceof Num)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a number",
                execCtx
        ));
        return new RTResult().success(new Num(Math.abs(
                ((Num) num).trueValue()
        )));
    }

    public RTResult execute_run(Context execCtx) {
        RTResult res = new RTResult();
        Obj fln = (Obj) execCtx.symbolTable.get("fn");
        if (!(fln instanceof Str)) return res.failure(new RTError(
                pos_start, pos_end,
                "Argument must be a string",
                execCtx
        ));

        String fn = ((Str) fln).trueValue();
        Path path = Path.of(fn);
        File s = new File(String.valueOf(path));
        if (!s.exists()) return res.failure(new RTError(
                pos_start, pos_end,
                "File does not exist in " + Paths.get("").toAbsolutePath(),
                execCtx
        )); String script;
        try {
            script = Files.readString(path);
        } catch (IOException e) {
            return res.failure(new RTError(
                    pos_start, pos_end,
                    "IOException reading file",
                    execCtx
            ));
        }
        Double runtime = Shell.run(fn, script);
        if (runtime.get(1) != null) return res.failure(new RTError(
                pos_start, pos_end,
                String.format("Failed to finish executing script \"%s\"%n%s", fn, ((Error) runtime.get(1)).asString()),
                execCtx
        ));
        System.out.println(runtime.get(0));
        return res.success(runtime.get(0) != null ? runtime.get(0) : new Null());
    }

    public RTResult execute_size(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("value")).alist();
        if (!(list instanceof PList)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list",
                execCtx
        ));
        return new RTResult().success(((PList) list).len());
    }

    public RTResult execute_insert(Context execCtx) {
        Obj index = ((Obj) execCtx.symbolTable.get("index")).number();
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj item = ((Obj) execCtx.symbolTable.get("item"));
        RTResult e = isInt(index, execCtx);
        if (e.error != null) return e;
        if (!(list instanceof PList)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list!",
                execCtx
        ));
        Num idx = (Num) index;
        PList lst = (PList) list;
        if (idx.trueValue() > lst.trueValue().size() || idx.trueValue() < 0) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Index is out of bounds!",
                execCtx
        ));
        lst.trueValue().add(Math.round(idx.trueValue()), item);
        return new RTResult().success(lst);
    }

    public RTResult execute_split(Context execCtx) {
        Obj string = ((Obj) execCtx.symbolTable.get("value")).astring();
        Obj splitter = ((Obj) execCtx.symbolTable.get("splitter")).astring();
        if (!(string instanceof Str) || !(splitter instanceof Str)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a string!",
                execCtx
        ));
        String str = ((Str) string).trueValue();
        String split = ((Str) splitter).trueValue();
        String[] pieces = str.split(split);
        List<Obj> fragments = new ArrayList<>();
        int length = pieces.length;
        for (int i = 0; i < length; i++) {
            fragments.add(new Str(pieces[i]));
        } return new RTResult().success(new PList(fragments));
    }

    public RTResult execute_str(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).astring());
    }
    public RTResult execute_list(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).alist());
    }
    public RTResult execute_bool(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).bool());
    }
    public RTResult execute_num(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).number());
    }
    public RTResult execute_dict(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).dictionary());
    }
    public RTResult execute_func(Context execCtx) {
        return new RTResult().success(((Obj) execCtx.symbolTable.get("value")).function());
    }

    public RTResult execute_isNumber(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Num));
    }
    public RTResult execute_isList(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof PList));
    }
    public RTResult execute_isDict(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Dict));
    }
    public RTResult execute_isString(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Str));
    }
    public RTResult execute_isBoolean(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Bool));
    }
    public RTResult execute_isFunction(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Function));
    }
    public RTResult execute_isNull(Context execCtx) {
        return new RTResult().success(new Bool(execCtx.symbolTable.get("value") instanceof Null));
    }

    public RTResult execute_printback(Context execCtx) {
        Obj obj = ((Obj) execCtx.symbolTable.get("value")).astring();
        System.out.print(obj);
        return new RTResult().success(new Str(obj.toString()));
    }

    public RTResult execute_field(Context execCtx) {
        System.out.print(((Obj) execCtx.symbolTable.get("value")).astring());
        String text = scanner.nextLine();
        return new RTResult().success(new Str(text));
    }

    public RTResult execute_nfield(Context execCtx) {
        System.out.print(((Obj) execCtx.symbolTable.get("value")).astring());
        String text;
        do {
            text = scanner.nextLine();
        } while (!pattern.matcher(text).matches());
        return new RTResult().success(new Num(Float.parseFloat(text)));
    }

    public RTResult execute_choose(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("value")).alist();
        if (!(list instanceof PList)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list!",
                context
        ));
        List<Obj> value = ((PList) list).trueValue();
        if (value.size() == 0) return new RTResult().success(new Null());
        return new RTResult().success(value.get(random.nextInt(value.size())));
    }

    private RTResult isInt(Obj num, Context execCtx) {
        if (!(num instanceof Num)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a number!",
                execCtx
        ));
        if (((Num) num).floating()) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be an int!",
                execCtx
        ));
        return new RTResult().success(new Null());
    }

    public RTResult execute_randint(Context execCtx) {
        RTResult res = new RTResult();
        Obj min = ((Obj) execCtx.symbolTable.get("min")).number();
        Obj max = ((Obj) execCtx.symbolTable.get("max")).number();
        res.register(isInt(min, execCtx));
        if (res.error != null) return res;
        res.register(isInt(max, execCtx));
        if (res.error != null) return res;
        int iMin = Math.round(((Num) min).trueValue());
        int iMax = Math.round(((Num) max).trueValue());
        return new RTResult().success(new Num(random.nextInt(iMax - iMin + 1) + iMin));
    }

    public RTResult execute_append(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj value = (Obj) execCtx.symbolTable.get("value");
        if (!(list instanceof PList)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list!",
                execCtx
        ));
        Double result = ((PList) list).append(value);
        if (result.get(1) != null) return new RTResult().failure((Error) result.get(1));
        return new RTResult().success(result.get(0));
    }

    public RTResult execute_remove(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj value = ((Obj) execCtx.symbolTable.get("value"));
        if (!(list instanceof PList)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list!",
                execCtx
        ));
        Double result = ((PList) list).remove(value);
        if (result.get(1) != null) return new RTResult().failure((Error) result.get(1));
        return new RTResult().success(result.get(0));
    }

    public RTResult execute_contains(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj value = ((Obj) execCtx.symbolTable.get("value"));
        if (!(list instanceof PList)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list!",
                execCtx
        ));
        return new RTResult().success(((PList) list).contains(value));
    }

    public RTResult execute_pop(Context execCtx) {
        Obj list = ((Obj) execCtx.symbolTable.get("list")).alist();
        Obj value = ((Obj) execCtx.symbolTable.get("value")).number();
        if (!(list instanceof PList)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list!",
                execCtx
        ));
        RTResult e = isInt(value, execCtx);
        if (e.error != null) return e;
        Double result = ((PList) list).pop(value);
        if (result.get(1) != null) return new RTResult().failure((Error) result.get(1));
        return new RTResult().success(result.get(0));
    }

    public RTResult execute_extend(Context execCtx) {
        Obj listA = ((Obj) execCtx.symbolTable.get("listA")).alist();
        Obj listB = ((Obj) execCtx.symbolTable.get("listB")).alist();
        if (!(listA instanceof PList) || !(listB instanceof PList)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list!",
                execCtx
        ));
        List<Obj> listAval = ((PList) listA).trueValue();
        listAval.addAll(((PList) listB).trueValue());
        return new RTResult().success(new PList(listAval));
    }

    public RTResult execute_get(Context execCtx) {
        Obj dict = ((Obj) execCtx.symbolTable.get("dict")).dictionary();
        Obj value = ((Obj) execCtx.symbolTable.get("value"));
        if (!(dict instanceof Dict)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list!",
                execCtx
        ));
        Dict x = (Dict) dict;
        if (!x.contains(value)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Key not in dict",
                execCtx
        ));
        return new RTResult().success(x.get(value));
    }

    public RTResult execute_delete(Context execCtx) {
        Obj dict = ((Obj) execCtx.symbolTable.get("dict")).dictionary();
        Obj value = ((Obj) execCtx.symbolTable.get("value"));
        if (!(dict instanceof Dict)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list!",
                execCtx
        ));
        Dict x = (Dict) dict;
        if (!x.contains(value)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Key not in dict",
                execCtx
        ));
        return new RTResult().success(x.delete(value));
    }

    public RTResult execute_set(Context execCtx) {
        Obj dict = ((Obj) execCtx.symbolTable.get("dict")).dictionary();
        Obj key = ((Obj) execCtx.symbolTable.get("key"));
        Obj value = ((Obj) execCtx.symbolTable.get("value"));
        if (!(dict instanceof Dict)) return new RTResult().failure(new RTError(
                pos_start, pos_end,
                "Argument must be a list!",
                execCtx
        ));
        Dict x = (Dict) dict;
        return new RTResult().success(x.set(key, value));
    }

}
