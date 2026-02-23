package tr.com.logidex.cad.geometry;

public final class BoundingBox {
    private final double minX;
    private final double minY;
    private final double width;
    private final double height;

    public BoundingBox(double minX, double minY, double width, double height) {
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
    }

    public double getMinX()   { return minX; }
    public double getMinY()   { return minY; }
    public double getWidth()  { return width; }
    public double getHeight() { return height; }
    public double getMaxX()   { return minX + width; }
    public double getMaxY()   { return minY + height; }

    @Override
    public String toString() {
        return "BoundingBox [minX=" + minX + ", minY=" + minY +
               ", width=" + width + ", height=" + height + "]";
    }
}