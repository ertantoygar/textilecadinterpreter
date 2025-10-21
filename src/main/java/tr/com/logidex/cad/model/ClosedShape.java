package tr.com.logidex.cad.model;

import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.helper.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a closed polygon shape in a CAD system.
 * Provides functionality for centroid calculation, point-in-polygon detection,
 * and shape transformations.
 */
public class ClosedShape {

    // Constants
    private static final double MIN_VALID_AREA = 850;
    private static final double MAX_VALID_AREA = 1_500_000;
    private static final double AREA_EPSILON = 0.0001;
    private static final double DEFAULT_OPACITY = 0.5;

    // Core properties
    private final List<Line> lines;
    private final boolean isGGTFile;
    private final Color originalColor;

    // Mutable state
    private Lbl label;
    private Color color;
    private Point2D center;
    private BoundingBox bounds;
    private boolean calculatedCenterPointIsInThisShape;
    private boolean shapeSelected;
    private boolean shapePrinted;
    private Integer id;

    // Bounds cache
    private double minX, maxX, minY, maxY;

    public ClosedShape(List<Line> lines, boolean isGGTFile) {
        this.lines = new ArrayList<>(lines);
        this.isGGTFile = isGGTFile;
        this.color = generateRandomColor();
        this.originalColor = this.color;
        analyzePath();
    }

    // ==================== Static Utility Methods ====================

    /**
     * Performs the even-odd-rule ray casting algorithm to determine if a point
     * is inside a polygon.
     *
     * @param lines The lines forming the polygon
     * @param p The point to test
     * @return true if the point is inside the polygon
     */
    public static boolean pointInPolygon(List<Line> lines, Point2D p) {
        double[][] polygon = extractPolygonCoordinates(lines);
        double[] point = {p.getX(), p.getY()};

        boolean odd = false;
        int j = polygon.length - 1;

        for (int i = 0; i < polygon.length; i++) {
            if (((polygon[i][1] > point[1]) != (polygon[j][1] > point[1])) &&
                    (point[0] < (polygon[j][0] - polygon[i][0]) * (point[1] - polygon[i][1]) /
                            (polygon[j][1] - polygon[i][1]) + polygon[i][0])) {
                odd = !odd;
            }
            j = i;
        }

        return odd;
    }

    /**
     * Calculates the centroid of a polygon using the standard algorithm.
     */
    public static Point2D calculateCentroid(List<Line> lines) {
        ArrayList<Double> xP = new ArrayList<>();
        ArrayList<Double> yP = new ArrayList<>();

        lines.forEach(line -> {
            double x1 = line.getStartX();
            double y1 = line.getStartY();
            double x2 = line.getEndX();
            double y2 = line.getEndY();
            xP.add(x2);
            xP.add(x1);
            yP.add(y2);
            yP.add(y1);
        });

        double[] x = Util.convertDoubles(xP);
        double[] y = Util.convertDoubles(yP);

        return calculateCentroidFromCoordinates(x, y);
    }

    /**
     * Calculates the centroid for GGT file format with ordered point handling.
     */
    public static Point2D calculateCentroidGGT(List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            return new Point2D(0, 0);
        }

        List<Point2D> orderedPoints = new ArrayList<>();
        orderedPoints.add(new Point2D(lines.get(0).getStartX(), lines.get(0).getStartY()));

        for (int i = 0; i < lines.size(); i++) {
            Line currentLine = lines.get(i);
            Point2D endPoint = new Point2D(currentLine.getEndX(), currentLine.getEndY());

            if (i < lines.size() - 1 || !endPoint.equals(orderedPoints.get(0))) {
                orderedPoints.add(endPoint);
            }
        }

        int n = orderedPoints.size();
        if (n < 3) {
            return new Point2D(0, 0);
        }

