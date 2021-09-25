package lemon.jpizza.results;

import lemon.jpizza.errors.Error;
import lemon.jpizza.objects.Obj;

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
        error = null;
        funcReturn = null;
        continueLoop = false;
        breakLoop = false;
        this.value = value;
        return this;
    }

    public RTResult sreturn(Obj value) {
        this.value = null;
        error = null;
        continueLoop = false;
        breakLoop = false;
        funcReturn = value;
        return this;
    }

    public RTResult scontinue() {
        value = null;
        error = null;
        funcReturn = null;
        breakLoop = false;
        continueLoop = true;
        return this;
    }

    public RTResult sbreak() {
        value = null;
        error = null;
        funcReturn = null;
        continueLoop = false;
        breakLoop = true;
        return this;
    }

    public RTResult failure(Error error) {
        value = null;
        funcReturn = null;
        continueLoop = false;
        breakLoop = false;
        this.error = error;
        return this;
    }

    public boolean shouldReturn() {
        return error != null || funcReturn != null || breakLoop || continueLoop;
    }

}
