package lemon.jpizza.Libraries;

import lemon.jpizza.Objects.Executables.Library;
import lemon.jpizza.Objects.Obj;
import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Results.RTResult;
import lemon.jpizza.Objects.Primitives.*;

@SuppressWarnings("unused")
public class GUIs extends Library {

    public GUIs(String name) { super(name); }

    public RTResult execute_GUI(Context execCtx) {
        // Get value arg from variables passed into function
        Obj value = (Obj) execCtx.symbolTable.get("value");
        // Convert value argument to a string type
        value = value.astring();
        // Print value argument
        System.out.println(value);
        // Return null
        return new RTResult().success(new Null());
    }
}
