package tr.com.logidex.cad;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class Util {

    public static void showMessageDialog(Alert.AlertType alertType, Exception throwable) {

        Alert alert = new Alert(alertType);
        alert.setTitle("ERROR");
        alert.setHeaderText(throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getName());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static ImageView createImageViewFromLines(List<Line> lines, double maxWidth, double maxHeight) {
        Pane pane = new Pane();

        Rectangle background = new Rectangle(maxWidth, maxHeight);
        background.setFill(Color.TRANSPARENT);


        CoordinateBounds cb = Util.findMinMaxCoordinates(lines);


        Polygon polygon = new Polygon();
        lines.forEach(line -> {

            double x1 = line.getStartX() - cb.getMinX();
            double y1 = line.getStartY() - cb.getMinY();
            double x2 = line.getEndX() - cb.getMinX();
            double y2 = line.getEndY() - cb.getMinY();
            // Point2D newStartPoint = Util.revertXpoint(x1, y1);
            // Point2D newEndPoint = Util.revertXpoint(x2, y2);


            //  x1 = newStartPoint.getX();
            // y1 = newStartPoint.getY();
            // x2 = newEndPoint.getX();
            //y2 = newEndPoint.getY();
            polygon.getPoints().addAll(x2, y2, x1, y1);
        });


        polygon.setScaleY(-1);
        pane.getChildren().add(polygon);


        // Canvas'tan görüntü (image) oluştur
        WritableImage writableImage = pane.snapshot(null, null);

        // Görüntüyü ImageView içinde göster
        ImageView imageView = new ImageView(writableImage);

        // En-boy oranını koru ve boyutları sınırla
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(maxWidth);
        imageView.setFitHeight(maxHeight);


        return imageView;
    }

    public static CoordinateBounds findMinMaxCoordinates(List<Line> lines) {
        if (lines.isEmpty()) {
            return null;
        }

        double minX = lines.get(0).getStartX();
        double minY = lines.get(0).getStartY();
        double maxX = lines.get(0).getStartX();
        double maxY = lines.get(0).getStartY();

        for (Line line : lines) {
            double startX = line.getStartX();
            double startY = line.getStartY();
            double endX = line.getEndX();
            double endY = line.getEndY();

            minX = Math.min(minX, Math.min(startX, endX));
            minY = Math.min(minY, Math.min(startY, endY));
            maxX = Math.max(maxX, Math.max(startX, endX));
            maxY = Math.max(maxY, Math.max(startY, endY));
        }

        return new CoordinateBounds(minX, minY, maxX, maxY);
    }

    public static Point2D revertXpoint(double x1, double y1) {
        return new Point2D(y1, -x1);
    }

    public static double mmToPixel(double mm) {


        // Ekranın DPI değerini alın
        double dpi = 72;


        // Sabitleri hesaplayın
        double mmToInch = 0.0393700787401575;

        // Resmin boyutunu hesaplayın
        double widthInches = mm * mmToInch;

        double widthPixels = widthInches * dpi;

        return widthPixels;


    }


    public static int linearScale(int input, int inputLower, int inputUpper, int outputLower, int outputUpper) {
        if (inputUpper == inputLower) {
            throw new IllegalArgumentException("inputUpper and inputLower cannot be the same value");
        }
        return (input - inputLower) * (outputUpper - outputLower) / (inputUpper - inputLower) + outputLower;
    }




    public static String formatDecimalPoint(double value, int decimalP) {


        return String.valueOf(String.format("%."+decimalP+"f",value));
    }







    public static double scaleValue(double value, double minOld, double maxOld, double minNew, double maxNew) {
        // Eğer eski aralık sıfır genişliğinde ise (örn: minOld == maxOld), hata fırlatıyoruz.
        if (maxOld == minOld) {
            throw new IllegalArgumentException("Old range cannot have zero width.");
        }

        // Değerin yeni aralığa ölçeklenmesi
        return ((double) (value - minOld) / (maxOld - minOld)) * (maxNew - minNew) + minNew;
    }

    public static double mmToInch(double mm){
        return mm/25.4;
    }

    public static double incToMM(double inches){
        return inches * 25.4;
    }



}
