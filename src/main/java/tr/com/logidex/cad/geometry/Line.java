package tr.com.logidex.cad.geometry;

import java.util.Objects;

public class Line {
    private double startX;
    private double startY;
    private double endX;
    private double endY;

    public Line(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }

    public void setStartX(double startX) { this.startX = startX; }
    public void setStartY(double startY) { this.startY = startY; }
    public void setEndX(double endX) { this.endX = endX; }
    public void setEndY(double endY) { this.endY = endY; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Line other)) return false;
        return Double.compare(other.startX, startX) == 0 &&
               Double.compare(other.startY, startY) == 0 &&
               Double.compare(other.endX, endX) == 0 &&
               Double.compare(other.endY, endY) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startX, startY, endX, endY);
    }

    @Override
    public String toString() {
        return "Line[startX=" + startX + ", startY=" + startY +
               ", endX=" + endX + ", endY=" + endY + "]";
    }
}