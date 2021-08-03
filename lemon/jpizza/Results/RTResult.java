package lemon.jpizza.Results;

import lemon.jpizza.Errors.Error;
import lemon.jpizza.Objects.Obj;

public class RTResult {
    public Obj value;
    public Error error;
    public Obj funcReturn;
    public boolean continueLoop;
    public boolean breakLoop;

    public RTResult() {reset();}

    private void reset() {
        value = null;
        error = null;
        funcReturn = null;
        continueLoop = false;
        breakLoop = false;
    }

    public Obj register(RTResult res) {
        if (res.error != null)
            error = res.error;
        funcReturn = res.funcReturn;
        continueLoop = res.continueLoop;
        breakLoop = res.breakLoop;
        return res.value;
    }

    public RTResult success(Obj value) {
        reset();
        this.value = value;
        return this;
    }

    public RTResult sreturn(Obj value) {
        reset();
        funcReturn = value;
        return this;
    }

    public RTResult scontinue() {
        reset();
        continueLoop = true;
        return this;
    }

    public RTResult sbreak() {
        reset();
        breakLoop = true;
        return this;
    }

    public RTResult failure(Error error) {
        reset();
        this.error = error;
        return this;
    }

    public boolean shouldReturn() {
        return error != null || funcReturn != null || breakLoop || continueLoop;
    }

}
