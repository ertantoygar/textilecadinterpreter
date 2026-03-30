package tr.com.logidex.cad.processor;

import java.util.Locale;

/**
 * Selects how labels are ordered for robot travel.
 */
public enum RouteStrategy {
    LEGACY_SNAKE,
    WEIGHTED_BANDED,
    AUTO_SAFE,
    COMPARE;

    public static RouteStrategy fromValue(String value) {
        if (value == null || value.isBlank()) {
            return LEGACY_SNAKE;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "LEGACY", "LEGACY_SNAKE", "SNAKE" -> LEGACY_SNAKE;
            case "WEIGHTED", "WEIGHTED_BANDED", "BANDED" -> WEIGHTED_BANDED;
            case "AUTO", "AUTO_SAFE" -> AUTO_SAFE;
            case "COMPARE" -> COMPARE;
            default -> throw new IllegalArgumentException("Unknown route strategy: " + value);
        };
    }
}
