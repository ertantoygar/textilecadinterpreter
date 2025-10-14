package tr.com.logidex.cad.processor;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.Unit;
import tr.com.logidex.cad.model.CenterRelocator;
import tr.com.logidex.cad.model.Label;
import tr.com.logidex.cad.model.Pattern;

import java.util.*;
import java.util.stream.Collectors;

public abstract class FileProcessor {
    public static final String REFERENCE_SIGN = "+";
    private static final double DRAWING_SPLIT_WIDTH = 50;
    public static Unit unit;
    public List<Label> sortedAndOptimizedLabels = new ArrayList<Label>();


    public Dimension2D drawingDimensions = new Dimension2D(0, 0);
    public List<Pattern> shapes = new ArrayList<Pattern>();
    public List<Label> labels = new ArrayList<Label>();
    public List<Label> sortedLabels = new ArrayList<Label>();
    public List<Line> lines = new ArrayList<Line>();
    protected List<String> UNWANTED_CHARS;
    protected String SPLIT_REGEX;
    protected List<String> commands;

    protected HashMap<Integer, List<Line>> linesForClosedShapes = new HashMap<Integer, List<Line>>();
    boolean err = false;
    private String fileContent;
    private LabelGroupingManager labelGroupingManager = new LabelGroupingManager();
    private List<GGTPattern> GGTPatterns = new ArrayList<>();
    private static final double PLOTTER_SCALE = 40;


    public FileProcessor(String fileContent) {
        this.fileContent = fileContent;
        initUnwantedChars();
        initSplitRegex();

    }


    public LabelGroupingManager getLabelGroupingManager() {
        return labelGroupingManager;
    }


    protected abstract void initUnwantedChars();

    protected abstract void initSplitRegex();


