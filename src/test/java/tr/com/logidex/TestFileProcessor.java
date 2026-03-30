package tr.com.logidex;

import tr.com.logidex.cad.geometry.Dimension2D;
import tr.com.logidex.cad.geometry.Point2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tr.com.logidex.cad.ColorTheme;
import tr.com.logidex.cad.PlotterScale;
import tr.com.logidex.cad.helper.PieceSequenceNumberCreator;
import tr.com.logidex.cad.processor.FileProcessor;
import tr.com.logidex.cad.processor.GGTFileProcessor;
import tr.com.logidex.cad.processor.GerberFileProcessor;
import tr.com.logidex.cad.processor.HPGLFileProcessor;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestFileProcessor {


    @BeforeEach
    public void setUp() {

        PieceSequenceNumberCreator.resetCounter();
        FileProcessor.unit = null;
        FileProcessor.plotterScale = PlotterScale.DEFAULT;
        FileProcessor.colorTheme = ColorTheme.LIGHT;
        FileProcessor.excludeReferenceSign = false;
        FileProcessor.contourBoundaries = false;
        System.clearProperty("cad.route.strategy");
    }

    @Test
    public void testFileProcessorHPGL() throws Exception {

        FileProcessor fileProcessor = new HPGLFileProcessor(Files.readString(Path.of("test.hpgl"), StandardCharsets.UTF_8));
        fileProcessor.startFileProcessing();




        AtomicReference<String> s = new AtomicReference<>("");
        fileProcessor.getShapes().forEach(closedShape -> {

            s.set(s.get() + closedShape.getLines().toString());


        });

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        String digestString  = "";

        byte[] digest = md.digest(s.get().getBytes());

        for (int i = 0; i < digest.length; i++) {
            digestString += digest[i];
        }

        assertEquals("2776-44-33107-71-19-3-7195-35-1171004124-118-1144-4111175311-4-16-58-2948109-6059", digestString);
        assertEquals(159, fileProcessor.  getSortedAndOptimizedLbls().size());
        assertEquals(new Dimension2D(6605.6256,1664.5128), fileProcessor.drawingDimensions);



        Point2D expected = new Point2D(25.4, 1217.68);
        Point2D actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));

        fileProcessor.invertFlipV();

        expected = new Point2D(25.4, 446.84);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));


        fileProcessor.invertFlipV();

        expected = new Point2D(25.4, 1217.68);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));


        fileProcessor.invertFlipH();

        expected = new Point2D(101.44, 441.13);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        System.out.println("Expected:" + expected);
        System.out.println("Actual:" + actual);
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));


        fileProcessor.invertFlipH();

        expected = new Point2D(25.4, 1217.68);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));

        fileProcessor.invertFlipH();
        fileProcessor.invertFlipV();

        expected = new Point2D(103.82, 106.92);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        System.out.println("Expected:" + expected);
        System.out.println("Actual:" + actual);
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));

        fileProcessor.invertFlipH();
        fileProcessor.invertFlipV();

        expected = new Point2D(25.4, 1217.68);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));





    }






    @Test
    public void testFileProcessorCUT()  throws Exception {

        FileProcessor fileProcessor = new GerberFileProcessor(Files.readString(Path.of("GEMINI.cut"), StandardCharsets.UTF_8));
        fileProcessor.startFileProcessing();




        AtomicReference<String> s = new AtomicReference<>("");
        fileProcessor.getShapes().forEach(closedShape -> {

            s.set(s.get() + closedShape.getLines().toString());


        });

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        String digestString  = "";

        byte[] digest = md.digest(s.get().getBytes());

        for (int i = 0; i < digest.length; i++) {
            digestString += digest[i];
        }

        assertEquals("-89-3435-126017-23-82-778919-3333-4763-103-92-44114628095-5212-5391123-236-60-977", digestString);
        assertEquals(161, fileProcessor.getSortedAndOptimizedLbls().size());
        assertEquals(new Dimension2D(9419.5,1800.0), fileProcessor.drawingDimensions);



        Point2D expected = new Point2D(147.33, 977.52);
        Point2D actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        System.out.println("Expected:" + expected);
        System.out.println("Actual:" + actual);
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));

        fileProcessor.invertFlipV();

        expected = new Point2D(147.33, 822.48);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));


        fileProcessor.invertFlipV();

        expected = new Point2D(147.33, 977.52);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));


        fileProcessor.invertFlipH();

        expected = new Point2D(67.50, 409.95);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        System.out.println("Expected:" + expected);
        System.out.println("Actual:" + actual);
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));


        fileProcessor.invertFlipH();

        expected = new Point2D(147.33, 977.52);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));

        fileProcessor.invertFlipH();
        fileProcessor.invertFlipV();

        expected = new Point2D(67.50, 1390.05);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        System.out.println("Expected:" + expected);
        System.out.println("Actual:" + actual);
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));

        fileProcessor.invertFlipH();
        fileProcessor.invertFlipV();

        expected = new Point2D(147.33, 977.52);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));





    }



    @Test
    public void testFileProcessorGGT()  throws Exception {

        FileProcessor fileProcessor = new GGTFileProcessor(Files.readString(Path.of("ggttest.ggt"), StandardCharsets.UTF_8));
        fileProcessor.startFileProcessing();




        AtomicReference<String> s = new AtomicReference<>("");
        fileProcessor.getShapes().forEach(closedShape -> {

            s.set(s.get() + closedShape.getLines().toString());


        });

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        String digestString  = "";

        byte[] digest = md.digest(s.get().getBytes());

        for (int i = 0; i < digest.length; i++) {
            digestString += digest[i];
        }

        assertEquals("21-1149592109631080-38215-15-55881126-124-29-64-1-6781117-10312520-117-70-31-462482", digestString);
        assertEquals(43, fileProcessor.getSortedAndOptimizedLbls().size());
        assertEquals(new Dimension2D(2905.252,1400.0480000000002), fileProcessor.drawingDimensions);




        Point2D expected = new Point2D(19.94, 96.90);
        Point2D actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        System.out.println("Expected:" + expected);
        System.out.println("Actual:" + actual);
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));

        fileProcessor.invertFlipV();

        expected = new Point2D(19.94, 1263.14);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));


        fileProcessor.invertFlipV();

        expected = new Point2D(19.94, 96.90);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));


        fileProcessor.invertFlipH();

        expected = new Point2D(56.53, 1062.98);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        System.out.println("Expected:" + expected);
        System.out.println("Actual:" + actual);
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));


        fileProcessor.invertFlipH();

        expected = new Point2D(19.94, 96.90);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));

        fileProcessor.invertFlipH();
        fileProcessor.invertFlipV();

        expected = new Point2D(55.36, 1209.98);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        System.out.println("Expected:" + expected);
        System.out.println("Actual:" + actual);
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));

        fileProcessor.invertFlipH();
        fileProcessor.invertFlipV();

        expected = new Point2D(19.94, 96.90);
        actual =fileProcessor.getSortedAndOptimizedLbls().get(1).getPosition();
        assertTrue(arePointsNearlyEqual(expected, actual, 0.01));









    }

    private boolean arePointsNearlyEqual(Point2D p1, Point2D p2, double epsilon) {
        return Math.abs(p1.getX() - p2.getX()) < epsilon &&
                Math.abs(p1.getY() - p2.getY()) < epsilon;
    }


}
