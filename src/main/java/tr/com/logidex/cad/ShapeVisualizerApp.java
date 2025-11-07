package tr.com.logidex.cad;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import tr.com.logidex.cad.model.ClosedShape;
import tr.com.logidex.cad.processor.FileProcessor;
import tr.com.logidex.cad.processor.HPGLFileProcessor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX Application to visualize ClosedShape objects with zoom, pan, and selection capabilities.
 */
public class ShapeVisualizerApp extends Application {

    private static final int CANVAS_WIDTH = 1200;
    private static final int CANVAS_HEIGHT = 800;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 10.0;
    private static final double ZOOM_FACTOR = 1.1;

    private Canvas canvas;
    private GraphicsContext gc;
    private List<ClosedShape> shapes;

    // View transformation properties
    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    // Mouse interaction
    private double lastMouseX;
    private double lastMouseY;
    private boolean isPanning = false;

    // UI Components
    private Label zoomLabel;
    private Label shapeCountLabel;
    private Label animationProgressLabel;
    private boolean showLabels = true;
    private boolean showCenters = true;
    private boolean fillShapes = false;
    private CheckBox showLabelsCheckbox;
    private CheckBox showCentersCheckbox;
    private CheckBox fillShapesCheckbox;

    // Selection
    private ClosedShape selectedShape = null;

    // Animation properties
    private boolean isAnimating = false;
    private int currentLineIndex = 0;
    private List<LineWithShape> allLines = new ArrayList<>();
    private AnimationTimer animationTimer;
    private long lastAnimationUpdate = 0;
    private double animationSpeed = 50; // milliseconds per line
    private Button playPauseButton;
    private Button stepForwardButton;
    private Button stepBackwardButton;
    private Button resetAnimationButton;
    private Slider speedSlider;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create canvas
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // Setup mouse interactions
        setupMouseHandlers();

        // Initialize animation timer
        initializeAnimationTimer();

        // Create UI components FIRST (before loading shapes)
        BorderPane root = new BorderPane();
        root.setCenter(canvas);

        VBox topContainer = new VBox(5);
        topContainer.getChildren().addAll(createToolbar(), createAnimationControls());
        root.setTop(topContainer);

        root.setBottom(createStatusBar());

        // NOW load and initialize shapes (after UI components are created)
        FileProcessor fileProcessor = new HPGLFileProcessor(Files.readString(Path.of("AG-1009-2.plt"), StandardCharsets.UTF_8));
        fileProcessor.startFileProcessing();
        setShapes(fileProcessor.getShapes());

        // Setup scene
        Scene scene = new Scene(root);
        primaryStage.setTitle("Shape Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: #f0f0f0;");

        Button zoomInBtn = new Button("Zoom In (+)");
        zoomInBtn.setOnAction(e -> zoomAt(CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2, ZOOM_FACTOR));

        Button zoomOutBtn = new Button("Zoom Out (-)");
        zoomOutBtn.setOnAction(e -> zoomAt(CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2, 1 / ZOOM_FACTOR));

        Button resetBtn = new Button("Reset View");
        resetBtn.setOnAction(e -> {
            autoFitShapes();
            draw();
        });

        Button fitBtn = new Button("Fit to Window");
        fitBtn.setOnAction(e -> {
            autoFitShapes();
            draw();
        });

        zoomLabel = new Label("Zoom: 100%");
        zoomLabel.setStyle("-fx-font-weight: bold;");

        showCentersCheckbox = new CheckBox("Show Centers");
        showCentersCheckbox.setSelected(true);
        showCentersCheckbox.setOnAction(e -> {
            showCenters = showCentersCheckbox.isSelected();
            draw();
        });

        showLabelsCheckbox = new CheckBox("Show Labels");
        showLabelsCheckbox.setSelected(true);
        showLabelsCheckbox.setOnAction(e -> {
            showLabels = showLabelsCheckbox.isSelected();
            draw();
        });

