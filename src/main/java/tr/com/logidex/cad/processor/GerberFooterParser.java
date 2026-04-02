package tr.com.logidex.cad.processor;

import tr.com.logidex.cad.geometry.Point2D;
import tr.com.logidex.cad.model.Lbl;

import java.util.*;

/**
 * Parses structured footer label data from Gerber .cut files.
 * <p>
 * Some Gerber files store label information in a footer section (L,P,D blocks)
 * instead of inline M31 commands. This parser extracts those labels and maps them
 * to piece numbers.
 * <p>
 * Footer format:
 * <pre>
 *   V,field_name,index,width     — field definitions
 *   L,piece_number               — start of label block
 *   s,section                    — section marker (ignored)
 *   P,x,y,rotation,mirror        — label position
 *   D,field_index,value          — data field value
 *   E,count,flag                 — end marker
 * </pre>
 */
final class GerberFooterParser {

    private GerberFooterParser() {
    }

    /**
     * Parses footer label blocks from raw Gerber file content.
     *
     * @param rawFileContent the complete file content
     * @param scaleFactor    the coordinate scale factor (e.g. 10.0 for Gerber)
     * @return map of piece number to Lbl, or empty map if no footer found
     */
    static Map<Integer, Lbl> parseFooterLabels(String rawFileContent, double scaleFactor) {
        String footerText = extractFooterText(rawFileContent);
        if (footerText == null) {
            return Collections.emptyMap();
        }

        String[] footerLines = footerText.split("[\\r\\n]+");
        return parseLabelBlocks(footerLines, scaleFactor);
    }

    /**
     * Extracts the footer text portion from the raw file content.
     * The footer starts after the last '*' character in the geometry section.
     */
    private static String extractFooterText(String rawFileContent) {
        if (rawFileContent == null || rawFileContent.isEmpty()) {
            return null;
        }

        int lastStar = rawFileContent.lastIndexOf('*');
        if (lastStar < 0 || lastStar >= rawFileContent.length() - 1) {
            return null;
        }

        // Footer is everything before the last '*' that contains L, blocks
        // The actual footer is embedded between geometry end and file end
        // Look for the first 'L,' pattern that signals label blocks
        String candidate = rawFileContent.substring(lastStar + 1);

        // The footer may also be between the geometry end marker and the trailing L0*
        // Search backwards from end for the structured data
        int footerSearchStart = rawFileContent.lastIndexOf("\nL,");
        if (footerSearchStart < 0) {
            footerSearchStart = rawFileContent.lastIndexOf("\rL,");
        }
        if (footerSearchStart < 0) {
            return null;
        }

        // Find the actual start of the footer section by looking for V, or N, or M, lines
        // that precede the L, blocks
        int sectionStart = footerSearchStart;
        String beforeLabels = rawFileContent.substring(0, footerSearchStart);
        int vStart = beforeLabels.lastIndexOf("\nV,");
        if (vStart < 0) vStart = beforeLabels.lastIndexOf("\rV,");
        int mStart = beforeLabels.lastIndexOf("\nM,");
        if (mStart < 0) mStart = beforeLabels.lastIndexOf("\rM,");
        int nStart = beforeLabels.lastIndexOf("\nN,");
        if (nStart < 0) nStart = beforeLabels.lastIndexOf("\rN,");

        // Take the earliest of V, M, N markers as footer start
        int earliest = sectionStart;
        if (vStart > 0) earliest = Math.min(earliest, vStart);
        if (mStart > 0) earliest = Math.min(earliest, mStart);
        if (nStart > 0) earliest = Math.min(earliest, nStart);

        String footer = rawFileContent.substring(earliest);

        // Verify this actually contains label blocks (L, followed by P, and D,)
        if (!footer.contains("L,") || !footer.contains("P,") || !footer.contains("D,")) {
            return null;
        }

        return footer;
    }

    /**
     * Parses label blocks from footer lines.
     */
    private static Map<Integer, Lbl> parseLabelBlocks(String[] lines, double scaleFactor) {
        Map<Integer, Lbl> result = new LinkedHashMap<>();

        Integer currentPieceNumber = null;
        double posX = 0, posY = 0;
        List<String> dataFields = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("L,")) {
                // Save previous block if exists
                if (currentPieceNumber != null) {
                    saveLabelBlock(result, currentPieceNumber, posX, posY, dataFields, scaleFactor);
                }
                // Start new block
                currentPieceNumber = parsePieceNumber(line);
                posX = 0;
                posY = 0;
                dataFields = new ArrayList<>();

            } else if (line.startsWith("P,") && currentPieceNumber != null) {
                double[] pos = parsePosition(line);
                if (pos != null) {
                    posX = pos[0];
                    posY = pos[1];
                }

            } else if (line.startsWith("D,") && currentPieceNumber != null) {
                String value = parseDataFieldValue(line);
                if (value != null && !value.isEmpty()) {
                    dataFields.add(value);
                }

            } else if (line.startsWith("E,")) {
                // End marker — save last block and stop
                if (currentPieceNumber != null) {
                    saveLabelBlock(result, currentPieceNumber, posX, posY, dataFields, scaleFactor);
                }
                break;
            }
            // Skip V,, M,, N,, s,, B,, R,, F,, S,, T, and other header lines
        }

        // Handle case where no E, marker exists
        if (currentPieceNumber != null && !result.containsKey(currentPieceNumber)) {
            saveLabelBlock(result, currentPieceNumber, posX, posY, dataFields, scaleFactor);
        }

        return result;
    }

    private static void saveLabelBlock(Map<Integer, Lbl> result, int pieceNumber,
                                       double posX, double posY,
                                       List<String> dataFields, double scaleFactor) {
        if (dataFields.isEmpty()) {
            return;
        }

        String text = String.join("\n", dataFields);
        double scaledX = posX / scaleFactor;
        double scaledY = posY / scaleFactor;
        Point2D position = new Point2D(scaledX, scaledY);

        Lbl label = new Lbl(text, position, 0, 2, 12, 12);
        result.put(pieceNumber, label);
    }

    /**
     * Parses piece number from "L,54" format.
     */
    private static Integer parsePieceNumber(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1].trim());
            }
        } catch (NumberFormatException e) {
            // Skip invalid L lines
        }
        return null;
    }

    /**
     * Parses position from "P,29156,5152,0,0" format.
     * Returns [x, y] as raw (unscaled) values.
     */
    private static double[] parsePosition(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length >= 3) {
                double x = Double.parseDouble(parts[1].trim());
                double y = Double.parseDouble(parts[2].trim());
                return new double[]{x, y};
            }
        } catch (NumberFormatException e) {
            // Skip invalid P lines
        }
        return null;
    }

    /**
     * Parses data field value from "D,1,697793-AR-R6-IMLT" format.
     * Returns the value portion, or null if empty.
     * Handles edge cases like "D,9,,255,210,130" where value is empty.
     */
    private static String parseDataFieldValue(String line) {
        // Format: D,<index>,<value>[,<extra>...]
        int firstComma = line.indexOf(',');
        if (firstComma < 0) return null;

        int secondComma = line.indexOf(',', firstComma + 1);
        if (secondComma < 0) return null;

        // Value is between second comma and third comma (or end of line)
        int thirdComma = line.indexOf(',', secondComma + 1);
        String value;
        if (thirdComma < 0) {
            value = line.substring(secondComma + 1).trim();
        } else {
            value = line.substring(secondComma + 1, thirdComma).trim();
        }

        return value.isEmpty() ? null : value;
    }
}
