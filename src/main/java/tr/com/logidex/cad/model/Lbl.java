package tr.com.logidex.cad.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import tr.com.logidex.cad.processor.FileProcessor;
import tr.com.logidex.cad.Unit;
import tr.com.logidex.cad.helper.Util;

import java.text.DecimalFormat;

/**
 * Represents a label in a CAD system with position, rotation, and dimension properties.
 * Labels can be associated with closed shapes and support position tracking and conversion.
 */
public class Lbl {

    // Formatting
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    private static final String POSITION_FORMAT = "%,.2f";

    // Core properties
    private String text;
    private final double angle;
    private final double origin;
    private final double width;
    private final double height;

    // Position tracking
    private final SimpleObjectProperty<Point2D> position;
    private final Point2D originalXY;

    // Associated shape
    private ClosedShape shape;

    public Lbl(String text, Point2D pos, double angle, double origin, double width, double height) {
        this.text = text;
        this.position = new SimpleObjectProperty<>(pos);
        this.angle = angle;
        this.origin = origin;
        this.width = width;
        this.height = height;
        this.originalXY = new Point2D(pos.getX(), pos.getY());
    }

    // ==================== Position Management ====================

    public Point2D getPosition() {
        return position.get();
    }

    public void setPosition(Point2D position) {
        this.position.set(position);
    }

    public SimpleObjectProperty<Point2D> positionProperty() {
        return position;
    }

    public Point2D getOriginalXY() {
        return originalXY;
    }

    /**
     * Offsets the label position by the specified deltas.
     *
     * @param xOffset The offset to apply to the x-coordinate
     * @param yOffset The offset to apply to the y-coordinate
     */
    public void offsetLabelPosition(double xOffset, double yOffset) {
        Point2D currentPos = position.get();
        position.set(new Point2D(currentPos.getX() + xOffset, currentPos.getY() + yOffset));
    }

    /**
     * Changes the label position to a new point.
     *
     * @param newPoint The new position for the label
     */
    public void changeLabelPosition(Point2D newPoint) {
        position.set(newPoint);
    }

    /**
     * Checks if the label position has been modified from its original position.
     *
     * @return true if the position has changed, false otherwise
     */
    public boolean isLabelPositionChanged() {
        return position.get().distance(originalXY) != 0;
    }

    // ==================== Position Formatting ====================

    /**
     * Returns a human-readable string representation of the current position,
     * with unit conversion applied based on the current file processor unit setting.
     *
     * @return Formatted position string (e.g., "x=10.50 y=20.75")
     */
    public String getPrintablePosition() {
        Point2D currentPos = position.get();
        double x = currentPos.getX();
        double y = currentPos.getY();

        if (FileProcessor.unit == Unit.IN) {
            x = Util.mmToInch(x);
            y = Util.mmToInch(y);
        }

        return String.format("x=%s y=%s",
                DECIMAL_FORMAT.format(x),
                DECIMAL_FORMAT.format(y));
    }

    // ==================== Text Management ====================

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    // ==================== Dimension Properties ====================

    public double getAngle() {
        return angle;
    }

    public double getOrigin() {
        return origin;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    // ==================== Shape Association ====================

    public ClosedShape getShape() {
        return shape;
    }

    public void setShape(ClosedShape shape) {
        this.shape = shape;
    }

    // ==================== Object Methods ====================

    @Override
    public String toString() {
        Point2D currentPos = position.get();
        String formattedX = String.format(POSITION_FORMAT, currentPos.getX());
        String formattedY = String.format(POSITION_FORMAT, currentPos.getY());
        return text + "\n[x=" + formattedX + " , y=" + formattedY + "]";
    }
}