        fillShapesCheckbox = new CheckBox("Fill Shapes");
        fillShapesCheckbox.setSelected(false);
        fillShapesCheckbox.setOnAction(e -> {
            fillShapes = fillShapesCheckbox.isSelected();
            draw();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(
                zoomInBtn, zoomOutBtn, resetBtn, fitBtn, spacer,
                showCentersCheckbox, showLabelsCheckbox, fillShapesCheckbox, zoomLabel
        );

        return toolbar;
    }

    private HBox createAnimationControls() {
        HBox animationBar = new HBox(10);
        animationBar.setPadding(new Insets(10));
        animationBar.setStyle("-fx-background-color: #e8f4f8;");

        Label animationLabel = new Label("Animation:");
        animationLabel.setStyle("-fx-font-weight: bold;");

        playPauseButton = new Button("▶ Play");
        playPauseButton.setOnAction(e -> toggleAnimation());

        stepBackwardButton = new Button("◀ Step Back");
        stepBackwardButton.setOnAction(e -> stepBackward());

        stepForwardButton = new Button("Step Forward ▶");
        stepForwardButton.setOnAction(e -> stepForward());

        resetAnimationButton = new Button("⟲ Reset");
        resetAnimationButton.setOnAction(e -> resetAnimation());

        Label speedLabel = new Label("Speed:");
        speedSlider = new Slider(10, 200, 50);
        speedSlider.setShowTickLabels(false);
        speedSlider.setShowTickMarks(true);
        speedSlider.setPrefWidth(150);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            animationSpeed = newVal.doubleValue();
        });

        animationProgressLabel = new Label("Line: 0 / 0");
        animationProgressLabel.setStyle("-fx-font-family: monospace;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        animationBar.getChildren().addAll(
                animationLabel, playPauseButton, stepBackwardButton, stepForwardButton,
                resetAnimationButton, speedLabel, speedSlider, spacer, animationProgressLabel
        );

        return animationBar;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #e0e0e0;");

        shapeCountLabel = new Label("Shapes: 0");

        Label instructions = new Label(
                "Controls: Mouse Wheel = Zoom | Left Click + Drag = Pan | Click Shape Border = Select"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(shapeCountLabel, spacer, instructions);

        return statusBar;
    }