     public void startFileProcessing() throws FileProcessingException {
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
            throw new FileProcessingException("Error occurred during file processing \n" + e.getMessage());
        }
    }


    protected double getMinPosInX() {
        double xMin = Double.MAX_VALUE;
        for (Line line : lines) {
            if (line.getStartX() < xMin) { // check the xmax
                xMin = line.getStartX();
            }
        }
        return xMin;
    }


    private void checkOverlapError() {
        shapes.forEach((sh) -> {
            for (int i = 0; i < shapes.size(); i++) {
                if (!sh.equals(shapes.get(i))) {
                    if (Pattern.containsPoint(sh.getLines(), shapes.get(i).getCenter())) {
                        err = true;
                    }
                }
            }
        });
        if (err) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText("WARNING!");
            alert.setContentText("Ic ice gecmis parcalar var! Bir parcanin hesaplanan merkezi, baska bir parcanin da alani icerisinde kaliyor."
                    + "\n"
                    + "\n"
                    + "There are overlapped patterns! The calculated centroid of a pattern is falling under another pattern.");
            alert.showAndWait();
        }
    }


    protected double scale(double number) {
        return number / PLOTTER_SCALE * 1.016;
    }


    protected void determineDrawingDimension() {
        drawingDimensions = calculateDrawingDimensions(lines);
    }


    protected Dimension2D calculateDrawingDimensions(List<Line> lines) {
        double lengthOfTheDrawing = 0;
        double widthOfTheDrawing = 0;
        for (Line line : lines) {
            if (line.getEndX() > lengthOfTheDrawing) { // check the xmax
                lengthOfTheDrawing = line.getEndX();
            }
            if (line.getEndY() > widthOfTheDrawing) { // check the ymax
                widthOfTheDrawing = line.getEndY();
            }
        }
        return new Dimension2D(lengthOfTheDrawing, widthOfTheDrawing);
    }


    protected void splitCommands() {
        commands = new ArrayList<>(Arrays.asList(fileContent.split(SPLIT_REGEX)));
        fileContent = null;
    }


    protected void removeUnwantedCharacters() {
        commands.removeAll(UNWANTED_CHARS);
    }


    protected abstract void interpretCommands();


    public void groupSortLabelsAndOptimizeRoutes(FlipHorizontally flipH, FlipVertically flipV) {
        if (!labels.isEmpty()) {
            double minPosX = getMinPosInX();
            double width = drawingDimensions.getWidth();
            double height = drawingDimensions.getHeight();
            sortedLabels = labelGroupingManager.groupAndSortLabels(labels, minPosX, width, height, flipH, flipV);
            sortedAndOptimizedLabels = organizeLabels(sortedLabels, drawingDimensions.getWidth(), DRAWING_SPLIT_WIDTH);

            addReferenceLabelToTheFinalList();
        }
    }


    /**
     * Finds the labels that have been bound to the same shape and merges them.
     *
     */
    public void mergeLabelsWithSameShape(){


      //  Find the labels that have been bound to the same shape.
        Map<Pattern, List<Label>> duplicatedShapes = sortedAndOptimizedLabels.stream()
                .filter(lbl -> lbl.getShape() != null)
                .collect(Collectors.groupingBy(Label::getShape));


        List<Label> toRemove = new ArrayList<>();

        duplicatedShapes.forEach((shape, lblList) -> {

            if (lblList.size() > 1) {
                // Find the label that has the longest text and keep it.
                Label longest = lblList.stream()
                        .max(Comparator.comparingInt(lbl -> lbl.getText().length()))
                        .orElse(lblList.get(0));

                // Add the texts of the other to the longest.
                lblList.stream()
                        .filter(lbl -> lbl != longest)
                        .forEach(shortLbl -> {
                            String currentText = longest.getText();
                            String textToAdd = shortLbl.getText();

                            // Add the short text to the end of the long text
                            longest.setText(currentText + "\n" + textToAdd);

                            // Add the label that has the shortest text to the to-be-removed list.
                            toRemove.add(shortLbl);
                        });
            }
        });

        sortedAndOptimizedLabels.removeAll(toRemove);

    }
    public void mergeLabelsIfPatternHasTwoLabels() {

        System.out.println("mergeLabelsIfPatternHasTwoLabels");
        List<Label> notHaveAShape = sortedAndOptimizedLabels.stream().filter(lbl -> lbl.getShape() == null && !lbl.getText().equals(REFERENCE_SIGN)).collect(Collectors.toList());
        Map<Label, Pattern> map = new HashMap<>();

        notHaveAShape.forEach(lbl -> {

            for (int index = 0; index < shapes.size(); index++) {
                Pattern s = shapes.get(index);
                if (Pattern.containsPoint(s.getLines(), lbl.getPosition())) {
                    map.put(lbl, s);
                    break;
                }
            }

        });

        sortedAndOptimizedLabels.removeAll(notHaveAShape);

        map.entrySet().forEach(entry -> {

            Label l = entry.getValue().getLabel();

            String labelText = l.getText();
            String textToAdd = entry.getKey().getText();
            l.setText(labelText + "\n" + textToAdd);


        });


    }


    protected void createPieces() {
        if (this instanceof GGTFileProcessor) {
            for (int i = 0; i < linesForClosedShapes.size() + 1; i++) {
                labels.add(null);
            }
        }

        for (Map.Entry<Integer, List<Line>> entry : linesForClosedShapes.entrySet()) {
            Pattern pattern = new Pattern(entry.getValue(), (this instanceof GGTFileProcessor)); //closed shape e numara vermek gerekiyor!!
            if (pattern.isValid()) {

                pattern.relocateCenterX();
                pattern.setID(entry.getKey());

                boolean isGGT = this instanceof GGTFileProcessor;

                if (isGGT) {

                    for (GGTPattern p : getGGTPatterns()) {
                        if (pattern.getID().equals(p.getId())) {

                            Label label = p.getLabel();
                            if (label != null) {


                                label.changeLabelPosition(pattern.getCenter());
                                pattern.setLabel(label);
                                label.setShape(pattern);


                            } else { //label is null
                                // cs.setLabel(new Lbl("No label data!",cs.getCenter(),0,2,12,12));
                                continue;

                            }


                            labels.set(pattern.getID(), pattern.getLabel());
                            labels.set(0, new Label(REFERENCE_SIGN, new Point2D(0, 0), 0, 0, 0, 0));

                        }
                    }


                    sortedAndOptimizedLabels = organizeLabels(labels, drawingDimensions.getWidth(), DRAWING_SPLIT_WIDTH);


                } else {
                    for (Label label : sortedAndOptimizedLabels) {

                        if (label == null)
                            continue;
                        if (Pattern.containsPoint(pattern.getLines(), label.getPosition()) && pattern.isCenterInside()) {
                            if (pattern.getLabel() == null) {
                                pattern.setLabel(label);
                                label.setShape(pattern);
                            }
                        }
                    }
                }


                boolean ItAlreadyHas = false;
                for (Pattern s : shapes) {
                    if (s.getLines().equals(pattern.getLines())) // birebir ayni sekil zaten var.
                        ItAlreadyHas = true;
                    break;
                }
                if (!ItAlreadyHas) {
                    shapes.add(pattern);
                }


            }
        }

    }



    public void flipShapes(Flipping flipping) {
        for (Pattern cs : shapes) {
            cs.restoreColor();

            // Shape'i flip et
            if (flipping == Flipping.HORIZONTAL)
                cs.mirrorX(drawingDimensions.getWidth());
            if (flipping == Flipping.VERTICAL)
                cs.mirrorY(drawingDimensions.getHeight());

            // GGT dosyaları için özel işlem
            if (this instanceof GGTFileProcessor) {
                // Mevcut label'ı koru ve pozisyonunu güncelle
                Label currentLabel = cs.getLabel();
                if (currentLabel != null) {
                    // Label pozisyonunu shape'in yeni merkezine taşı
                    currentLabel.changeLabelPosition(cs.getCenter());

                    // Shape-label ilişkisini koru
                    cs.setLabel(currentLabel);
                    currentLabel.setShape(cs);
                }
            } else {
                // Diğer formatlar için pozisyon bazlı yeniden eşleştirme
                for (Label label : sortedAndOptimizedLabels) {
                    if (Pattern.containsPoint(cs.getLines(), label.getPosition())) {
                        cs.setLabel(label);
                        label.setShape(cs);
                    }
                }
            }
        }

        // GGT için sortedAndOptimizedLbls listesini güncelle
        if (this instanceof GGTFileProcessor) {
            // Label listesini yeniden organize et (pozisyonlar güncellendiği için)
            sortedAndOptimizedLabels = organizeLabels(labels, drawingDimensions.getWidth(), DRAWING_SPLIT_WIDTH);
        }
    }


    public void clearAll() {
        commands = null;
        linesForClosedShapes = null;
        sortedAndOptimizedLabels = null;
        shapes = null;
        labels = null;
        sortedLabels = null;
        lines = null;
        fileContent = null;
        labelGroupingManager.clear();
    }


    private void addReferenceLabelToTheFinalList() {
        Label referenceLabel = new Label(REFERENCE_SIGN, new Point2D(0, 0), 0, 0, 0, 0);
        sortedAndOptimizedLabels.add(0, referenceLabel);
    }


    public String getPrintableDimensions() {

        String formattedW = String.format("%.2f", drawingDimensions.getWidth());
        String formattedH = String.format("%.2f", drawingDimensions.getHeight());
        return "L: " + formattedW + ", W: " + formattedH;
    }


    public List<GGTPattern> getGGTPatterns() {
        return GGTPatterns;
    }


    /**
     * Organizes labels in an optimal processing order.
     *
     * @param labels       List of labels to organize
     * @param drawingWidth Width of the drawing area
     * @param stripWidth   Width of each vertical strip for processing
     * @return List of labels in optimal processing order
     */
    public static List<Label> organizeLabels(List<Label> labels, double drawingWidth, double stripWidth) {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyList();
        }

        // Determine number of strips
        int stripCount = (int) Math.ceil(drawingWidth / stripWidth);

        // Create a list for each strip
        List<List<Label>> strips = new ArrayList<>(stripCount);
        for (int i = 0; i < stripCount; i++) {
            strips.add(new ArrayList<>());
        }


        // Assign each label to its appropriate strip
        for (Label label : labels) {

            if (label == null) {

                continue;
            }

            double x = label.getPosition().getX();
            int stripIndex = (int) (x / stripWidth);

            // Handle edge case where label is exactly at the boundary
            if (stripIndex == stripCount) {
                stripIndex = stripCount - 1;
            }

            // Ensure index is within bounds
            if (stripIndex >= 0 && stripIndex < stripCount) {
                strips.get(stripIndex).add(label);
            }
        }

        // Sort each strip by y-position
        for (int i = 0; i < stripCount; i++) {
            final int index = i;

            // Sort by Y position
            Collections.sort(strips.get(i), Comparator.comparingDouble(l -> l.getPosition().getY()));

            // For even-indexed strips, reverse the order for snake pattern
            if (i % 2 == 1) {
                Collections.reverse(strips.get(i));
            }
        }

        // Combine all strips into final result
        List<Label> result = new ArrayList<>(labels.size());
        for (List<Label> strip : strips) {
            result.addAll(strip);
        }

        return result;
    }
}


