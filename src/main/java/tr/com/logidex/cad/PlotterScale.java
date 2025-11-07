package tr.com.logidex.cad;

import java.util.Optional;

public enum PlotterScale {

    DEFAULT(1.016),
    NONE(1.0);

    private final double value;
    PlotterScale(double i) {
        value = i;

    }

    public double getValue() {
        return value;
    }

    public static Optional<PlotterScale> find(String name) {
        if (name == null) return Optional.empty();

        return java.util.Arrays.stream(PlotterScale.values())
                .filter(p -> p.name().equals(name))
                .findFirst();
    }
}