    private void setupMouseHandlers() {
        // Mouse wheel zoom
        canvas.setOnScroll((ScrollEvent event) -> {
            double mouseX = event.getX();
            double mouseY = event.getY();
            double zoomDelta = event.getDeltaY() > 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
            zoomAt(mouseX, mouseY, zoomDelta);
        });

        // Mouse press - check for shape selection or start panning
        canvas.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                // Check if we're clicking near any shape border
                Point2D clickPoint = new Point2D(
                        (event.getX() - offsetX) / zoom,
                        (event.getY() - offsetY) / zoom
                );

                ClosedShape clickedShape = findShapeNearClick(clickPoint, 10.0 / zoom);
                if (clickedShape != null) {
                    // Toggle selection
                    if (selectedShape == clickedShape) {
                        selectedShape = null;
                        System.out.println("Shape deselected");
                    } else {
                        selectedShape = clickedShape;
                        System.out.println("=== SHAPE SELECTED ===");
                        System.out.println("Shape ID: " + clickedShape.getId());
                        System.out.println("Label: " + clickedShape.getLabel());
                        System.out.println("Center: " + clickedShape.getCenter());
                        System.out.println("Number of lines: " + clickedShape.getLines().size());
                        System.out.println("Bounds: " + clickedShape.getBounds());
                        System.out.println("======================");
                    }
                    draw();
                    return; // Don't start panning if we clicked a shape
                }

                // Start panning
                lastMouseX = event.getX();
                lastMouseY = event.getY();
                isPanning = true;
                canvas.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            }
        });

        // Mouse drag - pan
        canvas.setOnMouseDragged(event -> {
            if (isPanning && event.getButton() == MouseButton.PRIMARY) {
                double deltaX = event.getX() - lastMouseX;
                double deltaY = event.getY() - lastMouseY;

                offsetX += deltaX;
                offsetY += deltaY;

                lastMouseX = event.getX();
                lastMouseY = event.getY();

                draw();
            }
        });

        // Mouse release - stop panning
        canvas.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                isPanning = false;
                canvas.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });

        // Mouse hover - show cursor
        canvas.setOnMouseEntered(event -> {
            if (!isPanning) {
                canvas.setCursor(javafx.scene.Cursor.HAND);
            }
        });

        canvas.setOnMouseExited(event -> {
            if (!isPanning) {
                canvas.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });
    }

    /**
     * Find a shape whose border is near the click point
     */
    private ClosedShape findShapeNearClick(Point2D clickPoint, double threshold) {
        if (shapes == null) return null;

        // Check shapes in reverse order (top to bottom in rendering)
        for (int i = shapes.size() - 1; i >= 0; i--) {
            ClosedShape shape = shapes.get(i);

            // Check if click is near any line in this shape
            for (Line line : shape.getLines()) {
                double distance = distanceToLineSegment(
                        clickPoint,
                        new Point2D(line.getStartX(), line.getStartY()),
                        new Point2D(line.getEndX(), line.getEndY())
                );

                if (distance <= threshold) {
                    return shape;
                }
            }
        }

        return null;
    }

    /**
     * Calculate the distance from a point to a line segment
     */
    private double distanceToLineSegment(Point2D point, Point2D lineStart, Point2D lineEnd) {
        double x = point.getX();
        double y = point.getY();
        double x1 = lineStart.getX();
        double y1 = lineStart.getY();
        double x2 = lineEnd.getX();
        double y2 = lineEnd.getY();

        double A = x - x1;
        double B = y - y1;
        double C = x2 - x1;
        double D = y2 - y1;

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = -1;

        if (lenSq != 0) {
            param = dot / lenSq;
        }

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        double dx = x - xx;
        double dy = y - yy;

        return Math.sqrt(dx * dx + dy * dy);
    }

    private void zoomAt(double pivotX, double pivotY, double factor) {
        double oldZoom = zoom;
        zoom *= factor;

        // Clamp zoom
        if (zoom < MIN_ZOOM) zoom = MIN_ZOOM;
        if (zoom > MAX_ZOOM) zoom = MAX_ZOOM;

        // Adjust offset to zoom toward pivot point
        double zoomChange = zoom / oldZoom;
        offsetX = pivotX - (pivotX - offsetX) * zoomChange;
        offsetY = pivotY - (pivotY - offsetY) * zoomChange;

        updateZoomLabel();
        draw();
    }

    private void updateZoomLabel() {
        if (zoomLabel != null) {
            zoomLabel.setText(String.format("Zoom: %.0f%%", zoom * 100));
        }
    }

    private void autoFitShapes() {
        if (shapes == null || shapes.isEmpty()) {
            zoom = 1.0;
            offsetX = 0;
            offsetY = 0;
            updateZoomLabel();
            return;
        }

        // Find bounding box of all shapes
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (ClosedShape shape : shapes) {
            for (Line line : shape.getLines()) {
                minX = Math.min(minX, Math.min(line.getStartX(), line.getEndX()));
                minY = Math.min(minY, Math.min(line.getStartY(), line.getEndY()));
                maxX = Math.max(maxX, Math.max(line.getStartX(), line.getEndX()));
                maxY = Math.max(maxY, Math.max(line.getStartY(), line.getEndY()));
            }
        }

        double shapeWidth = maxX - minX;
        double shapeHeight = maxY - minY;

        if (shapeWidth == 0 || shapeHeight == 0) {
            zoom = 1.0;
            offsetX = 0;
            offsetY = 0;
            updateZoomLabel();
            return;
        }

        // Calculate zoom to fit with padding
        double padding = 50;
        double zoomX = (CANVAS_WIDTH - 2 * padding) / shapeWidth;
        double zoomY = (CANVAS_HEIGHT - 2 * padding) / shapeHeight;
        zoom = Math.min(zoomX, zoomY);

        // Clamp zoom
        if (zoom < MIN_ZOOM) zoom = MIN_ZOOM;
        if (zoom > MAX_ZOOM) zoom = MAX_ZOOM;

        // Center the shapes
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;

        offsetX = CANVAS_WIDTH / 2 - centerX * zoom;
        offsetY = CANVAS_HEIGHT / 2 - centerY * zoom;

        updateZoomLabel();
    }

    // ==================== Animation Methods ====================

    private void initializeAnimationTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isAnimating) {
                    long elapsedMillis = (now - lastAnimationUpdate) / 1_000_000;
                    if (elapsedMillis >= animationSpeed) {
                        stepForward();
                        lastAnimationUpdate = now;
                    }
                }
            }
        };
        animationTimer.start();
    }

    private void prepareAnimationData() {
        allLines.clear();
        if (shapes == null) return;

        for (ClosedShape shape : shapes) {
            for (Line line : shape.getLines()) {
                allLines.add(new LineWithShape(line, shape));
            }
        }

        updateAnimationProgress();
    }

    private void toggleAnimation() {
        isAnimating = !isAnimating;
        if (isAnimating) {
            playPauseButton.setText("⏸ Pause");
            lastAnimationUpdate = System.nanoTime();
            if (currentLineIndex >= allLines.size()) {
                resetAnimation();
            }
        } else {
            playPauseButton.setText("▶ Play");
        }
    }

    private void stepForward() {
        if (currentLineIndex < allLines.size()) {
            currentLineIndex++;
            updateAnimationProgress();
            draw();

            if (currentLineIndex >= allLines.size() && isAnimating) {
                isAnimating = false;
                playPauseButton.setText("▶ Play");
            }
        }
    }

    private void stepBackward() {
        if (currentLineIndex > 0) {
            currentLineIndex--;
            updateAnimationProgress();
            draw();
        }
    }

    private void resetAnimation() {
        currentLineIndex = 0;
        isAnimating = false;
        playPauseButton.setText("▶ Play");
        updateAnimationProgress();
        draw();
    }

    private void updateAnimationProgress() {
        if (animationProgressLabel != null) {
            animationProgressLabel.setText(String.format("Line: %d / %d",
                    currentLineIndex, allLines.size()));
        }
    }

    // ==================== Drawing Methods ====================


    private void draw() {
        if (shapes == null) return;

        // Clear canvas
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Draw grid
        drawGrid();

        if (currentLineIndex == 0 || currentLineIndex >= allLines.size()) {
            // Normal mode - draw all shapes
            for (ClosedShape shape : shapes) {
                drawShape(shape);
            }

            // Draw shape centers (only if showCenters is enabled)
            if (showCenters) {
                for (ClosedShape shape : shapes) {
                    drawCenter(shape);
                }
            }
        } else {
            // Animation mode - draw only lines up to currentLineIndex
            drawAnimatedLines();
        }
    }

    private void drawAnimatedLines() {
        // Draw completed shapes in their normal color (faded)
        for (int i = 0; i < currentLineIndex; i++) {
            LineWithShape lws = allLines.get(i);
            Color color = lws.shape.getColor();
            if (color == null) {
                color = Color.rgb(100, 150, 255, 0.3);
            } else {
                color = Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.3);
            }

            gc.setStroke(color);
            gc.setLineWidth(2.0 / zoom);

            Line line = lws.line;
            double x1 = transformX(line.getStartX());
            double y1 = transformY(line.getStartY());
            double x2 = transformX(line.getEndX());
            double y2 = transformY(line.getEndY());

            gc.strokeLine(x1, y1, x2, y2);
        }

        // Draw current line being drawn with highlight
        if (currentLineIndex > 0 && currentLineIndex <= allLines.size()) {
            LineWithShape current = allLines.get(currentLineIndex - 1);

            // Highlight current line in bright color
            gc.setStroke(Color.RED);
            gc.setLineWidth(4.0 / zoom);

            Line line = current.line;
            double x1 = transformX(line.getStartX());
            double y1 = transformY(line.getStartY());
            double x2 = transformX(line.getEndX());
            double y2 = transformY(line.getEndY());

            gc.strokeLine(x1, y1, x2, y2);

            // Draw start and end points of current line
            double pointRadius = 6.0 / zoom;
            gc.setFill(Color.GREEN);
            gc.fillOval(x1 - pointRadius, y1 - pointRadius, pointRadius * 2, pointRadius * 2);

            gc.setFill(Color.BLUE);
            gc.fillOval(x2 - pointRadius, y2 - pointRadius, pointRadius * 2, pointRadius * 2);

            // Draw line number
            gc.setFill(Color.BLACK);
            gc.setFont(javafx.scene.text.Font.font(14 / zoom));
            gc.fillText(String.valueOf(currentLineIndex),
                    (x1 + x2) / 2 + 10 / zoom, (y1 + y2) / 2);
        }
    }

    private void drawGrid() {
        gc.setStroke(Color.gray(0.9));
        gc.setLineWidth(1.0 / zoom);

        double gridSize = 100;
        double scaledGridSize = gridSize * zoom;

        // Only draw grid if it's not too dense
        if (scaledGridSize > 10) {
            // Vertical lines
            double startX = offsetX % scaledGridSize;
            for (double x = startX; x < CANVAS_WIDTH; x += scaledGridSize) {
                gc.strokeLine(x, 0, x, CANVAS_HEIGHT);
            }

            // Horizontal lines
            double startY = offsetY % scaledGridSize;
            for (double y = startY; y < CANVAS_HEIGHT; y += scaledGridSize) {
                gc.strokeLine(0, y, CANVAS_WIDTH, y);
            }
        }
    }

    private void drawShape(ClosedShape shape) {
        Color color = shape.getColor();
        if (color == null) {
            color = Color.rgb(100, 150, 255, 0.5);
        }

        boolean isSelected = (shape == selectedShape);

        // If fillShapes is enabled, fill the polygon
        if (fillShapes) {
            List<Line> lines = shape.getLines();
            if (lines.size() >= 3) {
                // Build polygon points from lines
                double[] xPoints = new double[lines.size()];
                double[] yPoints = new double[lines.size()];

                for (int i = 0; i < lines.size(); i++) {
                    Line line = lines.get(i);
                    xPoints[i] = transformX(line.getStartX());
                    yPoints[i] = transformY(line.getStartY());
                }

                // Fill the polygon
                Color fillColor = isSelected
                        ? Color.rgb(255, 200, 0, 0.7)  // Brighter yellow for selected
                        : Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.3);
                gc.setFill(fillColor);
                gc.fillPolygon(xPoints, yPoints, lines.size());
            }
        }

        // Draw outline (always draw outline)
        Color strokeColor = isSelected ? Color.ORANGE : color;
        double lineWidth = isSelected ? 4.0 / zoom : 2.0 / zoom;

        gc.setStroke(strokeColor);
        gc.setLineWidth(lineWidth);

        List<Line> lines = shape.getLines();
        for (Line line : lines) {
            double x1 = transformX(line.getStartX());
            double y1 = transformY(line.getStartY());
            double x2 = transformX(line.getEndX());
            double y2 = transformY(line.getEndY());

            gc.strokeLine(x1, y1, x2, y2);
        }
    }

    private void drawCenter(ClosedShape shape) {
        Point2D center = shape.getCenter();
        if (center == null) return;

        double cx = transformX(center.getX());
        double cy = transformY(center.getY());
        double radius = 5.0 / zoom;

        // Draw center point
        gc.setFill(Color.RED);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

        // Draw crosshair
        gc.setStroke(Color.RED);
        gc.setLineWidth(1.5 / zoom);
        double crossSize = 10.0 / zoom;
        gc.strokeLine(cx - crossSize, cy, cx + crossSize, cy);
        gc.strokeLine(cx, cy - crossSize, cx, cy + crossSize);

        // Draw label if exists and showLabels is enabled
        if (showLabels && shape.getLabel() != null) {
            gc.setFill(Color.BLACK);
            gc.fillText(shape.getLabel().toString(), cx + 10 / zoom, cy - 10 / zoom);
        }
    }

    private double transformX(double x) {
        return x * zoom + offsetX;
    }

    private double transformY(double y) {
        return y * zoom + offsetY;
    }

    // ==================== Public API ====================

    /**
     * Set the shapes to visualize
     */
    public void setShapes(List<ClosedShape> shapes) {
        this.shapes = shapes != null ? shapes : new ArrayList<>();
        if (shapeCountLabel != null) {
            shapeCountLabel.setText("Shapes: " + this.shapes.size());
        }
        prepareAnimationData();
        autoFitShapes();
        draw();
    }

    /**
     * Get current shapes
     */
    public List<ClosedShape> getShapes() {
        return shapes;
    }

    public static void main(String[] args) {
        launch(args);
    }

    // ==================== Helper Classes ====================

    /**
     * Helper class to track which shape a line belongs to during animation
     */
    private static class LineWithShape {
        final Line line;
        final ClosedShape shape;

        LineWithShape(Line line, ClosedShape shape) {
            this.line = line;
            this.shape = shape;
        }
    }
}