package tr.com.logidex.cad.processor;

import tr.com.logidex.cad.geometry.Point2D;
import tr.com.logidex.cad.model.Lbl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds label routes for the supported strategies.
 */
final class LabelRoutePlanner {
    private static final Point2D ORIGIN = new Point2D(0, 0);
    private static final int TARGET_BAND_SIZE = 12;
    private static final int MIN_BAND_SIZE_FOR_GAP_SPLIT = 6;
    private static final int LOCAL_IMPROVEMENT_WINDOW = 8;
    private static final double X_AXIS_WEIGHT = 1.0;
    private static final double Y_AXIS_WEIGHT = 1.0;
    private static final double X_ROLLBACK_PENALTY = 2.0;

    private LabelRoutePlanner() {
    }

    static RoutePlan plan(List<Lbl> labels,
                          double drawingWidth,
                          double legacyStripWidth,
                          RouteStrategy requestedStrategy) {
        RouteStrategy strategy = requestedStrategy == null ? RouteStrategy.LEGACY_SNAKE : requestedStrategy;
        List<Lbl> sanitizedLabels = sanitizeLabels(labels);
        if (sanitizedLabels.isEmpty()) {
            return new RoutePlan(strategy, RouteStrategy.LEGACY_SNAKE, 0.0, 0.0, 0.0, 0, Collections.emptyList());
        }

        List<Lbl> pinnedLabels = extractPinnedLabels(sanitizedLabels);
        List<Lbl> routeableLabels = extractRouteableLabels(sanitizedLabels);

        List<Lbl> legacyRoute = prependPinnedLabels(
                pinnedLabels,
                organizeLegacySnake(routeableLabels, drawingWidth, legacyStripWidth)
        );
        List<Lbl> weightedRoute = prependPinnedLabels(
                pinnedLabels,
                buildWeightedBandedRoute(routeableLabels, drawingWidth, legacyStripWidth)
        );

        double legacyCost = calculateTravelCost(legacyRoute);
        double weightedCost = calculateTravelCost(weightedRoute);

        RouteStrategy appliedStrategy = selectAppliedStrategy(strategy, legacyCost, weightedCost);
        List<Lbl> selectedRoute = appliedStrategy == RouteStrategy.WEIGHTED_BANDED ? weightedRoute : legacyRoute;
        double selectedCost = appliedStrategy == RouteStrategy.WEIGHTED_BANDED ? weightedCost : legacyCost;

        return new RoutePlan(
                strategy,
                appliedStrategy,
                legacyCost,
                weightedCost,
                selectedCost,
                selectedRoute.size(),
                selectedRoute
        );
    }

    static List<Lbl> organizeLegacySnake(List<Lbl> labels, double drawingWidth, double stripWidth) {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyList();
        }

        int stripCount = Math.max(1, (int) Math.ceil(drawingWidth / stripWidth));
        List<List<Lbl>> strips = new ArrayList<>(stripCount);
        for (int index = 0; index < stripCount; index++) {
            strips.add(new ArrayList<>());
        }

        for (Lbl label : labels) {
            double x = label.getPosition().getX();
            int stripIndex = (int) (x / stripWidth);
            if (stripIndex == stripCount) {
                stripIndex = stripCount - 1;
            }
            if (stripIndex >= 0 && stripIndex < stripCount) {
                strips.get(stripIndex).add(label);
            }
        }

        for (int index = 0; index < strips.size(); index++) {
            strips.get(index).sort(Comparator.comparingDouble(l -> l.getPosition().getY()));
            if (index % 2 == 1) {
                Collections.reverse(strips.get(index));
            }
        }

