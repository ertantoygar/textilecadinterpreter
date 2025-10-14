package tr.com.logidex.cad.util;


import java.util.List;


public class Util {



    public static double mmToInch(double mm){
        return mm/25.4;
    }

    public static double incToMM(double inches){
        return inches * 25.4;
    }


    public static double[] convertDoubles(List<Double> doubles) {
        double[] ret = new double[doubles.size()];


        for (int i = 0; i < ret.length; i++) {
            ret[i] = doubles.get(i).doubleValue();
        }
        return ret;
    }

}
