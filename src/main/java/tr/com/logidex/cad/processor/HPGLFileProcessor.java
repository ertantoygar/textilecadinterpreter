package tr.com.logidex.cad.processor;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.PieceSequenceNumberCreator;
import tr.com.logidex.cad.model.Lbl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class HPGLFileProcessor extends FileProcessor {
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
        double tmpAngle = 0;
        Point2D tmpPenCurrent = new Point2D(0, 0);
        double tmpOrigin = 0;
        Dimension2D tmpCharSize = new Dimension2D(2, 2);
        Point2D tmpPenTarget = new Point2D(0, 0);
        List<Line> tmpList = new ArrayList<Line>();
        for (String c : commands) { // command list
            // The first two characters are the command.
            //System.out.println(c);
            if (c.trim().isEmpty()) {
                continue;
            }
            String prefix = c.trim().substring(0, 2); // for example LB,DI etc..
            String afterPrefix = c.substring(2); // After the prefix.
            ArrayList<String> params; // Parameters of the command.
            switch (prefix) {
                case "DI": // angle info
                    params = new ArrayList<>(Arrays.asList(afterPrefix.split(",")));
                    if (params.size() < 2) {
                        break;
                    }
                    double dirX = Double.parseDouble(params.get(0));
                    double dirY = Double.parseDouble(params.get(1));
                    tmpAngle = Math.toDegrees(Math.atan2(dirY, dirX));
                    break;
                case "LB": // lbl
                    if (afterPrefix.isEmpty()) {
                        break;
                    }
                    // Satir fazla uzunsa alt satira inmek icin \n ekleyelim.
                    //afterPrefix = CutFile.addNewLineIfTooLong(afterPrefix, CutFile.MAX_CHAR_FOR_A_LINE);
                    Lbl lbl = new Lbl(afterPrefix, tmpPenCurrent, tmpAngle, tmpOrigin, tmpCharSize.getWidth(), tmpCharSize.getHeight());
                    labels.add(lbl);
                    lbl = null;
                    break;
                case "LO": // lbl origin
                    if (afterPrefix.isEmpty()) {
                        break;
                    }
                    tmpOrigin = Double.parseDouble(afterPrefix);
                    break;
                case "PD": // pen down
                    params = new ArrayList<>(Arrays.asList(afterPrefix.split(",")));
                    if (params.size() < 2) {
                        break;
                    }
                    for (int i = 0; i < params.size(); i += 2) {
                        // index 0 and 1 = x1 y1 index 2 and 3 = x2 y2 .. .. ..
                        String strValX = params.get(i);
                        String strValY = params.get(i + 1);
                        double targetX = Double.parseDouble(strValX);
                        double targetY = Double.parseDouble(strValY);
                        double scaledTargetX = scale(targetX);
                        double scaledTargetY = scale(targetY);
                        tmpPenCurrent = new Point2D(scaledTargetX, scaledTargetY);
                        Line line = new Line(tmpPenCurrent.getX(), tmpPenCurrent.getY(), tmpPenTarget.getX(), tmpPenTarget.getY());
                        lines.add(line);
                        tmpList.add(line);
                        tmpPenTarget = tmpPenCurrent;
                    }
                    break;
                case "PU": // pen up
                    if (!tmpList.isEmpty()) {
                        linesForClosedShapes.put(PieceSequenceNumberCreator.getSequenceNumber(),tmpList);
                    }
                    tmpList = new ArrayList<Line>();
                    params = new ArrayList<>(Arrays.asList(afterPrefix.split(",")));
                    if (params.size() < 2) {
                        break;
                    }
                    for (int i = 0; i < params.size(); i += 2) {
                        String strValX = params.get(i);
                        String strValY = params.get(i + 1);
                        double targetX = Double.parseDouble(strValX);
                        double targetY = Double.parseDouble(strValY);
                        double scaledTargetX = scale(targetX);
                        double scaledTargetY = scale(targetY);
                        // current position of pen
                        tmpPenCurrent = new Point2D(scaledTargetX, scaledTargetY);
                        // target position of pen
                        tmpPenTarget = tmpPenCurrent;
                    }
                    break;
                case "SI": // char size
                    params = new ArrayList<>(Arrays.asList(afterPrefix.split(",")));
                    if (params.size() < 2) {
                        break;
                    }
                    double w = Double.parseDouble(params.get(0));
                    double h = Double.parseDouble(params.get(1));
                    tmpCharSize = new Dimension2D(w, h);
                    break;
                default:
                    break;
            }
            prefix = afterPrefix = null;
            params = null;
        }
        ;
        // her PU komutu ile bir path olusmaya basliyor ve tmp listedeki veriler parca
        // olarak source file a ekleniyor.
        // fakat son komut pu olmazsa en son parca listeye eklenmiyor.Listenin son
        // halini parca olarak ekleyelim.
        linesForClosedShapes.put(PieceSequenceNumberCreator.getSequenceNumber(),tmpList);
    }
}
