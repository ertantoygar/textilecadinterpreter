package tr.com.logidex.cad;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import tr.com.logidex.cad.model.ClosedShape;

import java.util.List;
import java.util.stream.Collectors;

public class SvgGenerationService {

    // Sabitler (Zoom, transformasyon ve seçili olma durumu kaldırıldı)
    private static final double DEFAULT_LINE_WIDTH = 2.0; // Sabit çizgi kalınlığı
    private static final double DEFAULT_FILL_ALPHA = 0.3; // Sabit dolgu şeffaflığı
    private static final Color DEFAULT_COLOR = Color.rgb(100, 150, 255, 1.0); // Varsayılan tam renk

    /**
     * Kapalı şekilden ham koordinatları kullanarak SVG <polygon> dizesi oluşturur.
     * Bu metot, zoom, pan veya seçili olma durumu gibi dönüşüm/sunum mantığı İÇERMEZ.
     * * @param shape Çizilecek kapalı şekil nesnesi.
     * @return SVG <polygon> etiketini içeren String.
     */
    public static String generateSvgForShape(ClosedShape shape,double scale,String strokeColorName,String fillColorName) {
        List<Line> lines = shape.getLines();
        if (lines.size() < 3) {
            return ""; // SVG <polygon> için en az üç nokta gereklidir.
        }

        Color color = shape.getColor();
        if (color == null) {
            color = DEFAULT_COLOR;
        }

        // 1. Noktaları Topla ve SVG "points" dizesini oluştur (Ham Koordinatlar Kullanılır)
        // Yalnızca başlangıç noktalarını kullanırız.
        String pointsString = lines.stream()
                .map(line -> String.format("%.2f,%.2f", line.getStartX() * scale, line.getStartY() * scale))
                .collect(Collectors.joining(" "));

        // 2. Stroke (Çizgi) Stilleri
        // Şeklin kendi rengi, tam opaklıkta kullanılır.
        String strokeColor = strokeColorName==null|| strokeColorName.isEmpty()?"gray":strokeColorName;
        double lineWidth = DEFAULT_LINE_WIDTH;

        // 3. Dolgu (Fill) Rengini Hesaplama
        // Şeklin kendi rengi, sabit şeffaflık ile kullanılır.
        String fillColor = fillColorName==null||fillColorName.isEmpty()?"white":fillColorName;

        // 4. SVG <polygon> Etiketini Oluşturma
        return String.format(
                "<polygon " +
                        "points=\"%s\" " +
                        "fill=\"%s\" " +
                        "stroke=\"%s\" " +
                        "stroke-width=\"%.2f\" " +
                        "stroke-linejoin=\"round\" " +
                        "stroke-linecap=\"round\" " +
                        "/>",
                pointsString,
                fillColor,
                strokeColor,
                lineWidth
        );
    }

}