package tr.com.logidex.cad.helper;

import javafx.geometry.Point2D;
import tr.com.logidex.cad.processor.FlipHorizontally;
import tr.com.logidex.cad.processor.FlipVertically;
import tr.com.logidex.cad.model.Lbl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages the grouping and sorting of labels in CAD files.
 * Groups nearby labels together and creates consolidated label entries.
 */
public class LabelGroupingManager {

    // Constants
    private static final double GROUPING_DISTANCE_THRESHOLD = 20.0;
    private static final int MAX_SINGLE_LINE_LENGTH = 150;
    private static final char CARRIAGE_RETURN = 0x0d;

    // State
    private ArrayList<Lbl> lbls;
    private ArrayList<ArrayList<Lbl>> groupedLabels;
    private ArrayList<Lbl> finalLabels;
    private boolean isFlipHorizontal;
    private boolean isFlipVertical;
    private double maxX;
    private double minX;
    private double totalWidth;
    private double totalHeight;

    /**
     * Groups and sorts labels based on proximity and orientation.
     *
     * @param labelsToGroup The raw list of labels from the file
     * @param minPosX The minimum X position in the drawing
     * @param totalLength The total length of the pattern
     * @param totalWidth The total width of the pattern
     * @param flipHorizontal Whether the pattern is flipped horizontally
     * @param flipVertical Whether the pattern is flipped vertically
     * @return A list of grouped and sorted labels
     */
    public List<Lbl> groupAndSortLabels(List<Lbl> labelsToGroup, double minPosX,
                                        double totalLength, double totalWidth,
                                        FlipHorizontally flipHorizontal,
                                        FlipVertically flipVertical) {

        initializeState(labelsToGroup, minPosX, totalLength, totalWidth, flipHorizontal, flipVertical);

        List<ArrayList<Lbl>> groups = groupNearbyLabels();
        createFinalLabelList(groups);

        return finalLabels;
    }

    public void clear() {
        lbls = null;
        groupedLabels = null;
        finalLabels = null;
    }

    // ==================== Initialization ====================

    private void initializeState(List<Lbl> labelsToGroup, double minPosX,
                                 double totalLength, double totalWidth,
                                 FlipHorizontally flipHorizontal,
                                 FlipVertically flipVertical) {
        this.lbls = new ArrayList<>(labelsToGroup);
        this.minX = minPosX;
        this.maxX = totalLength;
        this.totalWidth = totalLength;
        this.totalHeight = totalWidth;
        this.isFlipHorizontal = flipHorizontal == FlipHorizontally.YES;
        this.isFlipVertical = flipVertical == FlipVertically.YES;
        this.groupedLabels = new ArrayList<>();
    }

    // ==================== Grouping Logic ====================

    /**
     * Groups labels that are close to each other.
     * Labels within GROUPING_DISTANCE_THRESHOLD of each other are grouped together.
     */
    private List<ArrayList<Lbl>> groupNearbyLabels() {
        List<ArrayList<Lbl>> groups = new ArrayList<>();
        ArrayList<Lbl> currentGroup = new ArrayList<Lbl>();

        for (int i = 0; i < lbls.size(); i++) {
            Lbl currentLabel = lbls.get(i);
            Lbl nextLabel = getNextLabel(i);

            if (shouldAddToGroup(currentLabel, nextLabel, currentGroup)) {
                currentGroup.add(currentLabel);
            } else {
                currentGroup.add(currentLabel);
                groups.add(currentGroup);
                currentGroup = new ArrayList<Lbl>();
            }
        }

        // Add the last group if it has content
        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }

        return groups;
    }

    private Lbl getNextLabel(int currentIndex) {
        try {
            return lbls.get(currentIndex + 1);
        } catch (Exception e) {
            return lbls.get(currentIndex); // Return current if no next exists
        }
    }

    /**
     * Determines if a label should be added to the current group.
     * Labels are grouped if they are within the distance threshold.
     */
    private boolean shouldAddToGroup(Lbl currentLabel, Lbl nextLabel, ArrayList<Lbl> currentGroup) {
        if (currentLabel == null || nextLabel == null) {
            return false;
        }

        double distance = calculateDistance(currentLabel, nextLabel);
        return distance <= GROUPING_DISTANCE_THRESHOLD;
    }

    /**
     * Calculates the Euclidean distance between two labels.
     */
    private double calculateDistance(Lbl label1, Lbl label2) {
        double dx = label1.getPosition().getX() - label2.getPosition().getX();
        double dy = label1.getPosition().getY() - label2.getPosition().getY();
        return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    }

    // ==================== Final Label Creation ====================

    /**
     * Creates the final list of labels ready for export.
     */
    private void createFinalLabelList(List<ArrayList<Lbl>> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }

        finalLabels = new ArrayList<>();
        removeDuplicateTextLines(groups);

        for (ArrayList<Lbl> group : groups) {
            if (group == null || group.isEmpty()) {
                continue;
            }

            Lbl consolidatedLabel = createConsolidatedLabel(group);
            finalLabels.add(consolidatedLabel);
        }

        removeInvalidLabels();
    }

    /**
     * Creates a single consolidated label from a group of labels.
     */
    private Lbl createConsolidatedLabel(ArrayList<Lbl> group) {
        int referenceRow = determineReferenceRow(group.size());
        Lbl referenceLabel = group.get(referenceRow);

        Point2D position = calculateLabelPosition(group, referenceRow, referenceLabel);
        String text = consolidateGroupText(group);

        return new Lbl(
                text,
                position,
                referenceLabel.getAngle(),
                referenceLabel.getOrigin(),
                referenceLabel.getWidth(),
                referenceLabel.getHeight()
        );
    }

    /**
     * Calculates the position for a consolidated label based on flipping and reference row.
     */
    private Point2D calculateLabelPosition(ArrayList<Lbl> group, int referenceRow, Lbl referenceLabel) {
        double labelXoffset = isFlipHorizontal
                ? totalWidth
                : referenceLabel.getPosition().getX() * 2;

        double labelYoffset = isFlipVertical
                ? totalHeight
                : referenceLabel.getPosition().getY() * 2;

        double x = labelXoffset - referenceLabel.getPosition().getX();
        double y = labelYoffset - referenceLabel.getPosition().getY();

        return new Point2D(x, y);
    }

    /**
     * Consolidates all text from a group into a single string.
     */
    private String consolidateGroupText(ArrayList<Lbl> group) {
        StringBuilder sb = new StringBuilder();
        for (Lbl label : group) {
            sb.append(label.getText()).append(CARRIAGE_RETURN);
        }
        return sb.toString();
    }

    /**
     * Determines which row in the group should be used as the reference position.
     * Aims to select a row near the center of the label.
     */
    private int determineReferenceRow(int rowCount) {
        if (rowCount <= 1) {
            return 0;
        } else if (rowCount == 2) {
            return 1;
        } else if (rowCount <= 6) {
            return 2;
        } else if (rowCount == 7) {
            return 3;
        }
        return 2; // Default fallback
    }

    // ==================== Validation and Cleanup ====================

    /**
     * Removes duplicate text lines within groups.
     * If a line's text is contained in a later line, the earlier line is removed.
     */
    private void removeDuplicateTextLines(List<ArrayList<Lbl>> groups) {
        for (ArrayList<Lbl> group : groups) {
            for (int i = 0; i < group.size(); i++) {
                Lbl currentLabel = group.get(i);

                for (int j = i + 1; j < group.size(); j++) {
                    Lbl nextLabel = group.get(j);

                    if (nextLabel.getText().contains(currentLabel.getText())) {
                        group.remove(currentLabel);
                        i--; // Adjust index after removal
                        break;
                    }
                }
            }
        }
    }

    /**
     * Removes invalid labels from the final list.
     * Labels are invalid if they are outside the drawing bounds or exceed length limits.
     */
    private void removeInvalidLabels() {
        Iterator<Lbl> iter = finalLabels.iterator();

        while (iter.hasNext()) {
            Lbl label = iter.next();

            if (isInvalidLabel(label)) {
                iter.remove();
            }
        }
    }

    /**
     * Checks if a label is invalid based on position and text length.
     */
    private boolean isInvalidLabel(Lbl label) {
        double labelPosX = label.getPosition().getX();
        int rowCount = getLabelRowCount(label);

        // Label is outside drawing bounds
        boolean outsideBounds = labelPosX <= minX || labelPosX >= maxX;

        // Single-line label exceeds maximum length
        boolean exceedsLength = rowCount == 1 && label.getText().length() > MAX_SINGLE_LINE_LENGTH;

        return outsideBounds || exceedsLength;
    }

    /**
     * Gets the number of rows (lines) in a label's text.
     */
    private int getLabelRowCount(Lbl label) {
        String[] rows = label.getText().split("\\n");
        return rows.length;
    }
}