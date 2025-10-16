package tr.com.logidex.cad.processor;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.Unit;

import tr.com.logidex.cad.model.GGTPattern;
import tr.com.logidex.cad.model.Label;
import tr.com.logidex.cad.model.Pattern;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract base class for processing CAD files.
 *
 * Handles common operations like parsing, label organization,
 * and pattern creation across different file formats (HPGL, GGT, Gerber).
 */
public abstract class FileProcessor {



    public static final String REFERENCE_SIGN = "+";
    private static final double DRAWING_SPLIT_WIDTH = 50.0;
    private static final double PLOTTER_SCALE = 40.0;
    private static final double SCALE_ADJUSTMENT = 1.016;



    public static Unit unit;



    protected List<Label> labels = new ArrayList<>();
    protected List<Line> lines = new ArrayList<>();
    protected List<String> commands;
    protected List<String> UNWANTED_CHARS;
    protected String SPLIT_REGEX;
    protected HashMap<Integer, List<Line>> linesForClosedShapes = new HashMap<>();



    private String fileContent;
    private final LabelGroupingManager labelGroupingManager = new LabelGroupingManager();

    private List<Label> sortedLabels = new ArrayList<>();
    private List<Label> sortedAndOptimizedLabels = new ArrayList<>();
    private List<Pattern> patterns = new ArrayList<>();
    private List<GGTPattern> ggtPatterns = new ArrayList<>();
    private Dimension2D drawingDimensions = new Dimension2D(0, 0);
    private boolean hasOverlapError = false;



    protected FileProcessor(String fileContent) {
        if (fileContent == null || fileContent.trim().isEmpty()) {
            throw new IllegalArgumentException("File content cannot be null or empty");
        }
        this.fileContent = fileContent;
        initUnwantedChars();
        initSplitRegex();
    }



    protected abstract void initUnwantedChars();
    protected abstract void initSplitRegex();
    protected abstract void interpretCommands() throws FileProcessingException;


    /**
     * Executes the complete file processing pipeline.
     *
     * Steps:
     * 1. Split commands
     * 2. Remove unwanted characters
     * 3. Interpret commands (format-specific)
     * 4. Determine drawing dimensions
     * 5. Group and sort labels
     * 6. Create pattern pieces
     * 7. Merge duplicate labels
     * 8. Check for overlaps
     *
     * @throws FileProcessingException if processing fails
     */
    public void startFileProcessing() throws FileProcessingException {
        try {
            splitCommands();
            removeUnwantedCharacters();
            interpretCommands();
            determineDrawingDimensions();
            groupSortAndOptimizeLabels(FlipDirection.NONE);
            createPatterns();
            mergeLabelsIfPatternHasMultipleLabels();
            checkForOverlappingPatterns();
        } catch (FileProcessingException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new FileProcessingException(
                    "Error occurred during file processing: " + e.getMessage()
            );
        } finally {
            // Clear file content to free memory
            fileContent = null;
        }
    }

    /**
     * Flips all patterns in the specified direction.
     *
     * @param direction the flip direction
     */
    public void flipPatterns(FlipDirection direction) {
        if (direction == FlipDirection.NONE) {
            return;
        }

        for (Pattern pattern : patterns) {
            pattern.restoreColor();

            // Apply flip transformation
            if (direction == FlipDirection.HORIZONTAL) {
                pattern.mirrorX(drawingDimensions.getWidth());
            }
            if (direction == FlipDirection.VERTICAL) {
                pattern.mirrorY(drawingDimensions.getHeight());
            }

            // Re-associate labels
            reassociateLabelsAfterFlip(pattern);
        }

        // Reorganize labels if GGT format
        if (this instanceof GGTFileProcessor) {
            sortedAndOptimizedLabels = organizeLabels(
                    labels,
                    drawingDimensions.getWidth(),
                    DRAWING_SPLIT_WIDTH
            );
        }
    }

    /**
     * Clears all data to free memory.
     */
    public void clearAll() {
        commands = null;
        linesForClosedShapes = null;
        sortedAndOptimizedLabels = null;
        patterns = null;
        labels = null;
        sortedLabels = null;
        lines = null;
        fileContent = null;
        labelGroupingManager.clear();
    }

    // ============================================
    // PUBLIC API - Getters
    // ============================================

