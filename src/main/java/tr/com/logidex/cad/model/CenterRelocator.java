package tr.com.logidex.cad.model;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;

import java.util.List;


public  class CenterRelocator {

    private static final int MAX_CENTER_SEARCH_ATTEMPTS_MULTIPLIER = 2;


    private final List<Line> lines;
    private Point2D center;
    private final boolean initiallyInside;
    private final double searchDistance;
    private boolean centerInside;

    public CenterRelocator(List<Line> lines, Point2D center,
                           boolean initiallyInside, double searchDistance) {
        this.lines = lines;
        this.center = center;
        this.initiallyInside = initiallyInside;
        this.centerInside = initiallyInside;
        this.searchDistance = searchDistance;
    }

    public Point2D relocateAlongX() {
        return relocateAlong(Axis.X);
    }

    public Point2D relocateAlongY() {
        return relocateAlong(Axis.Y);
    }

    public boolean isCenterInside() {
        return centerInside;
    }

    private Point2D relocateAlong(Axis axis) {
        double originalCoord = axis.getCoordinate(center);
        int direction = findInitialBoundary(axis, originalCoord);

        if (direction == 0) {
            System.err.println("Error: Could not find pattern boundary");
            return center;
        }

        double first = axis.getCoordinate(center);
        double second = findOppositeBoundary(axis, direction);
        double newCoord = (first + second) / 2.0;

        centerInside = true;
        return axis.setCoordinate(center, newCoord);
    }

    private int findInitialBoundary(Axis axis, double originalCoord) {
        int attempts = 0;
        int maxAttempts = (int) (searchDistance * MAX_CENTER_SEARCH_ATTEMPTS_MULTIPLIER);
        boolean forward = true;

        boolean targetState = !initiallyInside;

        while (Pattern.containsPoint(lines, center) != targetState) {
            if (attempts++ >= maxAttempts) {
                return 0;
            }

            double current = axis.getCoordinate(center);

            if (Math.abs(current - originalCoord) >= searchDistance) {
                forward = !forward;
                center = axis.setCoordinate(center, originalCoord);
            }

            center = axis.move(center, forward ? 1 : -1);
        }

        return forward ? 1 : -1;
    }

    private double findOppositeBoundary(Axis axis, int direction) {
        // Re-enter if we started inside
        if (initiallyInside) {
            while (!Pattern.containsPoint(lines, center)) {
                center = axis.move(center, -direction);
            }
        }

        // Exit again
        while (Pattern.containsPoint(lines, center)) {
            int moveDirection = initiallyInside ? -direction : direction;
            center = axis.move(center, moveDirection);
        }

        return axis.getCoordinate(center);
    }
}
