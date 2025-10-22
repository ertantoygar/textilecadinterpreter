package tr.com.logidex.cad.processor;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.helper.PieceSequenceNumberCreator;
import tr.com.logidex.cad.model.Lbl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Processor for Gerber format CAD files (.CUT, .CAM extensions).
 * Handles M14/M15 (knife down/up) commands and M31 label commands.
 */
public final class GerberFileProcessor extends FileProcessor {

    private static final String LABEL_COMMAND_ATTACHED = "M31";
    private static final String LABEL_COMMAND_INDEPENDENT = "M31";
    private static final String KNIFE_DOWN_COMMAND = "M14";
    private static final String KNIFE_UP_COMMAND = "M15";
    private static final String NEW_PIECE_PREFIX = "N";
    private static final String COORDINATE_PREFIX = "X";
    private static final String PARAMETER_PREFIX = "p";
    private static final double SCALE_FACTOR = 10.0f;

    public GerberFileProcessor(String fileContent) {
        super(fileContent);
    }

    @Override
    protected void initUnwantedChars() {
        UNWANTED_CHARS = Arrays.asList("", null, "\32", "\n", "\r");
    }

    @Override
    protected void initSplitRegex() {
        SPLIT_REGEX = "[\\*]";
    }

    @Override
    protected void interpretCommands() {
        CommandState state = new CommandState();
        List<Line> currentPieceLines = new ArrayList<>();

        for (String instruction : commands) {
            if (instruction.startsWith(PARAMETER_PREFIX)) {
                continue; // Skip parameter settings
            }

            if (instruction.startsWith(NEW_PIECE_PREFIX)) {
                savePieceIfNotEmpty(currentPieceLines);
                currentPieceLines = new ArrayList<>();
                continue;
            }

            processKnifeCommands(instruction, state);
            processAttachedLabel(instruction, state);
            processCoordinateCommand(instruction, state, currentPieceLines);
            processIndependentLabel(instruction, state);
        }

        savePieceIfNotEmpty(currentPieceLines);
    }

    private void processKnifeCommands(String instruction, CommandState state) {
        if (instruction.startsWith(KNIFE_DOWN_COMMAND)) {
            state.knifeDown = true;
        } else if (instruction.startsWith(KNIFE_UP_COMMAND)) {
            state.knifeDown = false;
        }
    }

    private void processAttachedLabel(String instruction, CommandState state) {
        if (state.m31DetectedAttachedToXY) {
            labels.add(new Lbl(instruction, state.currentPosition, 0, 2, 12, 12));
            state.m31DetectedAttachedToXY = false;
        }
    }

    private void processCoordinateCommand(String instruction, CommandState state, List<Line> currentPieceLines) {
        if (!instruction.startsWith(COORDINATE_PREFIX)) {
            return;
        }

        String coordinateData = instruction.substring(1);
        CoordinatePair coords = parseCoordinates(coordinateData);

        if (coords != null) {
            state.currentPosition = coords.toPoint();

            if (coords.hasLabelCommand) {
                state.m31DetectedAttachedToXY = true;
            }

            if (state.knifeDown) {
                Line line = new Line(
                        state.currentPosition.getX(),
                        state.currentPosition.getY(),
                        state.targetPosition.getX(),
                        state.targetPosition.getY()
                );
                lines.add(line);
                currentPieceLines.add(line);
            }

            state.targetPosition = state.currentPosition;
        }
    }

    private void processIndependentLabel(String instruction, CommandState state) {
        if (instruction.equals(LABEL_COMMAND_INDEPENDENT)) {
            state.m31DetectedIndependently = true;
            return;
        }

        if (state.m31DetectedIndependently) {
            state.xyCaughtFor31 = true;
            state.m31DetectedIndependently = false;
            return;
        }

        if (state.xyCaughtFor31) {
            labels.add(new Lbl(instruction, state.currentPosition, 0, 2, 12, 12));
            state.xyCaughtFor31 = false;
        }
    }

    private CoordinatePair parseCoordinates(String data) {
        StringBuilder x = new StringBuilder();
        StringBuilder y = new StringBuilder();
        boolean readingY = false;
        boolean hasM31 = false;

        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);

            if (c == 'Y') {
                readingY = true;
                continue;
            }

            if (c == 'M') {
                String remaining = data.substring(i);
                if (remaining.startsWith("M31")) {
                    hasM31 = true;
                }
                break;
            }

            if (readingY) {
                y.append(c);
            } else {
                x.append(c);
            }
        }

        try {
            double xValue = scale(Float.parseFloat(x.toString()));
            double yValue = scale(Float.parseFloat(y.toString()));
            return new CoordinatePair(xValue, yValue, hasM31);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void savePieceIfNotEmpty(List<Line> pieceLines) {
        if (!pieceLines.isEmpty()) {
            linesForClosedShapes.put(PieceSequenceNumberCreator.getSequenceNumber(), pieceLines);
        }
    }

    @Override
    protected double scale(double number) {
        return number / SCALE_FACTOR;
    }

    // ==================== Helper Classes ====================

    private static class CommandState {
        boolean knifeDown = false;
        boolean m31DetectedAttachedToXY = false;
        boolean m31DetectedIndependently = false;
        boolean xyCaughtFor31 = false;
        Point2D currentPosition = new Point2D(0, 0);
        Point2D targetPosition = new Point2D(0, 0);
    }

    private static class CoordinatePair {
        final double x;
        final double y;
        final boolean hasLabelCommand;

        CoordinatePair(double x, double y, boolean hasLabelCommand) {
            this.x = x;
            this.y = y;
            this.hasLabelCommand = hasLabelCommand;
        }

        Point2D toPoint() {
            return new Point2D(x, y);
        }
    }
}