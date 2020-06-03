package sample;

import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.function.Function;
import javax.crypto.AEADBadTagException;
import javax.script.*;

public class XferManip {
    private ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");

    private Scanner scanner = new Scanner(System.in);

    // from numerical analysis needed 6 significant figures at least.
    // 0.5*(10^(2-n))  where n is number of significant figures.
    private final double tol = 5E-5;

    public XferManip(Function<Double, Double> function, int maxPower, double[] coefficients, String[] poles) {
        DataSet.openLoopXferFunction = function;
        if(coefficients.length == maxPower)  {
            DataSet.coefficients = new double[maxPower+1];
            System.arraycopy(coefficients, 0, DataSet.coefficients, 0, coefficients.length);
        }
        else if(coefficients.length < maxPower)
            throw new RuntimeException();
        else
            DataSet.coefficients = coefficients;
        DataSet.poles = poles;
        DataSet.zeros = new String[0];
        DataSet.maxPower = maxPower;

        // initialize derivative formula ...
        DataSet.derivativeCofficients = new double[DataSet.maxPower];
        Arrays.fill(DataSet.derivativeCofficients, 0.0);
    }

    public XferManip(Function<Double, Double> function, int maxPower, double[] coefficients, String[] poles, String[] zeros) {
        this(function, maxPower, coefficients, poles);
        DataSet.zeros = zeros;
    }

    public void getAsymptote() {
        Double submission = 0.0;
        for(int i = 0; i < DataSet.realPoles.length; ++i)
            submission += DataSet.realPoles[i];
        for(int i = 0; i < DataSet.imagPoles.length; ++i) {
            String[] split = DataSet.imagPoles[i].split("\\s+[\\-\\+]{1}\\s+");
            for(int j = 0; j < split.length; ++j) {
                if(!split[j].contains("j"))
                    submission += Double.parseDouble(split[j]);
            }
        }
        for(int i = 0; i < DataSet.zeros.length; ++i) {
            if(!DataSet.zeros[i].contains("j"))
                submission += Double.parseDouble(DataSet.zeros[i]);
            else {
                String[] split = DataSet.imagPoles[i].split("\\s+[\\-\\+]{1}\\s+");
                for(int j = 0; j < split.length; ++j) {
                    if(!split[j].contains("j"))
                        submission += Double.parseDouble(split[j]);
                }
            }
        }
        DataSet.asymptotePoint = submission / (DataSet.poles.length - DataSet.zeros.length);

        // getting angels
        int q = Math.abs(DataSet.poles.length - DataSet.zeros.length);
        DataSet.asymptoteAngels = new Double[q];
        for(int i = 0; i < q; ++i) {
            DataSet.asymptoteAngels[i] = Double.valueOf(180 * (2 * i + 1) / (DataSet.poles.length - DataSet.zeros.length));
        }
    }

    public void getDepreatureAngels() {
        ArrayList<Double> angels = new ArrayList<>();
        for(int i = 0; i < DataSet.poles.length; ++i) {
            if(!DataSet.poles[i].contains("j")) continue;
            String firstPoint = DataSet.poles[i];
            String[] splitter = firstPoint.split("\\s+");
            if(splitter.length == 1) {
                if(splitter[0].contains("j")) {
                    String x = splitter[0];
                    splitter = new String[3];
                    splitter[0] = "0.0";
                    splitter[1] = "+";
                    splitter[2] = x;
                }
            }
            for(int j = 0; j < DataSet.poles.length; ++j) {
                if(i == j) continue;
                String secondPoint = DataSet.poles[j];
                String[] splitter2 = secondPoint.split("\\s+");
                if(splitter2.length == 1) {
                    if(splitter2[0].contains("j")) {
                        String x = splitter2[0];
                        splitter2 = new String[3];
                        splitter2[0] = "0.0";
                        splitter2[1] = "+";
                        splitter2[2] = x;
                    } else {
                        String x = splitter2[0];
                        splitter2 = new String[3];
                        splitter2[0] = x;
                        splitter2[1] = "+";
                        splitter2[2] = "0.0j";
                    }
                }
                angels.add(calculateAngle(splitter, splitter2));
            }
            DataSet.depreatureAngels.put(DataSet.poles[i], new ArrayList<Double>(angels));
            angels.clear();
        }
    }

    private double calculateAngle(String[] splitter, String[] splitter2) {
        double real, imag;
        real = Double.parseDouble(splitter[0]) - Double.parseDouble(splitter2[0]);
        imag = Double.parseDouble(splitter[2].replace("j", "")) - Double.parseDouble(splitter2[2].replace("j", ""));
        double angle = Math.atan(imag / real) * 180.0 / 3.141592654;
        if(splitter[1].contains("-") || splitter2[1].contains("-"))
            angle = -1 * angle;
        if(Double.parseDouble(splitter[0]) < Double.parseDouble(splitter2[0])) {
            if(angle > 0.0)
                angle = -180.0 + angle;
            else
                angle = 180.0 + angle;
        }
        return angle;
    }

