package sample;

import javax.script.ScriptException;

public class Main {

    public static void main(String[] args) throws ScriptException {
        double[] x = {1, 125, 5100, 65000};
        String[] t = {"0", "-25", "-50 - 10j", "-50 + 10j"};
        XferManip xferManip = new XferManip(y -> 2.0, 4, x, t);
        xferManip.getBreakPoint();
        xferManip.getAsymptote();
        xferManip.getDepreatureAngels();
    }
}