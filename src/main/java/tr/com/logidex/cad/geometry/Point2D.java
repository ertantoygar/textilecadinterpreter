package tr.com.logidex.cad.geometry;

import java.util.Objects;

public final class Point2D {
    private final double x;
    private final double y;

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }

    public double distance(Point2D other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculates the angle (in degrees) at this point formed by
     * the two rays from this point to p1 and from this point to p2.
     * Returns a value in [0, 180].
     */
    public double angle(Point2D p1, Point2D p2) {
        double ax = p1.x - this.x;
        double ay = p1.y - this.y;
        double bx = p2.x - this.x;
        double by = p2.y - this.y;

        double dot = ax * bx + ay * by;
        double cross = ax * by - ay * bx;
        return Math.toDegrees(Math.atan2(Math.abs(cross), dot));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point2D other)) return false;
        return Double.compare(other.x, x) == 0 &&
               Double.compare(other.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Point2D [x = " + x + ", y = " + y + "]";
    }
}