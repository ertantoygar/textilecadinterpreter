package tr.com.logidex.cad.processor;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.*;
import tr.com.logidex.cad.helper.LabelGroupingManager;
import tr.com.logidex.cad.model.ClosedShape;
import tr.com.logidex.cad.model.Lbl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract base class for processing CAD files of different formats.
 * Handles common operations like shape creation, label organization, and transformations.
 */
public sealed abstract class FileProcessor permits GerberFileProcessor,GGTFileProcessor,HPGLFileProcessor {

    // Constants
    public static final String REFERENCE_SIGN = "+";
    private static final double DRAWING_SPLIT_WIDTH = 50;
    private static final double PLOTTER_SCALE = 40;

    // Static state
    public static Unit unit;

    // Collections
    private List<Lbl> sortedAndOptimizedLbls = new ArrayList<>();
    private List<ClosedShape> shapes = new ArrayList<>();
    protected List<Lbl> labels = new ArrayList<>();
    private List<Lbl> sortedLbls = new ArrayList<>();
    List<Line> lines = new ArrayList<>();
    protected List<String> commands;
    protected HashMap<Integer, List<Line>> linesForClosedShapes = new HashMap<>();

    // Configuration
    protected List<String> UNWANTED_CHARS;
    protected String SPLIT_REGEX;

    // State
    public Dimension2D drawingDimensions = new Dimension2D(0, 0);
    private String fileContent;
    private final LabelGroupingManager labelGroupingManager = new LabelGroupingManager();
    private final List<GGTPattern> GGTParcalar = new ArrayList<>();
    private FlipHorizontally flipHorizontally = FlipHorizontally.NO;
    private FlipVertically flipVertically = FlipVertically.NO;
    private boolean err = false;

    public FileProcessor(String fileContent) {
        this.fileContent = fileContent;
        initUnwantedChars();
        initSplitRegex();
    }



    // ==================== Abstract Methods ====================

    protected abstract void initUnwantedChars();

    protected abstract void initSplitRegex();

    protected abstract void interpretCommands();

    // ==================== Public API ====================

    public void startFileProcessing() throws Exception {
        try {
            splitCommands();
            removeUnwantedCharacters();
            interpretCommands();
            determineDrawingDimension();
            groupSortLabelsAndOptimizeRoutes(FlipHorizontally.NO, FlipVertically.NO);
            createPieces();
            mergeLabelsIfPatternHasTwoLabels();
            checkOverlapError();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error occurred during file processing \n" + e.getMessage());
        }
    }

    public void invertFlipH() {
        flipHorizontally = flipHorizontally == FlipHorizontally.YES ? FlipHorizontally.NO : FlipHorizontally.YES;

        if (this instanceof GGTFileProcessor) {
            flipShapes(Flipping.HORIZONTAL);
        } else {
            groupSortLabelsAndOptimizeRoutes(flipHorizontally, flipVertically);
            flipShapes(Flipping.HORIZONTAL);
            mergeLabelsWithSameShape();
        }
    }

    public void invertFlipV() {
        flipVertically = flipVertically == FlipVertically.YES ? FlipVertically.NO : FlipVertically.YES;

        if (this instanceof GGTFileProcessor) {
            flipShapes(Flipping.VERTICAL);
        } else {
            groupSortLabelsAndOptimizeRoutes(flipHorizontally, flipVertically);
            flipShapes(Flipping.VERTICAL);
            mergeLabelsWithSameShape();
        }
    }

    public void clearAll() {
        commands = null;
        linesForClosedShapes = null;
        sortedAndOptimizedLbls = null;
        shapes = null;
        labels = null;
        sortedLbls = null;
        lines = null;
        fileContent = null;
        labelGroupingManager.clear();
    }

    // ==================== Getters ====================

    public List<ClosedShape> getShapes() {
        return shapes;
    }

    public LabelGroupingManager getLabelGroupingManager() {
        return labelGroupingManager;
    }


    public List<Lbl> getSortedAndOptimizedLbls() {
        return sortedAndOptimizedLbls;
    }

