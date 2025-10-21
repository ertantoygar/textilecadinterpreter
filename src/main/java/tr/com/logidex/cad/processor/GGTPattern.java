package tr.com.logidex.cad.processor;


import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.model.Lbl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a pattern (piece) in a GGT file.
 * Each pattern has an ID, a collection of lines forming its shape,
 * and associated label information.
 */
public class GGTPattern {

    private final Integer id;
    private final List<Line> lines;
    private final Map<String, Point2D> labelTextPositions;
    private Lbl consolidatedLabel;

    public GGTPattern(Integer id) {
        this.id = id;
        this.lines = new ArrayList<>();
        this.labelTextPositions = new LinkedHashMap<>();
        this.consolidatedLabel = null;
    }

    // ==================== ID ====================

    /**
     * Gets the unique identifier for this pattern.
     *
     * @return The pattern ID
     */
    public Integer getId() {
        return id;
    }

    // ==================== Line Management ====================

    /**
     * Adds a line to this pattern from start point to end point.
     * Lines with identical start and end points are ignored.
     *
     * @param start The starting point of the line
     * @param end The ending point of the line
     */
    public void addLine(Point2D start, Point2D end) {
        if (start == null || end == null) {
            return;
        }

        if (!start.equals(end)) {
            Line line = new Line(
                    start.getX(), start.getY(),
                    end.getX(), end.getY()
            );
            lines.add(line);
        }
    }

    /**
     * Turkish compatibility method for addLine.
     *
     * @param baslangic Starting point
     * @param bitis Ending point
     */
    public void cizgiEkle(Point2D baslangic, Point2D bitis) {
        addLine(baslangic, bitis);
    }

    /**
     * Gets all lines that form this pattern's shape.
     *
     * @return The lines list
     */
    public List<Line> getLines() {
        return lines;
    }

    /**
     * Gets the number of lines in this pattern.
     *
     * @return The line count
     */
    public int getLineCount() {
        return lines.size();
    }

    // ==================== Label Management ====================

    /**
     * Gets the consolidated label for this pattern.
     *
     * @return The label, or null if no label has been set
     */
    public Lbl getLabel() {
        return consolidatedLabel;
    }

    /**
     * Sets the consolidated label for this pattern.
     *
     * @param label The label to set
     */
    public void setLabel(Lbl label) {
        this.consolidatedLabel = label;
    }

    /**
     * Turkish compatibility method for setLabel.
     *
     * @param label The label to set
     */
    public void setEtiket(Lbl label) {
        setLabel(label);
    }

    /**
     * Gets the map of label text strings to their positions.
     * This represents individual label components before consolidation.
     *
     * @return A map of label text to positions
     */
    public Map<String, Point2D> getLabelTextPositions() {
        return labelTextPositions;
    }

    /**
     * Turkish compatibility method for getLabelTextPositions.
     *
     * @return The label text positions map
     */
    public Map<String, Point2D> getParcaninEtiketleri() {
        return getLabelTextPositions();
    }

    /**
     * Adds a label text entry with its position.
     *
     * @param text The label text
     * @param position The position of the label
     */
    public void addLabelText(String text, Point2D position) {
        if (text != null && position != null) {
            labelTextPositions.put(text, position);
        }
    }

    /**
     * Checks if this pattern has any label text entries.
     *
     * @return true if label text exists, false otherwise
     */
    public boolean hasLabelText() {
        return !labelTextPositions.isEmpty();
    }

    /**
     * Checks if this pattern has a consolidated label.
     *
     * @return true if a label is set, false otherwise
     */
    public boolean hasLabel() {
        return consolidatedLabel != null;
    }

    // ==================== Utility Methods ====================

    @Override
    public String toString() {
        return String.format("GGTPattern[id=%d, lines=%d, labels=%d, hasConsolidatedLabel=%b]",
                id, lines.size(), labelTextPositions.size(), hasLabel());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GGTPattern other = (GGTPattern) obj;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}