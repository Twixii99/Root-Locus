package sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DataSet {
    public static Function<Double, Double> openLoopXferFunction;
    public static double[] coefficients; // coefficients of the cc equation
    public static int maxPower; // The order of the function

    public static double[] derivativeCofficients; // coefficients of the derivative of cc equation

    public static String[] poles;
    public static String[] zeros;

    public static Double[] realPoles;
    public static String[] imagPoles;

    public static Double[] breakPoints;

    public static String oscillationFrequency;

    public static Double asymptotePoint;
    public static Double[] asymptoteAngels;

    public static Map<String, ArrayList<Double>> depreatureAngels = new HashMap<>();
}
