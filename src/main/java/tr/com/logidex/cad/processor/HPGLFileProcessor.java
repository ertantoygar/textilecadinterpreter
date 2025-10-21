package tr.com.logidex.cad.processor;

import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.helper.PieceSequenceNumberCreator;
import tr.com.logidex.cad.model.Lbl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Processor for HPGL format CAD files (.HPGL, .PLT, .HPG extensions).
 * Handles pen up/down commands and label positioning.
 */
public class HPGLFileProcessor extends FileProcessor {

    private static final int COMMAND_PREFIX_LENGTH = 2;
    private static final String COMMAND_DELIMITER = ",";

    // Command prefixes
    private static final String CMD_DIRECTION = "DI";
    private static final String CMD_LABEL = "LB";
    private static final String CMD_LABEL_ORIGIN = "LO";
    private static final String CMD_PEN_DOWN = "PD";
    private static final String CMD_PEN_UP = "PU";
    private static final String CMD_CHAR_SIZE = "SI";

    public HPGLFileProcessor(String fileContent) {
        super(fileContent);
    }

    @Override
    protected void initUnwantedChars() {
        UNWANTED_CHARS = Arrays.asList("", null, "\32", "\n", "\r");
    }

    @Override
    protected void initSplitRegex() {
        SPLIT_REGEX = ";|\\n|\\u0003|\\u000D";
    }

    @Override
    protected void interpretCommands() {
        DrawingState state = new DrawingState();
        List<Line> currentPieceLines = new ArrayList<>();

        for (String command : commands) {
            if (command.trim().isEmpty()) {
                continue;
            }

            CommandParts parts = parseCommand(command);

            switch (parts.prefix) {
                case CMD_DIRECTION:
                    processDirectionCommand(parts.parameters, state);
                    break;

                case CMD_LABEL:
                    processLabelCommand(parts.parameters, state);
                    break;

                case CMD_LABEL_ORIGIN:
                    processLabelOriginCommand(parts.parameters, state);
                    break;

                case CMD_PEN_DOWN:
                    processPenDownCommand(parts.parameters, state, currentPieceLines);
                    break;

                case CMD_PEN_UP:
                    currentPieceLines = processPenUpCommand(parts.parameters, state, currentPieceLines);
                    break;

                case CMD_CHAR_SIZE:
                    processCharSizeCommand(parts.parameters, state);
                    break;

                default:
                    break;
            }
        }

        savePieceIfNotEmpty(currentPieceLines);
    }

    private CommandParts parseCommand(String command) {
        String trimmed = command.trim();
        String prefix = trimmed.substring(0, COMMAND_PREFIX_LENGTH);
        String parameters = trimmed.substring(COMMAND_PREFIX_LENGTH);
        return new CommandParts(prefix, parameters);
    }

    private void processDirectionCommand(String parameters, DrawingState state) {
        List<String> params = parseParameters(parameters);
        if (params.size() < 2) {
            return;
        }

        try {
            double dirX = Double.parseDouble(params.get(0));
            double dirY = Double.parseDouble(params.get(1));
            state.angle = Math.toDegrees(Math.atan2(dirY, dirX));
        } catch (NumberFormatException e) {
            // Skip invalid direction commands
        }
    }

    private void processLabelCommand(String parameters, DrawingState state) {
        if (parameters.isEmpty()) {
            return;
        }

        Lbl lbl = new Lbl(
                parameters,
                state.penCurrent,
                state.angle,
                state.origin,
                state.charSize.getWidth(),
                state.charSize.getHeight()
        );
        labels.add(lbl);
    }

    private void processLabelOriginCommand(String parameters, DrawingState state) {
        if (parameters.isEmpty()) {
            return;
        }

        try {
            state.origin = Double.parseDouble(parameters);
        } catch (NumberFormatException e) {
            // Skip invalid origin commands
        }
    }

    private void processPenDownCommand(String parameters, DrawingState state, List<Line> currentPieceLines) {
        List<String> params = parseParameters(parameters);
        if (params.size() < 2) {
            return;
        }

        for (int i = 0; i < params.size(); i += 2) {
            if (i + 1 >= params.size()) {
                break;
            }

            try {
                double targetX = Double.parseDouble(params.get(i));
                double targetY = Double.parseDouble(params.get(i + 1));
                double scaledTargetX = scale(targetX);
                double scaledTargetY = scale(targetY);

                state.penCurrent = new Point2D(scaledTargetX, scaledTargetY);

                Line line = new Line(
                        state.penCurrent.getX(),
                        state.penCurrent.getY(),
                        state.penTarget.getX(),
                        state.penTarget.getY()
                );
                lines.add(line);
                currentPieceLines.add(line);

                state.penTarget = state.penCurrent;
            } catch (NumberFormatException e) {
                // Skip invalid coordinate pairs
            }
        }
    }

    private List<Line> processPenUpCommand(String parameters, DrawingState state, List<Line> currentPieceLines) {
        savePieceIfNotEmpty(currentPieceLines);
        List<Line> newPieceLines = new ArrayList<>();

        List<String> params = parseParameters(parameters);
        if (params.size() < 2) {
            return newPieceLines;
        }

        for (int i = 0; i < params.size(); i += 2) {
            if (i + 1 >= params.size()) {
                break;
            }

            try {
                double targetX = Double.parseDouble(params.get(i));
                double targetY = Double.parseDouble(params.get(i + 1));
                double scaledTargetX = scale(targetX);
                double scaledTargetY = scale(targetY);

                state.penCurrent = new Point2D(scaledTargetX, scaledTargetY);
                state.penTarget = state.penCurrent;
            } catch (NumberFormatException e) {
                // Skip invalid coordinate pairs
            }
        }

        return newPieceLines;
    }

    private void processCharSizeCommand(String parameters, DrawingState state) {
        List<String> params = parseParameters(parameters);
        if (params.size() < 2) {
            return;
        }

        try {
            double w = Double.parseDouble(params.get(0));
            double h = Double.parseDouble(params.get(1));
            state.charSize = new Dimension2D(w, h);
        } catch (NumberFormatException e) {
            // Skip invalid char size commands
        }
    }

    private List<String> parseParameters(String parameters) {
        if (parameters.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(parameters.split(COMMAND_DELIMITER)));
    }

    private void savePieceIfNotEmpty(List<Line> pieceLines) {
        if (!pieceLines.isEmpty()) {
            linesForClosedShapes.put(PieceSequenceNumberCreator.getSequenceNumber(), pieceLines);
        }
    }

    // ==================== Helper Classes ====================

    private static class DrawingState {
        double angle = 0;
        Point2D penCurrent = new Point2D(0, 0);
        double origin = 0;
        Dimension2D charSize = new Dimension2D(2, 2);
        Point2D penTarget = new Point2D(0, 0);
    }

    private static class CommandParts {
        final String prefix;
        final String parameters;

        CommandParts(String prefix, String parameters) {
            this.prefix = prefix;
            this.parameters = parameters;
        }
    }
}