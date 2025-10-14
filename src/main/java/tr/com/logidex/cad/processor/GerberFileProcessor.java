package tr.com.logidex.cad.processor;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.model.Label;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class GerberFileProcessor extends FileProcessor {
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
        /**
         * for example *X3904Y1117M31*S:MS M:BAYAN BLUZ.gemx N:ON SOL F:fabric
         * D:Default*
         */
        boolean m31DetectedAttachedToXYcommand = false;
        boolean knifeDown = false;
        /**
         * for example *M31*X855Y670*BACK BODY*
         */
        boolean m31DetectedIndependently = false;
        boolean xyCaughtFor31 = false;
        Point2D tmpPenCurrent = new Point2D(0, 0);
        double tmpOrigin = 0;
        Dimension2D tmpCharSize = new Dimension2D(2, 2);
        Point2D tmpPenTarget = new Point2D(0, 0);
        List<Line> tmpList = new ArrayList<Line>();
        for (String instruction : commands) {
            if (instruction.startsWith("p")) {//set parameter
                //We do not need to implement.
                continue;
            }
            if (instruction.startsWith("N")) {
                if (!tmpList.isEmpty()) {
                    linesForClosedShapes.put(PieceSequenceNumberCreator.getSequenceNumber(),  tmpList);
                }
                tmpList = new ArrayList<Line>();
            }
            if (instruction.startsWith("M14")) {
                knifeDown = true;
            }
            if (instruction.startsWith("M15")) {
                knifeDown = false;
            }
            if (m31DetectedAttachedToXYcommand) {
                labels.add(new Label(instruction, tmpPenCurrent, 0, 2, 12, 12));
                m31DetectedAttachedToXYcommand = false;
            }
            if (instruction.startsWith("X")) {
                String s = instruction.substring(1);
                String x = "", y = "";
                char[] chars = s.toCharArray();
                int i = 0;
                for (char c : chars) {
                    i++;
                    if (c == 'Y') {
                        y = s.substring(i);
                        break;
                    } else {
                        x += c;
                    }
                }
                if (y.contains("M")) { // y ifadesinden sonra M31 geliyorsa bu bir label dir.
                    if (y.split("M")[1].equals("31")) {
                        m31DetectedAttachedToXYcommand = true;
                    }
                    // remove the "M31" instruction and use the Y pos, because positions are
                    // relative.We shouldn't skip the Y position.
                    y = y.substring(0, y.length() - 3);
                    // TODO detect the labels.
                }
                double xF = Float.parseFloat(x);
                double yF = Float.parseFloat(y);
                xF = scale(xF);
                yF = scale(yF);
                tmpPenCurrent = new Point2D(xF, yF);
                if (knifeDown) {
                    Line line = new Line(tmpPenCurrent.getX(), tmpPenCurrent.getY(), tmpPenTarget.getX(), tmpPenTarget.getY());
                    lines.add(line);
                    tmpList.add(line);
                }
                tmpPenTarget = tmpPenCurrent;
            }
            if (instruction.equals("M31")) {
                // format : *M31*X855Y670*BACK BODY*
                m31DetectedIndependently = true;
                continue; // to read xy positon
            }
            if (m31DetectedIndependently) {
                xyCaughtFor31 = true;
                m31DetectedIndependently = false;
                continue; // to read label string
            }
            if (xyCaughtFor31) {// The xy position of the label was read.
                labels.add(new Label(instruction, tmpPenCurrent, 0, 2, 12, 12));
                xyCaughtFor31 = false;
            }
        }
        linesForClosedShapes.put(PieceSequenceNumberCreator.getSequenceNumber(),tmpList);
    }


    @Override
    protected double scale(double number) {
        return  number / 10f;
    }
}
