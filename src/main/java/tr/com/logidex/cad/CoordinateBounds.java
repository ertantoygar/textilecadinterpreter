package tr.com.logidex.cad;

public class CoordinateBounds {
    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;

    public CoordinateBounds(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

	@Override
	public String toString() {
		return "CoordinateBounds [minX=" + minX + ", maxX=" + maxX + ", minY=" + minY + ", maxY=" + maxY + "]";
	}
    
    
    
}
