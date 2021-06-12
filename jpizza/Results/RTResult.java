package lemon.jpizza.Results;

import lemon.jpizza.Objects.Value;
import lemon.jpizza.Errors.Error;

public class RTResult {
    public Object value;
    public Error error;
    public Object funcReturn;
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

    public Object register(RTResult res) {
        if (res.error != null)
            error = res.error;
        funcReturn = res.funcReturn;
        continueLoop = res.continueLoop;
        breakLoop = res.breakLoop;
        return res.value;
    }

    public RTResult success(Object value) {
        reset();
        this.value = value;
        return this;
    }

    public RTResult sreturn(Object value) {
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