    public List<Line> getLines() {
        return lines;
    }

    public Dimension2D getDrawingDimensions() {
        return drawingDimensions;
    }

    public List<GGTPattern> getGGTParcalar() {
        return GGTParcalar;
    }

    public String getPrintableDimensions() {
        String formattedW = String.format("%.2f", drawingDimensions.getWidth());
        String formattedH = String.format("%.2f", drawingDimensions.getHeight());
        return "L: " + formattedW + ", W: " + formattedH;
    }

    public FlipHorizontally getFlipHorizontally() {
        return flipHorizontally;
    }

    public FlipVertically getFlipVertically() {
        return flipVertically;
    }

    // ==================== Processing Methods ====================

    protected void splitCommands() {
        commands = new ArrayList<>(Arrays.asList(fileContent.split(SPLIT_REGEX)));
        fileContent = null;
    }

    protected void removeUnwantedCharacters() {
        commands.removeAll(UNWANTED_CHARS);
    }

    protected void determineDrawingDimension() {
        drawingDimensions = calculateDrawingDimensions(lines);
    }

    protected Dimension2D calculateDrawingDimensions(List<Line> lines) {
        double lengthOfTheDrawing = 0;
        double widthOfTheDrawing = 0;

        for (Line line : lines) {
            if (line.getEndX() > lengthOfTheDrawing) {
                lengthOfTheDrawing = line.getEndX();
            }
            if (line.getEndY() > widthOfTheDrawing) {
                widthOfTheDrawing = line.getEndY();
            }
        }

        return new Dimension2D(lengthOfTheDrawing, widthOfTheDrawing);
    }

    protected double getMinPosInX() {
        double xMin = Double.MAX_VALUE;
        for (Line line : lines) {
            if (line.getStartX() < xMin) {
                xMin = line.getStartX();
            }
        }
        return xMin;
    }

    protected double scale(double number) {
        return number / PLOTTER_SCALE * 1.016;
    }

    // ==================== Label Management ====================

    public void groupSortLabelsAndOptimizeRoutes(FlipHorizontally flipH, FlipVertically flipV) {
        if (!labels.isEmpty()) {
            double minPosX = getMinPosInX();
            double width = drawingDimensions.getWidth();
            double height = drawingDimensions.getHeight();
            sortedLbls = labelGroupingManager.groupAndSortLabels(labels, minPosX, width, height, flipH, flipV);
            sortedAndOptimizedLbls = organizeLabels(sortedLbls, drawingDimensions.getWidth(), DRAWING_SPLIT_WIDTH);
            addReferenceLabelToTheFinalList();
        }
    }

    private void addReferenceLabelToTheFinalList() {
        Lbl referenceLabel = new Lbl(REFERENCE_SIGN, new Point2D(0, 0), 0, 0, 0, 0);
        sortedAndOptimizedLbls.add(0, referenceLabel);
    }

    /**
     * Organizes labels in an optimal processing order using a snake pattern.
     *
     * @param labels List of labels to organize
     * @param drawingWidth Width of the drawing area
     * @param stripWidth Width of each vertical strip for processing
     * @return List of labels in optimal processing order
     */
    public static List<Lbl> organizeLabels(List<Lbl> labels, double drawingWidth, double stripWidth) {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyList();
        }

        int stripCount = (int) Math.ceil(drawingWidth / stripWidth);
        List<List<Lbl>> strips = createStrips(stripCount);

        assignLabelsToStrips(labels, strips, stripWidth, stripCount);
        sortStripsInSnakePattern(strips);

