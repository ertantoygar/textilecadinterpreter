package tr.com.logidex.cad.model;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a closed pattern/shape in a CAD drawing.
 *
 * A Pattern consists of connected lines forming a closed polygon,
 * with associated properties like center point, boundaries, and labels.
 */
public class Pattern {

    // ============================================
    // CONSTANTS
    // ============================================

    public static final double MIN_VALID_PATTERN_AREA = 850.0;
    public static final double MAX_VALID_PATTERN_AREA = 1_500_000.0;
    private static final double CENTROID_AREA_THRESHOLD = 0.0001;
    private static final int MAX_CENTER_SEARCH_ATTEMPTS_MULTIPLIER = 2;

    // ============================================
    // FIELDS
    // ============================================

    private  Integer id;
    private final List<Line> lines;
    private final boolean isGGTFile;

    private Label label;
    private Point2D center;
    private BoundingBox bounds;
    private double minX, maxX, minY, maxY;

    private boolean calculatedCenterPointIsInside;
    private boolean isSelected;
    private boolean isPrinted;

    private Color color;
    private final Color originalColor;

    // ============================================
    // CONSTRUCTOR
    // ============================================

    public Pattern(Integer id, List<Line> lines, boolean isGGTFile) {
        this.id = id;
        this.lines = new ArrayList<>(lines); // Defensive copy
        this.isGGTFile = isGGTFile;
        this.color = generateRandomColor();
        this.originalColor = this.color;

        analyzePath();
    }

    // Convenience constructor with default ID
    public Pattern(List<Line> lines, boolean isGGTFile) {
        this(null, lines, isGGTFile);
    }

    public void setId(Integer id) {
        this.id = id;
    }




    // ============================================
    // PUBLIC API
    // ============================================

    /**
     * Checks if this pattern represents a valid piece based on area.
     *
     * @return true if area is within valid range
     */
    public boolean isValid() {
        double area = bounds.getWidth() * bounds.getHeight();
        return area > MIN_VALID_PATTERN_AREA && area < MAX_VALID_PATTERN_AREA;
    }

    /**
     * Mirrors the pattern horizontally across a vertical axis.
     *
     * @param drawingWidth the width of the drawing (axis position)
     */
    public void mirrorX(double drawingWidth) {
        for (Line line : lines) {
            line.setStartX(drawingWidth - line.getStartX());
            line.setEndX(drawingWidth - line.getEndX());
        }
        analyzePath();
        relocateCenterX();
    }

    /**
     * Mirrors the pattern vertically across a horizontal axis.
     *
     * @param drawingHeight the height of the drawing (axis position)
     */
    public void mirrorY(double drawingHeight) {
        for (Line line : lines) {
            line.setStartY(drawingHeight - line.getStartY());
            line.setEndY(drawingHeight - line.getEndY());
        }
        analyzePath();
        relocateCenterX(); // Will also call relocateCenterY
    }

    /**
     * Re-analyzes the pattern's geometry (useful after modifications).
     */
    public void reAnalyze() {
        analyzePath();
    }

    /**
     * Restores the pattern's color to its original random color.
     */
    public void restoreColor() {
        this.color = originalColor;
    }

    // ============================================
    // STATIC GEOMETRY UTILITIES
    // ============================================

