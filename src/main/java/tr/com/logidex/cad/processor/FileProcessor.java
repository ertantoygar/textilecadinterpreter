package tr.com.logidex.cad.processor;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.*;
import tr.com.logidex.cad.model.ClosedShape;
import tr.com.logidex.cad.model.Lbl;

import java.util.*;
import java.util.stream.Collectors;

public abstract class FileProcessor {
    public static final String REFERENCE_SIGN = "+";
    private static final double DRAWING_SPLIT_WIDTH = 50;
    public static Unit unit;
    public List<Lbl> sortedAndOptimizedLbls = new ArrayList<Lbl>();


    public Dimension2D drawingDimensions = new Dimension2D(0, 0);
    public List<ClosedShape> shapes = new ArrayList<ClosedShape>();
    public List<Lbl> labels = new ArrayList<Lbl>();
    public List<Lbl> sortedLbls = new ArrayList<Lbl>();
    public List<Line> lines = new ArrayList<Line>();
    protected List<String> UNWANTED_CHARS;
    protected String SPLIT_REGEX;
    protected List<String> commands;

    protected HashMap<Integer, List<Line>> linesForClosedShapes = new HashMap<Integer, List<Line>>();
    boolean err = false;
    private String fileContent;
    private LabelGroupingManager labelGroupingManager = new LabelGroupingManager();
    private List<Parca> GGTParcalar = new ArrayList<>();
    private static final double PLOTTER_SCALE = 40;
    private FlipHorizontally flipHorizontally = FlipHorizontally.NO;
    private FlipVertically flipVertically = FlipVertically.NO;

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

    public List<ClosedShape> getShapes() {
        return shapes;
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
                    if (ClosedShape.pointInPolygon(sh.getLines(), shapes.get(i).getCenter())) {
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
            sortedLbls = labelGroupingManager.groupAndSortLabels(labels, minPosX, width, height, flipH, flipV);
            sortedAndOptimizedLbls = organizeLabels(sortedLbls, drawingDimensions.getWidth(), DRAWING_SPLIT_WIDTH);

            addReferenceLabelToTheFinalList();
        }
    }


