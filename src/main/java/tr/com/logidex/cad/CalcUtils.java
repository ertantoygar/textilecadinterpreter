package tr.com.logidex.cad;


import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.util.List;


public final class CalcUtils {

    private CalcUtils() {
    }


    /**
     * @param numbers
     * @return double - average
     */
    public static double calcAverage(double... numbers) {
        double sum = 0;


        for (int i = 0; i < numbers.length; i++) {
            sum += numbers[i];
        }
        return sum / numbers.length;
    }


    public static double calcTheDistanceOfTwoPoints(Point2D p1, Point2D p2) {
        double a = Math.pow(p2.getX() - p1.getX(), 2);
        double b = Math.pow(p2.getY() - p1.getY(), 2);
        return Math.sqrt(a + b);
    }


    /**
     * This method returns the angle in the middle of the given three points.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static double calcAngleBetweenThreePoints(Point2D a, Point2D b, Point2D c) {
        return b.angle(a, c);
    }


    public static double invertSign(double d) {
        return d * -1;
    }


    public static boolean getBit(int n, int bit) {
        return ((n >> bit) & 1) == 1 ? true : false;
    }

    public static boolean getBit(short n, int bit) {
        return ((n >> bit) & 1) == 1 ? true : false;
    }


    public static int invertBit(int n, int bit) {
        return n ^= 1 << bit;
    }


    public static int setBit(int n, int bit, boolean state) {
        if (state)
            return n |= 1 << bit; // set bit
        else
            return n &= ~(1 << bit); // reset bit
    }


    public static short setBit(short n, int bit, boolean state) {
        if (state)
            return n |= 1 << bit; // set bit
        else
            return n &= ~(1 << bit); // reset bit
    }


    public static Polygon producePolygon(List<Line> lines) {
        Polygon polygon = new Polygon();


        for (Line line : lines) {
            polygon.getPoints().add(line.getStartX());
            polygon.getPoints().add(line.getStartY());
            polygon.getPoints().add(line.getEndX());
            polygon.getPoints().add(line.getEndY());
        }
        return polygon;
    }


    public static boolean isNear(Point2D p1, Point2D p2, double tolerance) {
        return Math.abs(p1.distance(p2)) < tolerance;
    }


    public static double[] convertDoubles(List<Double> doubles) {
        double[] ret = new double[doubles.size()];


        for (int i = 0; i < ret.length; i++) {
            ret[i] = doubles.get(i).doubleValue();
        }
        return ret;
    }

}