    public List<Pattern> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }

    public List<Label> getSortedAndOptimizedLabels() {
        return Collections.unmodifiableList(sortedAndOptimizedLabels);
    }

    public Dimension2D getDrawingDimensions() {
        return drawingDimensions;
    }


    public List<GGTPattern> getGGTPatterns() {
        return ggtPatterns;
    }

    public String getPrintableDimensions() {
        return String.format("L: %.2f, W: %.2f",
                drawingDimensions.getWidth(),
                drawingDimensions.getHeight()
        );
    }

    public boolean hasOverlapError() {
        return hasOverlapError;
    }

    public LabelGroupingManager getLabelGroupingManager() {
        return labelGroupingManager;
    }

    // ============================================
    // PROTECTED METHODS - For subclasses
    // ============================================

    /**
     * Scales a number according to plotter scaling rules.
     *
     * @param number the number to scale
     * @return the scaled number
     */
    protected double scale(double number) {
        return (number / PLOTTER_SCALE) * SCALE_ADJUSTMENT;
    }

    /**
     * Gets the minimum X position from all lines.
     *
     * @return the minimum X coordinate
     */
    protected double getMinPosInX() {
        return lines.stream()
                .mapToDouble(Line::getStartX)
                .min()
                .orElse(0.0);
    }

    /**
     * Determines if this processor handles GGT files.
     *
     * @return true if this is a GGT processor
     */
    protected boolean isGGTProcessor() {
        return this instanceof GGTFileProcessor;
    }

    // ============================================
    // PRIVATE METHODS - Processing Steps
    // ============================================

    /**
     * Step 1: Split file content into commands.
     */
    private void splitCommands() {
        commands = new ArrayList<>(Arrays.asList(fileContent.split(SPLIT_REGEX)));
    }

    /**
     * Step 2: Remove unwanted characters from commands.
     */
    private void removeUnwantedCharacters() {
        commands.removeAll(UNWANTED_CHARS);
    }

    /**
     * Step 3: Determine drawing dimensions from lines.
     */
    private void determineDrawingDimensions() {
        drawingDimensions = calculateDrawingDimensions(lines);
    }

    /**
     * Calculates the bounding dimensions of the drawing.
     *
     * @param lines all lines in the drawing
     * @return the dimensions (width, height)
     */
    private Dimension2D calculateDrawingDimensions(List<Line> lines) {
        double maxX = 0;
        double maxY = 0;

        for (Line line : lines) {
            maxX = Math.max(maxX, Math.max(line.getStartX(), line.getEndX()));
            maxY = Math.max(maxY, Math.max(line.getStartY(), line.getEndY()));
        }

        return new Dimension2D(maxX, maxY);
    }

    /**
     * Step 4: Group, sort, and optimize label processing order.
     */
    private void groupSortAndOptimizeLabels(tr.com.logidex.cad.processor.FlipDirection flipDirection) {
        if (labels.isEmpty()) {
            return;
        }

        double minPosX = getMinPosInX();
        double width = drawingDimensions.getWidth();
        double height = drawingDimensions.getHeight();

        sortedLabels = labelGroupingManager.groupAndSortLabels(
                labels, minPosX, width, height, flipDirection
        );

        sortedAndOptimizedLabels = organizeLabels(
                sortedLabels,
                width,
                DRAWING_SPLIT_WIDTH
        );

        addReferenceLabel();
    }

    /**
     * Adds a reference label at the origin.
     */
    private void addReferenceLabel() {
        Label referenceLabel = new Label(
                REFERENCE_SIGN,
                new Point2D(0, 0),
                0, 0, 0, 0
        );
        sortedAndOptimizedLabels.add(0, referenceLabel);
    }

    /**
     * Step 5: Create pattern pieces from lines.
     */
    private void createPatterns() {
        // Pre-populate labels list for GGT format
        if (isGGTProcessor()) {
            int requiredSize = linesForClosedShapes.size() + 1;
            while (labels.size() < requiredSize) {
                labels.add(null);
            }
        }

        // Create patterns from line groups
        for (Map.Entry<Integer, List<Line>> entry : linesForClosedShapes.entrySet()) {
            Pattern pattern = new Pattern(entry.getValue(), isGGTProcessor());

            if (pattern.isValid()) {
                pattern.relocateCenterX();
                pattern.setId(entry.getKey());

                if (isGGTProcessor()) {
                    processGGTPattern(pattern);
                } else {
                    associatePatternWithLabels(pattern);
                }

                addPatternIfUnique(pattern);
            }
        }
    }

    /**
     * Processes a GGT format pattern.
     */
    private void processGGTPattern(Pattern pattern) {
        for (GGTPattern ggtPattern : ggtPatterns) {
            if (pattern.getId().equals(ggtPattern.getId())) {
                Label label = ggtPattern.getLabel();

                if (label != null) {
                    label.changeLabelPosition(pattern.getCenter());
                    pattern.setLabel(label);
                    label.setShape(pattern);

                    labels.set(pattern.getId(), label);
                    labels.set(0, new Label(REFERENCE_SIGN, new Point2D(0, 0), 0, 0, 0, 0));
                }
                break;
            }
        }

        sortedAndOptimizedLabels = organizeLabels(
                labels,
                drawingDimensions.getWidth(),
                DRAWING_SPLIT_WIDTH
        );
    }

    /**
     * Associates a pattern with labels that fall inside it.
     */
    private void associatePatternWithLabels(Pattern pattern) {
        for (Label label : sortedAndOptimizedLabels) {
            if (label == null) continue;

            if (Pattern.containsPoint(pattern.getLines(), label.getPosition())
                    && pattern.isCenterInside()) {
                if (pattern.getLabel() == null) {
                    pattern.setLabel(label);
                    label.setShape(pattern);
                    break; // Each pattern gets only one label initially
                }
            }
        }
    }

    /**
     * Adds pattern to list if it doesn't already exist.
     */
    private void addPatternIfUnique(Pattern pattern) {
        boolean alreadyExists = patterns.stream()
                .anyMatch(p -> p.getLines().equals(pattern.getLines()));

        if (!alreadyExists) {
            patterns.add(pattern);
        }
    }

    /**
     * Step 6: Merge labels that belong to the same pattern.
     */
    private void mergeLabelsIfPatternHasMultipleLabels() {
        // Find labels without patterns
        List<Label> orphanedLabels = sortedAndOptimizedLabels.stream()
                .filter(label -> label.getShape() == null
                        && !REFERENCE_SIGN.equals(label.getText()))
                .collect(Collectors.toList());

        // Map orphaned labels to patterns
        Map<Label, Pattern> labelToPattern = new HashMap<>();
        for (Label label : orphanedLabels) {
            findPatternForLabel(label).ifPresent(pattern ->
                    labelToPattern.put(label, pattern)
            );
        }

        // Remove orphaned labels from main list
        sortedAndOptimizedLabels.removeAll(orphanedLabels);

        // Merge text into existing pattern labels
        labelToPattern.forEach((orphanedLabel, pattern) -> {
            Label existingLabel = pattern.getLabel();
            if (existingLabel != null) {
                String mergedText = existingLabel.getText() + "\n" + orphanedLabel.getText();
                existingLabel.setText(mergedText);
            }
        });
    }

    /**
     * Finds the pattern that contains a given label.
     */
    private Optional<Pattern> findPatternForLabel(Label label) {
        return patterns.stream()
                .filter(pattern -> Pattern.containsPoint(pattern.getLines(), label.getPosition()))
                .findFirst();
    }

    /**
     * Merges labels that are associated with the same pattern.
     * (Alternative approach - keeps all labels but merges their text)
     */
    @SuppressWarnings("unused")
    private void mergeLabelsWithSamePattern() {
        Map<Pattern, List<Label>> patternToLabels = sortedAndOptimizedLabels.stream()
                .filter(label -> label.getShape() != null)
                .collect(Collectors.groupingBy(Label::getShape));

        List<Label> toRemove = new ArrayList<>();

        patternToLabels.forEach((pattern, labelList) -> {
            if (labelList.size() > 1) {
                // Keep the label with longest text
                Label primary = labelList.stream()
                        .max(Comparator.comparingInt(l -> l.getText().length()))
                        .orElse(labelList.get(0));

                // Merge other labels into primary
                labelList.stream()
                        .filter(label -> label != primary)
                        .forEach(label -> {
                            primary.setText(primary.getText() + "\n" + label.getText());
                            toRemove.add(label);
                        });
            }
        });

        sortedAndOptimizedLabels.removeAll(toRemove);
    }

    /**
     * Step 7: Check for overlapping patterns and show warning.
     */
    private void checkForOverlappingPatterns() {
        hasOverlapError = false;

        for (int i = 0; i < patterns.size(); i++) {
            Pattern pattern1 = patterns.get(i);
            for (int j = i + 1; j < patterns.size(); j++) {
                Pattern pattern2 = patterns.get(j);

                if (Pattern.containsPoint(pattern1.getLines(), pattern2.getCenter())) {
                    hasOverlapError = true;
                    break;
                }
            }
            if (hasOverlapError) break;
        }

        if (hasOverlapError) {
            showOverlapWarning();
        }
    }

    /**
     * Shows a warning dialog for overlapping patterns.
     */
    private void showOverlapWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("WARNING!");
        alert.setContentText(
                "There are overlapping patterns! " +
                        "The calculated centroid of one pattern falls inside another pattern.\n\n" +
                        "İç içe geçmiş parçalar var! " +
                        "Bir parçanın hesaplanan merkezi, başka bir parçanın da alanı içerisinde kalıyor."
        );
        alert.showAndWait();
    }

    /**
     * Re-associates labels with pattern after flipping.
     */
    private void reassociateLabelsAfterFlip(Pattern pattern) {
        if (isGGTProcessor()) {
            // GGT: Keep existing label and update position
            Label currentLabel = pattern.getLabel();
            if (currentLabel != null) {
                currentLabel.changeLabelPosition(pattern.getCenter());
                pattern.setLabel(currentLabel);
                currentLabel.setShape(pattern);
            }
        } else {
            // Other formats: Re-associate based on position
            for (Label label : sortedAndOptimizedLabels) {
                if (Pattern.containsPoint(pattern.getLines(), label.getPosition())) {
                    pattern.setLabel(label);
                    label.setShape(pattern);
                    break;
                }
            }
        }
    }

    // ============================================
    // STATIC UTILITY - Label Organization
    // ============================================

    /**
     * Organizes labels in an optimal processing order using a snake pattern.
     *
     * Labels are divided into vertical strips, sorted by Y within each strip,
     * and odd strips are reversed to create a snake/boustrophedon pattern
     * that minimizes processing head movement.
     *
     * @param labels the labels to organize
     * @param drawingWidth the width of the drawing
     * @param stripWidth the width of each strip
     * @return organized labels in processing order
     */
    public static List<Label> organizeLabels(
            List<Label> labels,
            double drawingWidth,
            double stripWidth
    ) {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyList();
        }

        // Calculate number of strips
        int stripCount = (int) Math.ceil(drawingWidth / stripWidth);

        // Create strips
        List<List<Label>> strips = createStrips(stripCount);

        // Assign labels to strips
        assignLabelsToStrips(labels, strips, stripWidth, stripCount);

        // Sort and create snake pattern
        sortStripsInSnakePattern(strips);

        // Flatten to single list
        return flattenStrips(strips);
    }

    private static List<List<Label>> createStrips(int count) {
        List<List<Label>> strips = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            strips.add(new ArrayList<>());
        }
        return strips;
    }

    private static void assignLabelsToStrips(
            List<Label> labels,
            List<List<Label>> strips,
            double stripWidth,
            int stripCount
    ) {
        for (Label label : labels) {
            if (label == null) continue;

            int stripIndex = (int) (label.getPosition().getX() / stripWidth);

            // Handle boundary cases
            stripIndex = Math.min(stripIndex, stripCount - 1);

            if (stripIndex >= 0) {
                strips.get(stripIndex).add(label);
            }
        }
    }

    private static void sortStripsInSnakePattern(List<List<Label>> strips) {
        for (int i = 0; i < strips.size(); i++) {
            // Sort by Y position
            strips.get(i).sort(
                    Comparator.comparingDouble(l -> l.getPosition().getY())
            );

            // Reverse odd strips for snake pattern
            if (i % 2 == 1) {
                Collections.reverse(strips.get(i));
            }
        }
    }

    private static List<Label> flattenStrips(List<List<Label>> strips) {
        return strips.stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
}