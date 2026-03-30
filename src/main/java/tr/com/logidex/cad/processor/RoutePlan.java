package tr.com.logidex.cad.processor;

import tr.com.logidex.cad.model.Lbl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the selected route together with comparison metrics.
 */
public final class RoutePlan {
    private final RouteStrategy requestedStrategy;
    private final RouteStrategy appliedStrategy;
    private final double legacyCost;
    private final double weightedBandedCost;
    private final double selectedCost;
    private final int labelCount;
    private final List<Lbl> orderedLabels;

    RoutePlan(RouteStrategy requestedStrategy,
              RouteStrategy appliedStrategy,
              double legacyCost,
              double weightedBandedCost,
              double selectedCost,
              int labelCount,
              List<Lbl> orderedLabels) {
        this.requestedStrategy = requestedStrategy;
        this.appliedStrategy = appliedStrategy;
        this.legacyCost = legacyCost;
        this.weightedBandedCost = weightedBandedCost;
        this.selectedCost = selectedCost;
        this.labelCount = labelCount;
        this.orderedLabels = Collections.unmodifiableList(new ArrayList<>(orderedLabels));
    }

    public RouteStrategy getRequestedStrategy() {
        return requestedStrategy;
    }

    public RouteStrategy getAppliedStrategy() {
        return appliedStrategy;
    }

    public double getLegacyCost() {
        return legacyCost;
    }

    public double getWeightedBandedCost() {
        return weightedBandedCost;
    }

    public double getSelectedCost() {
        return selectedCost;
    }

    public int getLabelCount() {
        return labelCount;
    }

    public List<Lbl> getOrderedLabels() {
        return orderedLabels;
    }
}