    /**
     * Finds the labels that have been bound to the same shape and merges them.
     *
     */
    public void mergeLabelsWithSameShape(){


      //  Find the labels that have been bound to the same shape.
        Map<ClosedShape, List<Lbl>> duplicatedShapes = sortedAndOptimizedLbls.stream()
                .filter(lbl -> lbl.getShape() != null)
                .collect(Collectors.groupingBy(Lbl::getShape));


        List<Lbl> toRemove = new ArrayList<>();

        duplicatedShapes.forEach((shape, lblList) -> {

            if (lblList.size() > 1) {
                // Find the label that has the longest text and keep it.
                Lbl longest = lblList.stream()
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

        sortedAndOptimizedLbls.removeAll(toRemove);

    }
    public void mergeLabelsIfPatternHasTwoLabels() {

        System.out.println("mergeLabelsIfPatternHasTwoLabels");
        List<Lbl> notHaveAShape = sortedAndOptimizedLbls.stream().filter(lbl -> lbl.getShape() == null && !lbl.getText().equals(REFERENCE_SIGN)).collect(Collectors.toList());
        Map<Lbl, ClosedShape> map = new HashMap<>();

        notHaveAShape.forEach(lbl -> {

            for (int index = 0; index < shapes.size(); index++) {
                ClosedShape s = shapes.get(index);
                if (ClosedShape.pointInPolygon(s.getLines(), lbl.getPosition())) {
                    map.put(lbl, s);
                    break;
                }
            }

        });

        sortedAndOptimizedLbls.removeAll(notHaveAShape);

        map.entrySet().forEach(entry -> {

            Lbl l = entry.getValue().getLabel();

            if(l!=null){

            String labelText = l.getText();
            String textToAdd = entry.getKey().getText();
            l.setText(labelText + "\n" + textToAdd);
            }


        });


    }


    protected void createPieces() {
        if (this instanceof GGTFileProcessor) {
            for (int i = 0; i < linesForClosedShapes.size() + 1; i++) {
                labels.add(null);
            }
        }

        for (Map.Entry<Integer, List<Line>> entry : linesForClosedShapes.entrySet()) {
            ClosedShape cs = new ClosedShape(entry.getValue(), (this instanceof GGTFileProcessor)); //closed shape e numara vermek gerekiyor!!
            if (cs.isAvalidPiece()) {

                cs.relocateTheOriginInXaxis();
                cs.setID(entry.getKey());

                boolean isGGT = this instanceof GGTFileProcessor;

                if (isGGT) {

                    for (Parca p : getGGTParcalar()) {
                        if (cs.getID() == p.getId()) {

                            Lbl label = p.getLabel();
                            if (label != null) {


                                label.changeLabelPosition(cs.getCenter());
                                cs.setLabel(label);
                                label.setShape(cs);


                            } else { //label is null
                                // cs.setLabel(new Lbl("No label data!",cs.getCenter(),0,2,12,12));
                                continue;

                            }


                            labels.set(cs.getID(), cs.getLbl());
                            labels.set(0, new Lbl(REFERENCE_SIGN, new Point2D(0, 0), 0, 0, 0, 0));

                        }
                    }


                    sortedAndOptimizedLbls = organizeLabels(labels, drawingDimensions.getWidth(), DRAWING_SPLIT_WIDTH);


                } else {
                    for (Lbl lbl : sortedAndOptimizedLbls) {

                        if (lbl == null)
                            continue;
                        if (cs.pointInPolygon(cs.getLines(), lbl.getPosition()) && cs.isCalculatedCenterPointIsInThisShape()) {
                            if (cs.getLabel() == null) {
                                cs.setLabel(lbl);
                                lbl.setShape(cs);
                            }
                        }
                    }
                }


                boolean ItAlreadyHas = false;
                for (ClosedShape s : shapes) {
                    if (s.getLines().equals(cs.getLines())) // birebir ayni sekil zaten var.
                        ItAlreadyHas = true;
                    break;
                }
                if (!ItAlreadyHas) {
                    shapes.add(cs);
                }


            }
        }

    }


    //    public void flipShapes(Flipping flipping) {
//        for (ClosedShape cs : shapes) {
//            cs.restoreColor();
//            if (flipping == Flipping.HORIZONTAL)
//                cs.mirrorX(drawingDimensions.getWidth());
//            if (flipping == Flipping.VERTICAL)
//                cs.mirrorY(drawingDimensions.getHeight());
//            for (Lbl lbl : sortedAndOptimizedLbls) {
//                if (ClosedShape.pointInPolygon(cs.getLines(), lbl.getPosition())) {
//                    cs.setLabel(lbl);
//                    lbl.setShape(cs);
//                }
//            }
//        }
//    }
    public void flipShapes(Flipping flipping) {
        for (ClosedShape cs : shapes) {
            cs.restoreColor();

            // Shape'i flip et
            if (flipping == Flipping.HORIZONTAL)
                cs.mirrorX(drawingDimensions.getWidth());
            if (flipping == Flipping.VERTICAL)
                cs.mirrorY(drawingDimensions.getHeight());

            // GGT dosyaları için özel işlem
            if (this instanceof GGTFileProcessor) {
                // Mevcut label'ı koru ve pozisyonunu güncelle
                Lbl currentLabel = cs.getLabel();
                if (currentLabel != null) {
                    // Label pozisyonunu shape'in yeni merkezine taşı
                    currentLabel.changeLabelPosition(cs.getCenter());

                    // Shape-label ilişkisini koru
                    cs.setLabel(currentLabel);
                    currentLabel.setShape(cs);
                }
            } else {
                // Diğer formatlar için pozisyon bazlı yeniden eşleştirme
                for (Lbl lbl : sortedAndOptimizedLbls) {
                    if (ClosedShape.pointInPolygon(cs.getLines(), lbl.getPosition())) {
                        cs.setLabel(lbl);
                        lbl.setShape(cs);
                    }
                }
            }
        }

        // GGT için sortedAndOptimizedLbls listesini güncelle
        if (this instanceof GGTFileProcessor) {
            // Label listesini yeniden organize et (pozisyonlar güncellendiği için)
            sortedAndOptimizedLbls = organizeLabels(labels, drawingDimensions.getWidth(), DRAWING_SPLIT_WIDTH);
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


    private void addReferenceLabelToTheFinalList() {
        Lbl referenceLabel = new Lbl(REFERENCE_SIGN, new Point2D(0, 0), 0, 0, 0, 0);
        sortedAndOptimizedLbls.add(0, referenceLabel);
    }


    public String getPrintableDimensions() {

        String formattedW = String.format("%.2f", drawingDimensions.getWidth());
        String formattedH = String.format("%.2f", drawingDimensions.getHeight());
        return "L: " + formattedW + ", W: " + formattedH;
    }


    public List<Parca> getGGTParcalar() {
        return GGTParcalar;
    }


    /**
     * Organizes labels in an optimal processing order.
     *
     * @param labels       List of labels to organize
     * @param drawingWidth Width of the drawing area
     * @param stripWidth   Width of each vertical strip for processing
     * @return List of labels in optimal processing order
     */
    public static List<Lbl> organizeLabels(List<Lbl> labels, double drawingWidth, double stripWidth) {
        if (labels == null || labels.isEmpty()) {
            return Collections.emptyList();
        }

        // Determine number of strips
        int stripCount = (int) Math.ceil(drawingWidth / stripWidth);

        // Create a list for each strip
        List<List<Lbl>> strips = new ArrayList<>(stripCount);
        for (int i = 0; i < stripCount; i++) {
            strips.add(new ArrayList<>());
        }


        // Assign each label to its appropriate strip
        for (Lbl label : labels) {

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
        List<Lbl> result = new ArrayList<>(labels.size());
        for (List<Lbl> strip : strips) {
            result.addAll(strip);
        }

        return result;
    }

    public void invertFlipH() {
        flipHorizontally = flipHorizontally == FlipHorizontally.YES ? FlipHorizontally.NO : FlipHorizontally.YES;

        // GGT dosyaları için özel işlem
        if (this instanceof GGTFileProcessor) {
            // Sadece shape'leri flip et, label organizasyonuna dokunma
          flipShapes(Flipping.HORIZONTAL);
        } else {
            // Diğer dosya tipleri için normal işlem
            groupSortLabelsAndOptimizeRoutes(flipHorizontally, flipVertically);
            flipShapes(Flipping.HORIZONTAL);
           mergeLabelsWithSameShape();
        }


    }

    public void invertFlipV() {
        flipVertically = flipVertically == FlipVertically.YES ? FlipVertically.NO : FlipVertically.YES;

        // GGT dosyaları için özel işlem
        if (this instanceof GGTFileProcessor) {
            // Sadece shape'leri flip et, label organizasyonuna dokunma
         flipShapes(Flipping.VERTICAL);
        } else {
            // Diğer dosya tipleri için normal işlem
            groupSortLabelsAndOptimizeRoutes(flipHorizontally, flipVertically);
            flipShapes(Flipping.VERTICAL);
            mergeLabelsWithSameShape();
        }



    }
}


