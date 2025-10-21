package tr.com.logidex.cad.processor;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.model.Lbl;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processor for GGT format CAD files.
 * Handles pattern parsing, label extraction, and coordinate scaling.
 */
public class GGTFileProcessor extends FileProcessor {

    private static final double SCALE_BASE = 0.025;
    private static final double SCALE_MULTIPLIER_1 = 1.016;
    private static final double SCALE_MULTIPLIER_2 = 10.0f;

    // Regex patterns
    private static final Pattern PIECE_START_PATTERN = Pattern.compile("N(\\d+)\\*");
    private static final Pattern LABEL_PATTERN = Pattern.compile("M31\\*X(-?\\d+)Y(-?\\d+)\\*([^*]+)\\*");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("X(-?\\d+)Y(-?\\d+)");

    // Command patterns for preprocessing
    private static final String M19_DISCONTINUITY_PATTERN = "\\*M19(\\*X\\d+Y\\d+)\\*M15\\*X\\d+Y\\d+\\*M14";
    private static final String M15_BETWEEN_M19_M31_PATTERN = "(\\*M19\\*X\\d+Y\\d+)\\*M15(\\*M31)";

    // Commands
    private static final String CMD_KNIFE_DOWN = "M14";
    private static final String CMD_KNIFE_UP = "M15";
    private static final String CMD_DELIMITER = "\\*";

    private List<GGTPattern> patterns;
    private GGTPattern activePattern;
    private final String fileContent;

    public GGTFileProcessor(String fileContent) {
        super(fileContent);
        this.patterns = new ArrayList<>();
        this.activePattern = null;
        this.fileContent = fileContent;
    }

    @Override
    protected void initUnwantedChars() {
        UNWANTED_CHARS = Arrays.asList("", null, "\32", "\n", "\r");
    }

    @Override
    protected void initSplitRegex() {
        SPLIT_REGEX = "N\\d+\\*";
    }

    @Override
    protected void interpretCommands() {
        patterns = parse(fileContent);

        for (GGTPattern pattern : patterns) {
            System.out.println("=== Pattern: " + pattern.getId() + " ===");

            List<Line> scaledLines = scaleLines(pattern.getLines());

            super.lines.addAll(scaledLines);
            super.linesForClosedShapes.put(pattern.getId(), scaledLines);
            super.getGGTParcalar().add(pattern);

            System.out.println("Line count: " + scaledLines.size());
            System.out.println("------------------------");
            System.out.println(pattern.getId() + " -> " + pattern.getLabel());
        }
    }

    // ==================== Parsing Methods ====================

    /**
     * Parses the GGT file content into a list of GGTPattern objects.
     *
     * @param fileContent The raw file content
     * @return List of parsed patterns
     */
    public List<GGTPattern> parse(String fileContent) {
        patterns.clear();

        List<PatternLocation> patternLocations = findPatternLocations(fileContent);

        for (PatternLocation location : patternLocations) {
            String patternContent = fileContent.substring(location.start, location.end);
            System.out.println(patternContent + "\n");

            activePattern = new GGTPattern(location.patternNumber);
            patterns.add(activePattern);

            parseLabels(patternContent, 0);
            String preprocessedContent = preprocessCommands(patternContent);
            parseCommands(preprocessedContent);
        }

        return patterns;
    }

    /**
     * Finds all pattern locations in the file content.
     *
     * @param fileContent The file content to search
     * @return List of pattern locations with start/end indices
     */
    private List<PatternLocation> findPatternLocations(String fileContent) {
        List<PatternLocation> locations = new ArrayList<>();
        Matcher matcher = PIECE_START_PATTERN.matcher(fileContent);

        List<Integer> startIndices = new ArrayList<>();
        List<Integer> patternNumbers = new ArrayList<>();

        while (matcher.find()) {
            startIndices.add(matcher.start());
            patternNumbers.add(Integer.parseInt(matcher.group(1)));
        }

        for (int i = 0; i < startIndices.size(); i++) {
            int start = startIndices.get(i);
            int end = (i < startIndices.size() - 1) ? startIndices.get(i + 1) : fileContent.length();
            locations.add(new PatternLocation(patternNumbers.get(i), start, end));
        }

        return locations;
    }

    /**
     * Preprocesses commands to fix known GGT format issues.
     *
     * @param content The raw pattern content
     * @return Preprocessed content
     */
    private String preprocessCommands(String content) {
        String fixed = fixM19Discontinuities(content);
        return removeM15BetweenM19AndM31(fixed);
    }

    /**
     * Fixes M19 discontinuities in the GGT format.
     * Pattern: M19*X[num]Y[num]*M15*X[num]Y[num]*M14
     * Removes M19, M15, and M14, keeping only coordinates.
     *
     * @param input The input string
     * @return Fixed string
     */
    public static String fixM19Discontinuities(String input) {
        return input.replaceAll(M19_DISCONTINUITY_PATTERN, "$1");
    }

    /**
     * Removes M15 commands between M19 and M31.
     * Pattern: *M19*X[num]Y[num]*M15*M31
     *
     * @param input The input string
     * @return Fixed string
     */
    public static String removeM15BetweenM19AndM31(String input) {
        return input.replaceAll(M15_BETWEEN_M19_M31_PATTERN, "$1$2");
    }

    // ==================== Label Parsing ====================