    public void getBreakPoint() throws ScriptException {
        String function = "";
        this.differentiateFunction();
        for(int i = DataSet.maxPower-1, j = 0; i >= 0; --i)
            function += DataSet.derivativeCofficients[j++] + "*Math.pow(s, " + i + ")+";
        function = function.substring(0, function.length()-1);

        DataSet.realPoles = new Double[DataSet.poles.length];
        DataSet.imagPoles = new String[DataSet.poles.length];

        int j = 0, k = 0;
        for(int i = 0; i < DataSet.poles.length; ++i) {
            if(!DataSet.poles[i].contains("j"))
                DataSet.realPoles[j++] = Double.parseDouble(DataSet.poles[i]);
            else
                DataSet.imagPoles[k++] = DataSet.poles[i];
        }

        Double[] auxiliary = new Double[j];
        for(int i = 0; i < j; ++i)
            auxiliary[i] = DataSet.realPoles[i];
        DataSet.realPoles = auxiliary;
        String[] dummy = new String[k];
        for(int i = 0; i < j; ++i)
            dummy[i] = DataSet.imagPoles[i];
        DataSet.imagPoles = dummy;

        Arrays.sort(DataSet.realPoles, Collections.reverseOrder());
        DataSet.breakPoints = new Double[DataSet.realPoles.length / 2];

        for(int i = 0, x = 0; i < DataSet.realPoles.length && i+1 < DataSet.realPoles.length ; i += 2) {
            double xl, xu;
            if(this.evaluate(function.replaceAll("s", String.valueOf(DataSet.realPoles[i]))).charAt(0) == '-') {
                xl = DataSet.realPoles[i];
                xu = DataSet.realPoles[i + 1];
            } else {
                xu = DataSet.realPoles[i];
                xl = DataSet.realPoles[i + 1];
            }
            DataSet.breakPoints[x++] = Double.valueOf(this.Bisection("s", function, xl, xu));
        }
    }

    private void differentiateFunction() {
        for(int i = DataSet.maxPower, j = 0; i > 0; --i, ++j) {
            DataSet.derivativeCofficients[j] = DataSet.coefficients[j] * i;
        }
    }

    public void routhStability() throws ScriptException {
        String[] string_Coefficients;
        int flag = 1;
        if((DataSet.coefficients.length & 1) == 1) {
            string_Coefficients = new String[DataSet.coefficients.length + 1];
            flag = 2;
        } else
            string_Coefficients = new String[DataSet.coefficients.length];

        for(int i = 0; i < DataSet.coefficients.length; ++i)
            string_Coefficients[i] = String.valueOf(DataSet.coefficients[i]);

        string_Coefficients[string_Coefficients.length - flag] = string_Coefficients[string_Coefficients.length-flag].matches("0\\.0") ?
                "k" : string_Coefficients[string_Coefficients.length-flag] + "+k";

        if((DataSet.coefficients.length & 1) == 1)
            string_Coefficients[string_Coefficients.length-1] = "0.0";

        String[][] routhArray = this.fillRouthArray(string_Coefficients);
        for(int i = 0; i < DataSet.maxPower-1; ++i) {
            for(int j = 1; j < Math.ceil(((double)routhArray.length) / 2); ++j) {
                routhArray[i+2][j-1] = this.evaluate("(1/" + routhArray[i+1][0] + ")") + "*" +
                        "(" + this.evaluate(routhArray[i+1][0] + "*" + routhArray[i][j]) +
                        "-" + this.evaluate(routhArray[i+1][j] + "*" + routhArray[i][0]) + ")";
                routhArray[i+2][j-1] = this.evaluate(routhArray[i+2][j-1]);
                if(i == 0 && j == DataSet.maxPower-2)
                    routhArray[i+2][j-1] = "k";
            }
        }
        System.out.print("Enter Lower Bound for Bisection solution: ");
        double x = scanner.nextDouble();
        System.out.print("Enter Upper Bound for Bisection solution: ");
        double y = scanner.nextDouble();
        String k = this.Bisection("k", routhArray[routhArray.length-2][0], x, y);
        DataSet.oscillationFrequency =  this.getOscillationFrequency(this.evaluate(routhArray[routhArray.length-3][1].replaceAll("k", k)),
                routhArray[routhArray.length-3][0]);
    }

    private String[][] fillRouthArray(String[] string_Coefficients) {
        String[][] routhArray = new String[DataSet.maxPower+1][string_Coefficients.length/2];
        for(int i = 0; i < routhArray.length; ++i) {
            for (int j = 0; j < routhArray[0].length; ++j)
                routhArray[i][j] = "0.0";
        }
        for(int i = 0, j = 0, k = 0; i < string_Coefficients.length; ++j) {
            routhArray[j & 1][k] = string_Coefficients[i++];
            if((j & 1) == 1) ++k;
        }
        return routhArray;
    }

    private String getOscillationFrequency(String x, String y) {
        return "j" + Math.sqrt(Math.abs(Double.parseDouble(x) / Double.parseDouble(y)));
    }

    private String Bisection(String symbol, String expression, double xl, double xu) throws ScriptException {
        double xr = (xl + xu) / 2;
        String replacement = expression.replaceAll(symbol, String.valueOf(xr));
        String answer = this.evaluate(replacement);
        while(Math.abs(Double.parseDouble(answer)) >= this.tol) {
            if(answer.charAt(0) == '-')
                xl = xr;
            else
                xu = xr;
            xr = (xl + xu) / 2;
            replacement = expression.replaceAll(symbol, String.valueOf(xr));
            answer = this.evaluate(replacement);
        }
        return String.valueOf(Math.round(xr * 1000.0) / 1000.0);
    }

    private String evaluate(String str) throws ScriptException{
        if(!str.contains("k"))
            return String.valueOf(engine.eval(str));
        return str;
    }
}