        double[] x = new double[n];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = orderedPoints.get(i).getX();
            y[i] = orderedPoints.get(i).getY();
        }

        return calculateCentroidFromCoordinates(x, y);
    }

    // ==================== Private Static Helpers ====================

    private static double[][] extractPolygonCoordinates(List<Line> lines) {
        double[][] polygon = new double[lines.size()][2];
        for (int i = 0; i < lines.size(); i++) {
            polygon[i][0] = lines.get(i).getStartX();
            polygon[i][1] = lines.get(i).getStartY();
        }
        return polygon;
    }

    private static Point2D calculateCentroidFromCoordinates(double[] x, double[] y) {
        int n = x.length;
        double a, cx, cy, t;
        int i, i1;

        /* First calculate the polygon's signed area A */
        a = 0.0f;
        i1 = 1;
        for (i = 0; i < n; i++) {
            a += x[i] * y[i1] - x[i1] * y[i];
            i1 = (i1 + 1) % n;
        }
        a *= 0.5;

        if (Math.abs(a) < AREA_EPSILON) {
            double avgX = 0, avgY = 0;
            for (i = 0; i < n; i++) {
                avgX += x[i];
                avgY += y[i];
            }
            return new Point2D(avgX / n, avgY / n);
        }

        /* Now calculate the centroid coordinates Cx and Cy */
        cx = cy = 0.0f;
        i1 = 1;
        for (i = 0; i < n; i++) {
            t = x[i] * y[i1] - x[i1] * y[i];
            cx += (x[i] + x[i1]) * t;
            cy += (y[i] + y[i1]) * t;
            i1 = (i1 + 1) % n;
        }
        cx = cx / (6.0f * a);
        cy = cy / (6.0f * a);

        return new Point2D(cx, cy);
    }

    private static Color generateRandomColor() {
        Random random = new Random();
        return Color.rgb(
                random.nextInt(256),
                random.nextInt(100),
                random.nextInt(256),
                DEFAULT_OPACITY
        );
    }

    // ==================== Instance Methods ====================

    private void analyzePath() {
        calculateBounds();
        center = isGGTFile ? calculateCentroidGGT(lines) : calculateCentroid(lines);
        calculatedCenterPointIsInThisShape = pointInPolygon(lines, center);
        bounds = new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    private void calculateBounds() {
        double xMax = 0;
        double yMax = 0;
        double xMin = Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;

        for (Line l : lines) {
            double endpx = l.getEndX();
            double endpy = l.getEndY();
            double startpx = l.getStartX();
            double startpy = l.getStartY();
            if (endpx > xMax) {
                xMax = endpx;
            }
            if (endpy > yMax) {
                yMax = endpy;
            }
            if (startpx < xMin) {
                xMin = startpx;
            }
            if (startpy < yMin) {
                yMin = startpy;
            }
        }

        this.minX = xMin;
        this.maxX = xMax;
        this.minY = yMin;
        this.maxY = yMax;
    }

    public void reAnalyze() {
        analyzePath();
    }

    // ==================== Transformation Methods ====================

    public void mirrorX(double pastalWidth) {
        for (Line line : lines) {
            line.setStartX(pastalWidth - line.getStartX());
            line.setEndX(pastalWidth - line.getEndX());
        }
        analyzePath();
        relocateOriginX();
    }

    public void mirrorY(double pastalHeight) {
        for (Line line : lines) {
            line.setStartY(pastalHeight - line.getStartY());
            line.setEndY(pastalHeight - line.getEndY());
        }
        analyzePath();
        relocateOriginX();
    }

    /**
     * Relocates the origin along the X-axis by finding the true center
     * through boundary intersection detection.
     */
    public void relocateOriginX() {
        OriginRelocationResult result = findAxisIntersections(
                center.getX(),
                maxX,
                calculatedCenterPointIsInThisShape,
                AxisDirection.HORIZONTAL
        );

        if (result == null) return;

        double newCenterX = (result.firstIntersection + result.secondIntersection) / 2.0;
        center = new Point2D(newCenterX, center.getY());

        if (!calculatedCenterPointIsInThisShape) {
            calculatedCenterPointIsInThisShape = true;
        }

        relocateOriginY();
    }

    /**
     * Relocates the origin along the Y-axis by finding the true center
     * through boundary intersection detection.
     */
    public void relocateOriginY() {
        OriginRelocationResult result = findAxisIntersections(
                center.getY(),
                maxY,
                calculatedCenterPointIsInThisShape,
                AxisDirection.VERTICAL
        );

        if (result == null) return;

        double newCenterY = (result.firstIntersection + result.secondIntersection) / 2.0;
        center = new Point2D(center.getX(), newCenterY);
    }

    private OriginRelocationResult findAxisIntersections(
            double originalCoord,
            double maxCoord,
            boolean startInside,
            AxisDirection axis) {

        int searchDistance = (int) maxCoord;
        int tryCount = 0;
        boolean forward = true;
        int lastDirection = 0;

        // Find first boundary crossing
        while (startInside ? isPointInside() : !isPointInside()) {
            if (tryCount++ >= searchDistance * 2) {
                System.err.println("Error while detecting center. There may be interwoven parts.");
                return null;
            }

            if (!isValidCenter()) return null;

            if (getAxisDistance(originalCoord, axis) >= searchDistance) {
                forward = false;
                resetAxisCoordinate(originalCoord, axis);
            }

            if (getAxisDistance(originalCoord, axis) <= -searchDistance) {
                forward = true;
                resetAxisCoordinate(originalCoord, axis);
            }

            moveCenter(forward ? 1 : -1, axis);
        }

        lastDirection = forward ? 1 : -1;
        double firstIntersection = getAxisCoordinate(axis);

        // Re-enter shape if started inside
        if (startInside) {
            while (!isPointInside()) {
                moveCenter(-lastDirection, axis);
            }
        }

        // Find second boundary crossing
        while (isPointInside()) {
            int direction = calculateExitDirection(lastDirection, startInside);
            moveCenter(direction, axis);
        }

        double secondIntersection = getAxisCoordinate(axis);

        return new OriginRelocationResult(firstIntersection, secondIntersection);
    }

    private boolean isPointInside() {
        return pointInPolygon(lines, center);
    }

    private boolean isValidCenter() {
        return center != null &&
                !Double.isInfinite(center.getX()) &&
                !Double.isInfinite(center.getY()) &&
                !Double.isNaN(center.getX()) &&
                !Double.isNaN(center.getY());
    }

    private double getAxisDistance(double original, AxisDirection axis) {
        return axis == AxisDirection.HORIZONTAL
                ? center.getX() - original
                : center.getY() - original;
    }

    private double getAxisCoordinate(AxisDirection axis) {
        return axis == AxisDirection.HORIZONTAL ? center.getX() : center.getY();
    }

    private void resetAxisCoordinate(double value, AxisDirection axis) {
        center = axis == AxisDirection.HORIZONTAL
                ? new Point2D(value, center.getY())
                : new Point2D(center.getX(), value);
    }

    private void moveCenter(int delta, AxisDirection axis) {
        center = axis == AxisDirection.HORIZONTAL
                ? new Point2D(center.getX() + delta, center.getY())
                : new Point2D(center.getX(), center.getY() + delta);
    }

    private int calculateExitDirection(int lastDirection, boolean startedInside) {
        if (lastDirection == 1) {
            return startedInside ? -1 : 1;
        } else {
            return startedInside ? 1 : -1;
        }
    }

    // ==================== Validation ====================

    public boolean isValidPiece() {
        double area = bounds.getWidth() * bounds.getHeight();
        return area > MIN_VALID_AREA && area < MAX_VALID_AREA;
    }

    // ==================== Getters and Setters ====================

    public Lbl getLabel() {
        return label;
    }

    public void setLabel(Lbl label) {
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

    public boolean isCalculatedCenterPointIsInThisShape() {
        return calculatedCenterPointIsInThisShape;
    }

    public List<Line> getLines() {
        return new ArrayList<>(lines);
    }

    public boolean isShapeSelected() {
        return shapeSelected;
    }

    public void setShapeSelected(boolean shapeSelected) {
        this.shapeSelected = shapeSelected;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void restoreColor() {
        this.color = originalColor;
    }

    public boolean isShapePrinted() {
        return shapePrinted;
    }

    public void setShapePrinted(boolean shapePrinted) {
        this.shapePrinted = shapePrinted;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    // ==================== Utility Methods ====================

    @Override
    public String toString() {
        return String.format("ClosedShape[id=%s, label=%s, center=%s]", id, label, center);
    }

    public void printLinePoints() {
        StringBuilder sb = new StringBuilder();
        for (Line line : lines) {
            sb.append(line.getEndX()).append(" , ").append(line.getEndY()).append(" , ")
                    .append(line.getStartX()).append(" , ").append(line.getStartY()).append(" , ");
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 3);
        }

        System.out.println(sb.toString());
    }

    // ==================== Inner Classes ====================

    private enum AxisDirection {
        HORIZONTAL, VERTICAL
    }

    private static class OriginRelocationResult {
        final double firstIntersection;
        final double secondIntersection;

        OriginRelocationResult(double first, double second) {
            this.firstIntersection = first;
            this.secondIntersection = second;
        }
    }
}