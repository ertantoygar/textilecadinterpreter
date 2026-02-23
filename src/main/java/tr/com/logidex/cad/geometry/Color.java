package tr.com.logidex.cad.geometry;

import java.util.Objects;

public final class Color {
    private final double red;
    private final double green;
    private final double blue;
    private final double opacity;

    public static final Color WHITE       = new Color(1.0, 1.0, 1.0, 1.0);
    public static final Color BLACK       = new Color(0.0, 0.0, 0.0, 1.0);
    public static final Color RED         = new Color(1.0, 0.0, 0.0, 1.0);
    public static final Color GREEN       = new Color(0.0, 128.0 / 255.0, 0.0, 1.0);
    public static final Color BLUE        = new Color(0.0, 0.0, 1.0, 1.0);
    public static final Color ORANGE      = new Color(1.0, 200.0 / 255.0, 0.0, 1.0);
    public static final Color LIGHTGRAY   = new Color(211.0 / 255.0, 211.0 / 255.0, 211.0 / 255.0, 1.0);
    public static final Color TRANSPARENT = new Color(0.0, 0.0, 0.0, 0.0);

    private Color(double red, double green, double blue, double opacity) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.opacity = opacity;
    }

    public static Color rgb(int red, int green, int blue, double opacity) {
        return new Color(red / 255.0, green / 255.0, blue / 255.0, opacity);
    }

    public static Color rgb(int red, int green, int blue) {
        return rgb(red, green, blue, 1.0);
    }

    public static Color color(double red, double green, double blue, double opacity) {
        return new Color(red, green, blue, opacity);
    }

    public static Color gray(double brightness) {
        return new Color(brightness, brightness, brightness, 1.0);
    }

    public double getRed()     { return red; }
    public double getGreen()   { return green; }
    public double getBlue()    { return blue; }
    public double getOpacity() { return opacity; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Color other)) return false;
        return Double.compare(other.red, red) == 0 &&
               Double.compare(other.green, green) == 0 &&
               Double.compare(other.blue, blue) == 0 &&
               Double.compare(other.opacity, opacity) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(red, green, blue, opacity);
    }

    @Override
    public String toString() {
        return String.format("Color[red=%.4f, green=%.4f, blue=%.4f, opacity=%.4f]",
                red, green, blue, opacity);
    }
}