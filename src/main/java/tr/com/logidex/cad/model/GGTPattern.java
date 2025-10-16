package tr.com.logidex.cad.model;

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GGTPattern {
    private Integer id;
    private List<Line> lines;
    private Label etiket;
    private final Map<String, Point2D> parcaninEtiketleri = new LinkedHashMap<>();

    public GGTPattern(Integer id) {
        this.id = id;
        this.lines = new ArrayList<>();
        this.etiket = null;
    }

    public Integer getId() {
        return id;
    }

    public void cizgiEkle(Point2D baslangic, Point2D bitis) {
        if (!baslangic.equals(bitis)) {
            Line line = new Line(
                    baslangic.getX(), baslangic.getY(),
                    bitis.getX(), bitis.getY()
            );
            lines.add(line);
        }
    }

    public List<Line> getLines() {
        return lines;
    }

    public Label getLabel() {
        return etiket;

    }

    public void setEtiket(Label label) {
        this.etiket = label;
    }


    public Map<String, Point2D> getParcaninEtiketleri() {
        return parcaninEtiketleri;
    }
}