    /**
     * Recursively parses labels (M31 commands) from piece content.
     *
     * @param pieceContent The piece content to parse
     * @param searchIndex The index to start searching from
     */
    private void parseLabels(String pieceContent, int searchIndex) {
        Matcher matcher = LABEL_PATTERN.matcher(pieceContent);

        if (matcher.find(searchIndex)) {
            try {
                double x = Double.parseDouble(matcher.group(1));
                double y = Double.parseDouble(matcher.group(2));
                String labelText = matcher.group(3);

                activePattern.getParcaninEtiketleri().put(labelText, new Point2D(x, y));

                int end = matcher.end();
                parseLabels(pieceContent, end);
            } catch (NumberFormatException e) {
                System.err.println("Label parse error: " + e.getMessage());
            }
        }

        assignLabel();
    }

    /**
     * Assigns a consolidated label to the active piece from all label text entries.
     */
    private void assignLabel() {
        Map<String, Point2D> labelData = activePattern.getLabelTextPositions();

        if (!labelData.isEmpty()) {
            StringBuilder labelTextBuilder = new StringBuilder();
            AtomicReference<Point2D> position = new AtomicReference<>(new Point2D(0, 0));

            labelData.forEach((text, pos) -> {
                labelTextBuilder.append(text).append("\n");
                position.set(pos);
            });

            Lbl createdLabel = new Lbl(labelTextBuilder.toString(), position.get(), 0, 2, 12, 12);
            activePattern.setLabel(createdLabel);
        }
    }

    // ==================== Command Parsing ====================

    /**
     * Parses drawing commands (M14/M15 knife up/down and coordinates) from piece content.
     *
     * @param pieceContent The preprocessed piece content
     */
    private void parseCommands(String pieceContent) {
        String[] commands = pieceContent.split(CMD_DELIMITER);

        Point2D currentPosition = null;
        boolean cutting = false;
        List<Point2D> cuttingPoints = new ArrayList<>();

        for (String command : commands) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }

            command = command.trim();

            if (command.equals(CMD_KNIFE_DOWN)) {
                cutting = true;
                cuttingPoints.clear();
            } else if (command.equals(CMD_KNIFE_UP)) {
                if (cutting && cuttingPoints.size() > 1) {
                    createLinesFromPoints(cuttingPoints);
                    closeShapeIfNeeded(cuttingPoints);
                }
                cutting = false;
                cuttingPoints.clear();
            } else if (command.startsWith("X") && command.contains("Y")) {
                Point2D newPosition = parseCoordinate(command);
                if (newPosition != null) {
                    if (cutting) {
                        cuttingPoints.add(newPosition);
                    }
                    currentPosition = newPosition;
                }
            }
        }
    }

    /**
     * Creates lines connecting consecutive points.
     *
     * @param points The list of points to connect
     */
    private void createLinesFromPoints(List<Point2D> points) {
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D start = points.get(i);
            Point2D end = points.get(i + 1);
            activePattern.addLine(start, end);
        }
    }

    /**
     * Closes the shape by connecting the last point to the first if they're not equal.
     *
     * @param points The list of points forming the shape
     */
    private void closeShapeIfNeeded(List<Point2D> points) {
        Point2D firstPoint = points.get(0);
        Point2D lastPoint = points.get(points.size() - 1);

        if (!firstPoint.equals(lastPoint)) {
            activePattern.addLine(lastPoint, firstPoint);
        }
    }

    /**
     * Parses a coordinate command (X[num]Y[num]).
     *
     * @param command The command string
     * @return The parsed Point2D, or null if parsing fails
     */
    private Point2D parseCoordinate(String command) {
        Matcher matcher = COORDINATE_PATTERN.matcher(command);

        if (matcher.find()) {
            try {
                double x = Double.parseDouble(matcher.group(1));
                double y = Double.parseDouble(matcher.group(2));
                return new Point2D(x, y);
            } catch (NumberFormatException e) {
                System.err.println("Coordinate parse error: " + command);
                return null;
            }
        }
        return null;
    }

    // ==================== Utility Methods ====================

    /**
     * Scales all lines using the GGT-specific scale factor.
     *
     * @param originalLines The original lines to scale
     * @return List of scaled lines
     */
    private List<Line> scaleLines(List<Line> originalLines) {
        List<Line> scaledLines = new ArrayList<>();

        for (Line line : originalLines) {
            Line scaledLine = new Line(
                    this.scale(line.getStartX()),
                    this.scale(line.getStartY()),
                    this.scale(line.getEndX()),
                    this.scale(line.getEndY())
            );
            scaledLines.add(scaledLine);
        }

        return scaledLines;
    }

    @Override
    protected double scale(double number) {
        double value = number * SCALE_BASE * SCALE_MULTIPLIER_1;
        return value * SCALE_MULTIPLIER_2;
    }

    /**
     * Gets all parsed patterns.
     *
     * @return List of all patterns
     */
    public List<GGTPattern> getPatterns() {
        return patterns;
    }

    /**
     * Turkish compatibility method for getPatterns.
     *
     * @return List of all patterns
     */
    public List<GGTPattern> getParcalar() {
        return getPatterns();
    }

    /**
     * Gets a pattern by its ID.
     *
     * @param id The pattern ID to search for
     * @return The pattern with the given ID, or null if not found
     */
    public GGTPattern getPatternById(String id) {
        for (GGTPattern pattern : patterns) {
            if (pattern.getId().equals(id)) {
                return pattern;
            }
        }
        return null;
    }

    /**
     * Turkish compatibility method for getPatternById.
     *
     * @param id The pattern ID to search for
     * @return The pattern with the given ID, or null if not found
     */
    public GGTPattern getParcaById(String id) {
        return getPatternById(id);
    }

    // ==================== Helper Classes ====================

    /**
     * Represents the location of a pattern in the file content.
     */
    private static class PatternLocation {
        final int patternNumber;
        final int start;
        final int end;

        PatternLocation(int patternNumber, int start, int end) {
            this.patternNumber = patternNumber;
            this.start = start;
            this.end = end;
        }
    }
}