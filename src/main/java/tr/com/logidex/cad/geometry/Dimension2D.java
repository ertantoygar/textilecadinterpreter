package tr.com.logidex.cad.geometry;

import java.util.Objects;

public final class Dimension2D {
    private final double width;
    private final double height;

    public Dimension2D(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double getWidth()  { return width; }
    public double getHeight() { return height; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dimension2D other)) return false;
        return Double.compare(other.width, width) == 0 &&
               Double.compare(other.height, height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "Dimension2D [width = " + width + ", height = " + height + "]";
    }
}