        List<Lbl> result = new ArrayList<>(labels.size());
        for (List<Lbl> strip : strips) {
            result.addAll(strip);
        }
        return result;
    }

    private static RouteStrategy selectAppliedStrategy(RouteStrategy requestedStrategy,
                                                       double legacyCost,
                                                       double weightedCost) {
        return switch (requestedStrategy) {
            case LEGACY_SNAKE, COMPARE -> RouteStrategy.LEGACY_SNAKE;
            case WEIGHTED_BANDED -> RouteStrategy.WEIGHTED_BANDED;
            case AUTO_SAFE -> weightedCost + 1e-9 < legacyCost
                    ? RouteStrategy.WEIGHTED_BANDED
                    : RouteStrategy.LEGACY_SNAKE;
        };
    }

    private static List<Lbl> buildWeightedBandedRoute(List<Lbl> labels,
                                                      double drawingWidth,
                                                      double legacyStripWidth) {
        if (labels.isEmpty()) {
            return Collections.emptyList();
        }
        if (labels.size() == 1) {
            return new ArrayList<>(labels);
        }

        List<List<Lbl>> bands = createBands(labels, drawingWidth, legacyStripWidth);
        if (bands.size() <= 1) {
            return improveLocally(sortBandByY(labels, true));
        }

        List<List<Lbl>> ascendingRoutes = new ArrayList<>(bands.size());
        List<List<Lbl>> descendingRoutes = new ArrayList<>(bands.size());
        List<Point2D> ascendingStarts = new ArrayList<>(bands.size());
        List<Point2D> ascendingEnds = new ArrayList<>(bands.size());
        List<Point2D> descendingStarts = new ArrayList<>(bands.size());
        List<Point2D> descendingEnds = new ArrayList<>(bands.size());

        for (List<Lbl> band : bands) {
            List<Lbl> ascending = sortBandByY(band, true);
            List<Lbl> descending = sortBandByY(band, false);
            ascendingRoutes.add(ascending);
            descendingRoutes.add(descending);
            ascendingStarts.add(ascending.get(0).getPosition());
            ascendingEnds.add(ascending.get(ascending.size() - 1).getPosition());
            descendingStarts.add(descending.get(0).getPosition());
            descendingEnds.add(descending.get(descending.size() - 1).getPosition());
        }

        double[][] costs = new double[bands.size()][2];
        int[][] previousState = new int[bands.size()][2];
        previousState[0][0] = -1;
        previousState[0][1] = -1;
        costs[0][0] = calculateHeuristicCost(ORIGIN, ascendingStarts.get(0));
        costs[0][1] = calculateHeuristicCost(ORIGIN, descendingStarts.get(0));

        for (int bandIndex = 1; bandIndex < bands.size(); bandIndex++) {
            double fromAscendingToAscending = costs[bandIndex - 1][0]
                    + calculateHeuristicCost(ascendingEnds.get(bandIndex - 1), ascendingStarts.get(bandIndex));
            double fromDescendingToAscending = costs[bandIndex - 1][1]
                    + calculateHeuristicCost(descendingEnds.get(bandIndex - 1), ascendingStarts.get(bandIndex));
            if (fromAscendingToAscending <= fromDescendingToAscending) {
                costs[bandIndex][0] = fromAscendingToAscending;
                previousState[bandIndex][0] = 0;
            } else {
                costs[bandIndex][0] = fromDescendingToAscending;
                previousState[bandIndex][0] = 1;
            }

            double fromAscendingToDescending = costs[bandIndex - 1][0]
                    + calculateHeuristicCost(ascendingEnds.get(bandIndex - 1), descendingStarts.get(bandIndex));
            double fromDescendingToDescending = costs[bandIndex - 1][1]
                    + calculateHeuristicCost(descendingEnds.get(bandIndex - 1), descendingStarts.get(bandIndex));
            if (fromAscendingToDescending <= fromDescendingToDescending) {
                costs[bandIndex][1] = fromAscendingToDescending;
                previousState[bandIndex][1] = 0;
            } else {
                costs[bandIndex][1] = fromDescendingToDescending;
                previousState[bandIndex][1] = 1;
            }
        }

        int state = costs[bands.size() - 1][0] <= costs[bands.size() - 1][1] ? 0 : 1;
        List<Integer> states = new ArrayList<>(bands.size());
        for (int bandIndex = bands.size() - 1; bandIndex >= 0; bandIndex--) {
            states.add(state);
            if (bandIndex > 0) {
                state = previousState[bandIndex][state];
            }
        }
        Collections.reverse(states);

        List<Lbl> route = new ArrayList<>(labels.size());
        for (int bandIndex = 0; bandIndex < bands.size(); bandIndex++) {
            route.addAll(states.get(bandIndex) == 0
                    ? ascendingRoutes.get(bandIndex)
                    : descendingRoutes.get(bandIndex));
        }

        return improveLocally(route);
    }

    private static List<List<Lbl>> createBands(List<Lbl> labels,
                                               double drawingWidth,
                                               double legacyStripWidth) {
        List<Lbl> sortedByX = new ArrayList<>(labels);
        sortedByX.sort(Comparator
                .comparingDouble((Lbl label) -> label.getPosition().getX())
                .thenComparingDouble(label -> label.getPosition().getY()));

        double minBandGap = Math.max(legacyStripWidth * 2.0, drawingWidth / 80.0);
        List<List<Lbl>> bands = new ArrayList<>();
        List<Lbl> currentBand = new ArrayList<>();
        double previousX = Double.NaN;

        for (Lbl label : sortedByX) {
            boolean startNewBand = false;
            if (!currentBand.isEmpty()) {
                if (currentBand.size() >= TARGET_BAND_SIZE) {
                    startNewBand = true;
                } else if (!Double.isNaN(previousX)
                        && label.getPosition().getX() - previousX >= minBandGap
                        && currentBand.size() >= MIN_BAND_SIZE_FOR_GAP_SPLIT) {
                    startNewBand = true;
                }
            }

            if (startNewBand) {
                bands.add(currentBand);
                currentBand = new ArrayList<>();
            }

            currentBand.add(label);
            previousX = label.getPosition().getX();
        }

        if (!currentBand.isEmpty()) {
            bands.add(currentBand);
        }

        return bands;
    }

    private static List<Lbl> sortBandByY(List<Lbl> band, boolean ascending) {
        List<Lbl> orderedBand = new ArrayList<>(band);
        orderedBand.sort(Comparator
                .comparingDouble((Lbl label) -> label.getPosition().getY())
                .thenComparingDouble(label -> label.getPosition().getX()));
        if (!ascending) {
            Collections.reverse(orderedBand);
        }
        return orderedBand;
    }

    private static List<Lbl> improveLocally(List<Lbl> route) {
        List<Lbl> bestRoute = new ArrayList<>(route);
        double bestCost = calculateHeuristicCost(bestRoute);
        boolean improved = true;

        while (improved) {
            improved = false;
            for (int insertIndex = 0; insertIndex < bestRoute.size() - 1; insertIndex++) {
                int searchLimit = Math.min(bestRoute.size(), insertIndex + LOCAL_IMPROVEMENT_WINDOW);
                for (int sourceIndex = insertIndex + 1; sourceIndex < searchLimit; sourceIndex++) {
                    List<Lbl> candidate = new ArrayList<>(bestRoute);
                    Lbl movedLabel = candidate.remove(sourceIndex);
                    candidate.add(insertIndex, movedLabel);

                    double candidateCost = calculateHeuristicCost(candidate);
                    if (candidateCost + 1e-9 < bestCost) {
                        bestRoute = candidate;
                        bestCost = candidateCost;
                        improved = true;
                    }
                }
            }
        }

        return bestRoute;
    }

    private static double calculateTravelCost(List<Lbl> labels) {
        Point2D previousPoint = ORIGIN;
        double totalCost = 0.0;
        for (Lbl label : labels) {
            totalCost += X_AXIS_WEIGHT * Math.abs(label.getPosition().getX() - previousPoint.getX());
            totalCost += Y_AXIS_WEIGHT * Math.abs(label.getPosition().getY() - previousPoint.getY());
            previousPoint = label.getPosition();
        }
        return totalCost;
    }

    private static double calculateHeuristicCost(List<Lbl> labels) {
        Point2D previousPoint = ORIGIN;
        double totalCost = 0.0;
        for (Lbl label : labels) {
            totalCost += calculateHeuristicCost(previousPoint, label.getPosition());
            previousPoint = label.getPosition();
        }
        return totalCost;
    }

    private static double calculateHeuristicCost(Point2D from, Point2D to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        return X_AXIS_WEIGHT * Math.abs(dx)
                + Y_AXIS_WEIGHT * Math.abs(dy)
                + X_ROLLBACK_PENALTY * Math.max(0.0, -dx);
    }

    private static List<Lbl> sanitizeLabels(List<Lbl> labels) {
        List<Lbl> sanitized = new ArrayList<>();
        if (labels == null) {
            return sanitized;
        }
        for (Lbl label : labels) {
            if (label != null) {
                sanitized.add(label);
            }
        }
        return sanitized;
    }

    private static List<Lbl> extractPinnedLabels(List<Lbl> labels) {
        List<Lbl> pinnedLabels = new ArrayList<>();
        for (Lbl label : labels) {
            if (FileProcessor.REFERENCE_SIGN.equals(label.getText())) {
                pinnedLabels.add(label);
            }
        }
        return pinnedLabels;
    }

    private static List<Lbl> extractRouteableLabels(List<Lbl> labels) {
        List<Lbl> routeableLabels = new ArrayList<>();
        for (Lbl label : labels) {
            if (!FileProcessor.REFERENCE_SIGN.equals(label.getText())) {
                routeableLabels.add(label);
            }
        }
        return routeableLabels;
    }

    private static List<Lbl> prependPinnedLabels(List<Lbl> pinnedLabels, List<Lbl> routeableLabels) {
        if (pinnedLabels.isEmpty()) {
            return routeableLabels;
        }

        List<Lbl> orderedLabels = new ArrayList<>(pinnedLabels.size() + routeableLabels.size());
        orderedLabels.addAll(pinnedLabels);
        orderedLabels.addAll(routeableLabels);
        return orderedLabels;
    }
}