    /**
     * Performs the raycasting algorithm to determine if a point is inside a polygon.
     *
     * This uses the even-odd rule algorithm with O(n) complexity where n is the
     * number of edges.
     *
     * @param lines the polygon edges
     * @param point the point to test.hpgl
     * @return true if the point is inside the polygon
     *
     * @see <a href="https://www.algorithms-and-technologies.com/point_in_polygon/java">
     *      Point in Polygon Algorithm</a>
     */
    public static boolean containsPoint(List<Line> lines, Point2D point) {
        double[][] polygon = linesToPolygonArray(lines);
        double px = point.getX();
        double py = point.getY();

        boolean inside = false;

        for (int i = 0, j = polygon.length - 1; i < polygon.length; i++) {
            double xi = polygon[i][0], yi = polygon[i][1];
            double xj = polygon[j][0], yj = polygon[j][1];

            // Check if ray crosses this edge
            if (((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }

            j = i;
        }

        return inside;
    }

    /**
     * Calculates the centroid of a polygon using the standard algorithm.
     *
     * @param lines the polygon edges
     * @return the centroid point
     *
     * @see <a href="https://stackoverflow.com/questions/19766485">
     *      Centroid Calculation</a>
     */
    public static Point2D calculateCentroid(List<Line> lines) {
        List<Double> xCoords = new ArrayList<>();
        List<Double> yCoords = new ArrayList<>();

        for (Line line : lines) {
            xCoords.add(line.getStartX());
            xCoords.add(line.getEndX());
            yCoords.add(line.getStartY());
            yCoords.add(line.getEndY());
        }

        double[] x = Util.convertDoubles(xCoords);
        double[] y = Util.convertDoubles(yCoords);

        return calculateCentroidFromArrays(x, y);
    }

    /**
     * Calculates the centroid for GGT files using ordered points.
     *
     * GGT files have a specific point ordering that requires different handling.
     *
     * @param lines the polygon edges
     * @return the centroid point
     */
    public static Point2D calculateCentroidGGT(List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            return new Point2D(0, 0);
        }

        List<Point2D> orderedPoints = extractOrderedPoints(lines);

        if (orderedPoints.size() < 3) {
            return new Point2D(0, 0);
        }

        double[] x = new double[orderedPoints.size()];
        double[] y = new double[orderedPoints.size()];

        for (int i = 0; i < orderedPoints.size(); i++) {
            x[i] = orderedPoints.get(i).getX();
            y[i] = orderedPoints.get(i).getY();
        }

        return calculateCentroidFromArrays(x, y);
    }

    // ============================================
    // GETTERS & SETTERS
    // ============================================

    public Integer getId() {
        return id;
    }

    public List<Line> getLines() {
        return new ArrayList<>(lines); // Return defensive copy
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
        if (label != null) {
            label.changeLabelPosition(center);
        }
    }

    public Point2D getCenter() {
        return center;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public boolean isCenterInside() {
        return calculatedCenterPointIsInside;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    public boolean isPrinted() {
        return isPrinted;
    }

    public void setPrinted(boolean printed) {
        this.isPrinted = printed;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    /**
     * Generates a random semi-transparent color for visualization.
     */
    private Color generateRandomColor() {
        Random random = new Random();
        return Color.rgb(
                random.nextInt(256),
                random.nextInt(100),
                random.nextInt(256),
                0.5
        );
    }

    /**
     * Analyzes the path to calculate bounds, center, and other properties.
     */
    private void analyzePath() {
        calculateBounds();
        calculateCenter();
        checkCenterInside();
    }

    /**
     * Calculates the bounding box of the pattern.
     */
    private void calculateBounds() {
        double xMax = Double.MIN_VALUE;
        double yMax = Double.MIN_VALUE;
        double xMin = Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;

        for (Line line : lines) {
            xMin = Math.min(xMin, Math.min(line.getStartX(), line.getEndX()));
            yMin = Math.min(yMin, Math.min(line.getStartY(), line.getEndY()));
            xMax = Math.max(xMax, Math.max(line.getStartX(), line.getEndX()));
            yMax = Math.max(yMax, Math.max(line.getStartY(), line.getEndY()));
        }

        this.minX = xMin;
        this.maxX = xMax;
        this.minY = yMin;
        this.maxY = yMax;
        this.bounds = new BoundingBox(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    /**
     * Calculates the center point using appropriate algorithm.
     */
    private void calculateCenter() {
        this.center = isGGTFile
                ? calculateCentroidGGT(lines)
                : calculateCentroid(lines);
    }

    /**
     * Checks if the calculated center is inside the polygon.
     */
    private void checkCenterInside() {
        this.calculatedCenterPointIsInside = containsPoint(lines, center);
    }

    /**
     * Converts lines to a 2D array for polygon algorithms.
     */
    private static double[][] linesToPolygonArray(List<Line> lines) {
        double[][] polygon = new double[lines.size()][2];
        for (int i = 0; i < lines.size(); i++) {
            polygon[i][0] = lines.get(i).getStartX();
            polygon[i][1] = lines.get(i).getStartY();
        }
        return polygon;
    }

    /**
     * Extracts ordered points from lines for GGT format.
     */
    private static List<Point2D> extractOrderedPoints(List<Line> lines) {
        List<Point2D> points = new ArrayList<>();

        // Add first point
        points.add(new Point2D(
                lines.get(0).getStartX(),
                lines.get(0).getStartY()
        ));

        // Add subsequent end points
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            Point2D endPoint = new Point2D(line.getEndX(), line.getEndY());

            // Don't add if it closes the polygon
            if (i < lines.size() - 1 || !endPoint.equals(points.get(0))) {
                points.add(endPoint);
            }
        }

        return points;
    }

    /**
     * Core centroid calculation from coordinate arrays.
     */
    private static Point2D calculateCentroidFromArrays(double[] x, double[] y) {
        int n = x.length;

        // Calculate signed area
        double area = 0.0;
        for (int i = 0, j = 1; i < n; i++) {
            area += x[i] * y[j] - x[j] * y[i];
            j = (j + 1) % n;
        }
        area *= 0.5;

        // Handle degenerate polygon (zero area)
        if (Math.abs(area) < CENTROID_AREA_THRESHOLD) {
            return calculateSimpleAverage(x, y);
        }

        // Calculate centroid coordinates
        double cx = 0.0, cy = 0.0;
        for (int i = 0, j = 1; i < n; i++) {
            double t = x[i] * y[j] - x[j] * y[i];
            cx += (x[i] + x[j]) * t;
            cy += (y[i] + y[j]) * t;
            j = (j + 1) % n;
        }

        return new Point2D(cx / (6.0 * area), cy / (6.0 * area));
    }

    /**
     * Calculates simple average when area is too small.
     */
    private static Point2D calculateSimpleAverage(double[] x, double[] y) {
        double sumX = 0, sumY = 0;
        for (int i = 0; i < x.length; i++) {
            sumX += x[i];
            sumY += y[i];
        }
        return new Point2D(sumX / x.length, sumY / y.length);
    }

    // ============================================
    // CENTER RELOCATION (Refactored)
    // ============================================

    /**
     * Relocates the center along the X axis by finding intersection points.
     *
     * This ensures the center is properly positioned within concave polygons
     * by finding where a horizontal line through the center intersects the
     * polygon boundaries.
     */
    public void relocateCenterX() {
        CenterRelocator relocator = new CenterRelocator(
                lines,
                center,
                calculatedCenterPointIsInside,
                maxX
        );

        this.center = relocator.relocateAlongX();
        this.calculatedCenterPointIsInside = relocator.isCenterInside();

        relocateCenterY();
    }

    /**
     * Relocates the center along the Y axis.
     *
     * Similar to X relocation but works along the vertical axis.
     */
    public void relocateCenterY() {
        CenterRelocator relocator = new CenterRelocator(
                lines,
                center,
                calculatedCenterPointIsInside,
                maxY
        );

        this.center = relocator.relocateAlongY();
    }

    // ============================================
    // INNER CLASS: Center Relocation Logic
    // ============================================




    // ============================================
    // OBJECT METHODS
    // ============================================

    @Override
    public String toString() {
        return String.format("Pattern[id=%d, center=%s, valid=%s]",
                id, center, isValid());
    }

    /**
     * Prints line coordinates for debugging.
     */
    public void printLinePoints() {
        StringBuilder sb = new StringBuilder();
        for (Line line : lines) {
            sb.append(String.format("(%.2f,%.2f)-(%.2f,%.2f) ",
                    line.getStartX(), line.getStartY(),
                    line.getEndX(), line.getEndY()));
        }
        System.out.println(sb.toString());
    }
}