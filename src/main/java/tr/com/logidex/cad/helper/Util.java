package tr.com.logidex.cad.helper;

import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import tr.com.logidex.cad.model.CoordinateBounds;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Utility class providing helper methods for CAD operations including:
 * - File I/O operations
 * - Unit conversions
 * - Coordinate transformations
 * - Image generation from line data
 */
public class Util {

    // Constants for unit conversions
    private static final double MM_TO_INCH_FACTOR = 25.4;
    private static final double MM_TO_INCH_CONVERSION = 0.0393700787401575;
    private static final double DEFAULT_DPI = 72.0;

    // Prevent instantiation
    private Util() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== File Operations ====================

    /**
     * Reads the entire contents of a file as a string.
     *
     * @param path The file path to read
     * @param encoding The character encoding to use
     * @return The file contents as a string
     * @throws IOException If an I/O error occurs
     */
    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    // ==================== Image Generation ====================

    /**
     * Creates an ImageView from a list of lines representing a shape.
     * The image is scaled to fit within the specified maximum dimensions.
     *
     * @param lines The lines forming the shape
     * @param maxWidth Maximum width for the image
     * @param maxHeight Maximum height for the image
     * @return An ImageView containing the rendered shape
     */
    public static ImageView createImageViewFromLines(List<Line> lines, double maxWidth, double maxHeight) {
        Pane pane = createPaneWithBackground(maxWidth, maxHeight);
        CoordinateBounds bounds = findMinMaxCoordinates(lines);

        if (bounds == null) {
            return createEmptyImageView(maxWidth, maxHeight);
        }

        Polygon polygon = createPolygonFromLines(lines, bounds);
        pane.getChildren().add(polygon);

        return createImageViewFromPane(pane, maxWidth, maxHeight);
    }

    private static Pane createPaneWithBackground(double width, double height) {
        Pane pane = new Pane();
        Rectangle background = new Rectangle(width, height);
        background.setFill(Color.TRANSPARENT);
        return pane;
    }

    private static Polygon createPolygonFromLines(List<Line> lines, CoordinateBounds bounds) {
        Polygon polygon = new Polygon();

        for (Line line : lines) {
            double x1 = line.getStartX() - bounds.getMinX();
            double y1 = line.getStartY() - bounds.getMinY();
            double x2 = line.getEndX() - bounds.getMinX();
            double y2 = line.getEndY() - bounds.getMinY();

            polygon.getPoints().addAll(x2, y2, x1, y1);
        }

        polygon.setScaleY(-1); // Flip vertically
        return polygon;
    }

    private static ImageView createImageViewFromPane(Pane pane, double maxWidth, double maxHeight) {
        WritableImage writableImage = pane.snapshot(null, null);
        ImageView imageView = new ImageView(writableImage);

        imageView.setPreserveRatio(true);
        imageView.setFitWidth(maxWidth);
        imageView.setFitHeight(maxHeight);

        return imageView;
    }

    private static ImageView createEmptyImageView(double maxWidth, double maxHeight) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(maxWidth);
        imageView.setFitHeight(maxHeight);
        return imageView;
    }

    // ==================== Coordinate Operations ====================

    /**
     * Finds the minimum and maximum coordinates from a list of lines.
     *
     * @param lines The lines to analyze
     * @return A CoordinateBounds object containing min/max coordinates, or null if list is empty
     */
    public static CoordinateBounds findMinMaxCoordinates(List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }

        Line firstLine = lines.get(0);
        double minX = firstLine.getStartX();
        double minY = firstLine.getStartY();
        double maxX = firstLine.getStartX();
        double maxY = firstLine.getStartY();

        for (Line line : lines) {
            minX = Math.min(minX, Math.min(line.getStartX(), line.getEndX()));
            minY = Math.min(minY, Math.min(line.getStartY(), line.getEndY()));
            maxX = Math.max(maxX, Math.max(line.getStartX(), line.getEndX()));
            maxY = Math.max(maxY, Math.max(line.getStartY(), line.getEndY()));
        }

        return new CoordinateBounds(minX, minY, maxX, maxY);
    }

    /**
     * Reverts (rotates) a point by swapping coordinates and negating X.
     * Transforms (x, y) to (y, -x).
     *
     * @param x1 The X coordinate
     * @param y1 The Y coordinate
     * @return A new Point2D with reverted coordinates
     */
    public static Point2D revertXpoint(double x1, double y1) {
        return new Point2D(y1, -x1);
    }

    // ==================== Scaling Operations ====================

    /**
     * Linearly scales an integer value from one range to another.
     *
     * @param input The input value to scale
     * @param inputLower Lower bound of input range
     * @param inputUpper Upper bound of input range
     * @param outputLower Lower bound of output range
     * @param outputUpper Upper bound of output range
     * @return The scaled value
     * @throws IllegalArgumentException If input range has zero width
     */
    public static int linearScale(int input, int inputLower, int inputUpper,
                                  int outputLower, int outputUpper) {
        if (inputUpper == inputLower) {
            throw new IllegalArgumentException("Input range cannot have zero width");
        }

        return (input - inputLower) * (outputUpper - outputLower) /
                (inputUpper - inputLower) + outputLower;
    }

    /**
     * Scales a double value from one range to another.
     *
     * @param value The value to scale
     * @param minOld Lower bound of old range
     * @param maxOld Upper bound of old range
     * @param minNew Lower bound of new range
     * @param maxNew Upper bound of new range
     * @return The scaled value
     * @throws IllegalArgumentException If old range has zero width
     */
    public static double scaleValue(double value, double minOld, double maxOld,
                                    double minNew, double maxNew) {
        if (maxOld == minOld) {
            throw new IllegalArgumentException("Old range cannot have zero width");
        }

        return ((value - minOld) / (maxOld - minOld)) * (maxNew - minNew) + minNew;
    }

    // ==================== Unit Conversions ====================

    /**
     * Converts millimeters to pixels using default DPI.
     *
     * @param mm The value in millimeters
     * @return The value in pixels
     */
    public static double mmToPixel(double mm) {
        double widthInches = mm * MM_TO_INCH_CONVERSION;
        return widthInches * DEFAULT_DPI;
    }

    /**
     * Converts millimeters to inches.
     *
     * @param mm The value in millimeters
     * @return The value in inches
     */
    public static double mmToInch(double mm) {
        return mm / MM_TO_INCH_FACTOR;
    }

    /**
     * Converts inches to millimeters.
     *
     * @param inches The value in inches
     * @return The value in millimeters
     */
    public static double inchToMM(double inches) {
        return inches * MM_TO_INCH_FACTOR;
    }

    // ==================== Formatting ====================

    /**
     * Formats a decimal value to a specified number of decimal places.
     *
     * @param value The value to format
     * @param decimalPlaces The number of decimal places
     * @return The formatted value as a string
     */
    public static String formatDecimalPoint(double value, int decimalPlaces) {
        return String.format("%." + decimalPlaces + "f", value);
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