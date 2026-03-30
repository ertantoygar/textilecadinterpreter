package tr.com.logidex;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tr.com.logidex.cad.ColorTheme;
import tr.com.logidex.cad.geometry.Point2D;
import tr.com.logidex.cad.helper.PieceSequenceNumberCreator;
import tr.com.logidex.cad.model.Lbl;
import tr.com.logidex.cad.PlotterScale;
import tr.com.logidex.cad.processor.FileProcessor;
import tr.com.logidex.cad.processor.GGTFileProcessor;
import tr.com.logidex.cad.processor.GerberFileProcessor;
import tr.com.logidex.cad.processor.HPGLFileProcessor;
import tr.com.logidex.cad.processor.RoutePlan;
import tr.com.logidex.cad.processor.RouteStrategy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRouteStrategies {
    private static final String ROUTE_STRATEGY_PROPERTY = "cad.route.strategy";

    @BeforeEach
    public void setUp() {
        PieceSequenceNumberCreator.resetCounter();
        FileProcessor.unit = null;
        FileProcessor.plotterScale = PlotterScale.DEFAULT;
        FileProcessor.colorTheme = ColorTheme.LIGHT;
        FileProcessor.excludeReferenceSign = false;
        FileProcessor.contourBoundaries = false;
        System.clearProperty(ROUTE_STRATEGY_PROPERTY);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(ROUTE_STRATEGY_PROPERTY);
    }

    @Test
    public void compareModeKeepsLegacyRouteAcrossFlips() throws Exception {
        FileProcessor legacy = new GerberFileProcessor(Files.readString(Path.of("GEMINI.cut"), StandardCharsets.UTF_8));
        legacy.startFileProcessing();

        FileProcessor compare = new GerberFileProcessor(Files.readString(Path.of("GEMINI.cut"), StandardCharsets.UTF_8));
        compare.setRouteStrategy(RouteStrategy.COMPARE);
        compare.startFileProcessing();

        assertRouteEquals(legacy.getSortedAndOptimizedLbls(), compare.getSortedAndOptimizedLbls());
        assertEquals(RouteStrategy.COMPARE, compare.getLastRoutePlan().getRequestedStrategy());
        assertEquals(RouteStrategy.LEGACY_SNAKE, compare.getLastRoutePlan().getAppliedStrategy());

        legacy.invertFlipH();
        compare.invertFlipH();
        assertRouteEquals(legacy.getSortedAndOptimizedLbls(), compare.getSortedAndOptimizedLbls());

        legacy.invertFlipV();
        compare.invertFlipV();
        assertRouteEquals(legacy.getSortedAndOptimizedLbls(), compare.getSortedAndOptimizedLbls());

        legacy.invertFlipH();
        compare.invertFlipH();
        assertRouteEquals(legacy.getSortedAndOptimizedLbls(), compare.getSortedAndOptimizedLbls());

        legacy.invertFlipV();
        compare.invertFlipV();
        assertRouteEquals(legacy.getSortedAndOptimizedLbls(), compare.getSortedAndOptimizedLbls());
    }

    @Test
    public void autoSafeNeverSelectsWorseCostAcrossAllFilesAndFlips() throws Exception {
        verifyAutoSafe("test.hpgl");
        verifyAutoSafe("GEMINI.cut");
        verifyAutoSafe("ggttest.ggt");
    }

    @Test
    public void routeStrategyCanBeConfiguredWithSystemProperty() throws Exception {
        System.setProperty(ROUTE_STRATEGY_PROPERTY, "weighted");
        FileProcessor processor = new HPGLFileProcessor(Files.readString(Path.of("test.hpgl"), StandardCharsets.UTF_8));
        assertEquals(RouteStrategy.WEIGHTED_BANDED, processor.getRouteStrategy());
    }

    private void verifyAutoSafe(String fileName) throws Exception {
        FileProcessor processor = createProcessor(fileName);
        processor.setRouteStrategy(RouteStrategy.AUTO_SAFE);
        processor.startFileProcessing();
        assertAutoSafeDecision(processor.getLastRoutePlan());

        processor.invertFlipH();
        assertAutoSafeDecision(processor.getLastRoutePlan());

        processor.invertFlipV();
        assertAutoSafeDecision(processor.getLastRoutePlan());

        processor.invertFlipH();
        assertAutoSafeDecision(processor.getLastRoutePlan());

        processor.invertFlipV();
        assertAutoSafeDecision(processor.getLastRoutePlan());
    }

    private FileProcessor createProcessor(String fileName) throws Exception {
        String content = Files.readString(Path.of(fileName), StandardCharsets.UTF_8);
        if (fileName.endsWith(".hpgl")) {
            return new HPGLFileProcessor(content);
        }
        if (fileName.endsWith(".cut")) {
            return new GerberFileProcessor(content);
        }
        if (fileName.endsWith(".ggt")) {
            return new GGTFileProcessor(content);
        }
        throw new IllegalArgumentException("Unsupported test file: " + fileName);
    }

    private void assertAutoSafeDecision(RoutePlan routePlan) {
        assertNotNull(routePlan);
        assertEquals(RouteStrategy.AUTO_SAFE, routePlan.getRequestedStrategy());
        assertTrue(routePlan.getSelectedCost() <= routePlan.getLegacyCost() + 0.0001);
    }

    private void assertRouteEquals(List<Lbl> expected, List<Lbl> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            Lbl expectedLabel = expected.get(index);
            Lbl actualLabel = actual.get(index);
            assertEquals(expectedLabel.getText(), actualLabel.getText());
            assertTrue(arePointsNearlyEqual(expectedLabel.getPosition(), actualLabel.getPosition(), 0.01));
        }
    }

    private boolean arePointsNearlyEqual(Point2D p1, Point2D p2, double epsilon) {
        return Math.abs(p1.getX() - p2.getX()) < epsilon
                && Math.abs(p1.getY() - p2.getY()) < epsilon;
    }
}
