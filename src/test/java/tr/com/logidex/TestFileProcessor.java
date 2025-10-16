package tr.com.logidex;

import javafx.geometry.Dimension2D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tr.com.logidex.cad.processor.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

public class TestFileProcessor {


    @BeforeEach
    public void setUp() {

        PieceSequenceNumberCreator.resetCounter();
    }

    @Test
    public void testFileProcessorHPGL() throws FileProcessingException, IOException, NoSuchAlgorithmException {

        FileProcessor fileProcessor = new HPGLFileProcessor(Files.readString(Path.of("test.hpgl"), StandardCharsets.UTF_8));
        fileProcessor.startFileProcessing();




        AtomicReference<String> s = new AtomicReference<>("");
        fileProcessor.getPatterns().forEach(closedShape -> {

            s.set(s.get() + closedShape.getLines().toString());


        });

        MessageDigest md = MessageDigest.getInstance("SHA-256");

       String digestString  = "";

        byte[] digest = md.digest(s.get().getBytes());

        for (int i = 0; i < digest.length; i++) {
            digestString += digest[i];
        }

        Assertions.assertEquals("-61-73107-100-100-107-18-21-96-6947-86-63-1113-10-68-117-19-9156-50-23-60892139-113-1005-55-19", digestString);
        Assertions.assertEquals(159, fileProcessor.getSortedAndOptimizedLabels().size());
        Assertions.assertEquals(new Dimension2D(6605.6256,1664.5128), fileProcessor.getDrawingDimensions());

        System.out.println(fileProcessor.getDrawingDimensions());

    }






    @Test
    public void testFileProcessorCUT() throws FileProcessingException, IOException, NoSuchAlgorithmException {

        FileProcessor fileProcessor = new GerberFileProcessor(Files.readString(Path.of("GEMINI.cut"), StandardCharsets.UTF_8));
        fileProcessor.startFileProcessing();




        AtomicReference<String> s = new AtomicReference<>("");
        fileProcessor.getPatterns().forEach(closedShape -> {

            s.set(s.get() + closedShape.getLines().toString());


        });

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        String digestString  = "";

        byte[] digest = md.digest(s.get().getBytes());

        for (int i = 0; i < digest.length; i++) {
            digestString += digest[i];
        }

        Assertions.assertEquals("-476648-36-45-11518-5-51-97-105-101-4-24-7990-96-26-169-216850-103-2828-12128998164-42", digestString);
        Assertions.assertEquals(161, fileProcessor.getSortedAndOptimizedLabels().size());
        Assertions.assertEquals(new Dimension2D(9419.5,1800.0), fileProcessor.getDrawingDimensions());





    }



    @Test
    public void testFileProcessorGGT() throws FileProcessingException, IOException, NoSuchAlgorithmException {

        FileProcessor fileProcessor = new GGTFileProcessor(Files.readString(Path.of("ggttest.ggt"), StandardCharsets.UTF_8));
        fileProcessor.startFileProcessing();




        AtomicReference<String> s = new AtomicReference<>("");
        fileProcessor.getPatterns().forEach(closedShape -> {

            s.set(s.get() + closedShape.getLines().toString());


        });

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        String digestString  = "";

        byte[] digest = md.digest(s.get().getBytes());

        for (int i = 0; i < digest.length; i++) {
            digestString += digest[i];
        }

        Assertions.assertEquals("69-87-126-47-6159-114-12226-44-71-14-128-65-10964-113-10672068-1211428-5-2682-331486-45-125", digestString);
        Assertions.assertEquals(43, fileProcessor.getSortedAndOptimizedLabels().size());
        Assertions.assertEquals(new Dimension2D(2905.252,1400.0480000000002), fileProcessor.getDrawingDimensions());





    }


}