        return combineStrips(strips, labels.size());
    }

    private static List<List<Lbl>> createStrips(int stripCount) {
        List<List<Lbl>> strips = new ArrayList<>(stripCount);
        for (int i = 0; i < stripCount; i++) {
            strips.add(new ArrayList<>());
        }
        return strips;
    }

    private static void assignLabelsToStrips(List<Lbl> labels, List<List<Lbl>> strips,
                                             double stripWidth, int stripCount) {
        for (Lbl label : labels) {
            if (label == null) {
                continue;
            }

            double x = label.getPosition().getX();
            int stripIndex = (int) (x / stripWidth);

            if (stripIndex == stripCount) {
                stripIndex = stripCount - 1;
            }

            if (stripIndex >= 0 && stripIndex < stripCount) {
                strips.get(stripIndex).add(label);
            }
        }
    }

    private static void sortStripsInSnakePattern(List<List<Lbl>> strips) {
        for (int i = 0; i < strips.size(); i++) {
            Collections.sort(strips.get(i), Comparator.comparingDouble(l -> l.getPosition().getY()));

            if (i % 2 == 1) {
                Collections.reverse(strips.get(i));
            }
        }
    }

    private static List<Lbl> combineStrips(List<List<Lbl>> strips, int estimatedSize) {
        List<Lbl> result = new ArrayList<>(estimatedSize);
        for (List<Lbl> strip : strips) {
            result.addAll(strip);
        }
        return result;
    }

    /**
     * Finds labels bound to the same shape and merges them.
     */
    public void mergeLabelsWithSameShape() {
        Map<ClosedShape, List<Lbl>> duplicatedShapes = sortedAndOptimizedLbls.stream()
                .filter(lbl -> lbl.getShape() != null)
                .collect(Collectors.groupingBy(Lbl::getShape));

        List<Lbl> toRemove = new ArrayList<>();

        duplicatedShapes.forEach((shape, lblList) -> {
            if (lblList.size() > 1) {
                Lbl longest = lblList.stream()
                        .max(Comparator.comparingInt(lbl -> lbl.getText().length()))
                        .orElse(lblList.get(0));

                lblList.stream()
                        .filter(lbl -> lbl != longest)
                        .forEach(shortLbl -> {
                            String currentText = longest.getText();
                            String textToAdd = shortLbl.getText();
                            longest.setText(currentText + "\n" + textToAdd);
                            toRemove.add(shortLbl);
                        });
            }
        });

        sortedAndOptimizedLbls.removeAll(toRemove);
    }

    public void mergeLabelsIfPatternHasTwoLabels() {
        System.out.println("mergeLabelsIfPatternHasTwoLabels");

        List<Lbl> notHaveAShape = sortedAndOptimizedLbls.stream()
                .filter(lbl -> lbl.getShape() == null && !lbl.getText().equals(REFERENCE_SIGN))
                .collect(Collectors.toList());

        Map<Lbl, ClosedShape> map = findShapesForLabels(notHaveAShape);

        sortedAndOptimizedLbls.removeAll(notHaveAShape);
        mergeLabelsIntoShapes(map);
    }

    private Map<Lbl, ClosedShape> findShapesForLabels(List<Lbl> labels) {
        Map<Lbl, ClosedShape> map = new HashMap<>();

        labels.forEach(lbl -> {
            for (ClosedShape s : shapes) {
                if (ClosedShape.pointInPolygon(s.getLines(), lbl.getPosition())) {
                    map.put(lbl, s);
                    break;
                }
            }
        });

        return map;
    }

    private void mergeLabelsIntoShapes(Map<Lbl, ClosedShape> map) {
        map.entrySet().forEach(entry -> {
            Lbl existingLabel = entry.getValue().getLabel();

            if (existingLabel != null) {
                String labelText = existingLabel.getText();
                String textToAdd = entry.getKey().getText();
                existingLabel.setText(labelText + "\n" + textToAdd);
            }
        });
    }

    // ==================== Shape Management ====================

    protected void createPieces() {
        if (this instanceof GGTFileProcessor) {
            initializeLabelsForGGT();
        }

        for (Map.Entry<Integer, List<Line>> entry : linesForClosedShapes.entrySet()) {
            ClosedShape cs = new ClosedShape(entry.getValue(), (this instanceof GGTFileProcessor));

            if (cs.isValidPiece()) {
                cs.relocateOriginX();
                cs.setId(entry.getKey());

                if (this instanceof GGTFileProcessor) {
                    processGGTShape(cs);
                } else {
                    processStandardShape(cs);
                }

                addShapeIfUnique(cs);
            }
        }
    }

    private void initializeLabelsForGGT() {
        for (int i = 0; i < linesForClosedShapes.size() + 1; i++) {
            labels.add(null);
        }
    }

    private void processGGTShape(ClosedShape cs) {
        for (GGTPattern p : getGGTParcalar()) {
            if (cs.getId().equals(p.getId())) {
                Lbl label = p.getLabel();

                if (label != null) {
                    label.changeLabelPosition(cs.getCenter());
                    cs.setLabel(label);
                    label.setShape(cs);
                    labels.set(cs.getId(), cs.getLabel());
                    labels.set(0, new Lbl(REFERENCE_SIGN, new Point2D(0, 0), 0, 0, 0, 0));
                } else {
                    continue;
                }
            }
        }

        sortedAndOptimizedLbls = organizeLabels(labels, drawingDimensions.getWidth(), DRAWING_SPLIT_WIDTH);
    }

    private void processStandardShape(ClosedShape cs) {
        for (Lbl lbl : sortedAndOptimizedLbls) {
            if (lbl == null) {
                continue;
            }

            if (cs.pointInPolygon(cs.getLines(), lbl.getPosition()) &&
                    cs.isCalculatedCenterPointIsInThisShape()) {
                if (cs.getLabel() == null) {
                    cs.setLabel(lbl);
                    lbl.setShape(cs);
                }
            }
        }
    }

    private void addShapeIfUnique(ClosedShape cs) {
        boolean alreadyExists = false;

        for (ClosedShape s : shapes) {
            if (s.getLines().equals(cs.getLines())) {
                alreadyExists = true;
                break;
            }
        }

        if (!alreadyExists) {
            shapes.add(cs);
        }
    }

    public void flipShapes(Flipping flipping) {
        for (ClosedShape cs : shapes) {
            cs.restoreColor();

            if (flipping == Flipping.HORIZONTAL) {
                cs.mirrorX(drawingDimensions.getWidth());
            }
            if (flipping == Flipping.VERTICAL) {
                cs.mirrorY(drawingDimensions.getHeight());
            }

            if (this instanceof GGTFileProcessor) {
                updateGGTShapeLabel(cs);
            } else {
                reassignLabelToShape(cs);
            }
        }

        if (this instanceof GGTFileProcessor) {
            sortedAndOptimizedLbls = organizeLabels(labels, drawingDimensions.getWidth(), DRAWING_SPLIT_WIDTH);
        }
    }

    private void updateGGTShapeLabel(ClosedShape cs) {
        Lbl currentLabel = cs.getLabel();
        if (currentLabel != null) {
            currentLabel.changeLabelPosition(cs.getCenter());
            cs.setLabel(currentLabel);
            currentLabel.setShape(cs);
        }
    }

    private void reassignLabelToShape(ClosedShape cs) {
        for (Lbl lbl : sortedAndOptimizedLbls) {
            if (ClosedShape.pointInPolygon(cs.getLines(), lbl.getPosition())) {
                cs.setLabel(lbl);
                lbl.setShape(cs);
            }
        }
    }

    // ==================== Validation ====================

    private void checkOverlapError() {
        shapes.forEach((sh) -> {
            for (int i = 0; i < shapes.size(); i++) {
                if (!sh.equals(shapes.get(i))) {
                    if (ClosedShape.pointInPolygon(sh.getLines(), shapes.get(i).getCenter())) {
                        err = true;
                    }
                }
            }
        });

        if (err) {
            showOverlapWarning();
        }
    }

    private void showOverlapWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("WARNING!");
        alert.setContentText("Ic ice gecmis parcalar var! Bir parcanin hesaplanan merkezi, baska bir parcanin da alani icerisinde kaliyor."
                + "\n\n"
                + "There are overlapped patterns! The calculated centroid of a pattern is falling under another pattern.");
        alert.showAndWait();
    }
}