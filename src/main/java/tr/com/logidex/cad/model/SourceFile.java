package tr.com.logidex.cad.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import tr.com.logidex.cad.processor.FileProcessingException;
import tr.com.logidex.cad.FileExtension;
import tr.com.logidex.cad.Unit;
import tr.com.logidex.cad.processor.FileProcessor;
import tr.com.logidex.cad.processor.GGTFileProcessor;
import tr.com.logidex.cad.processor.GerberFileProcessor;
import tr.com.logidex.cad.processor.HPGLFileProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SourceFile {

    private StringProperty activeFileName = new SimpleStringProperty("-----");

    public SourceFile() {
    }


    public StringProperty activeFileNameProperty() {
        return activeFileName;
    }


    public FileProcessor readAndSendToTheProcessing(File file, FileExtension extension, Unit unit) throws FileProcessingException {

        FileProcessor.unit = unit;

        String fileContent = "";
        try {
            fileContent = readFile(file.getAbsolutePath());
            FileProcessor fileProcessor = null;
            switch (extension) {
                case HPGL:
                case PLT:
                case HPG:
                    fileProcessor = new HPGLFileProcessor(fileContent);
                    break;
                case CUT:
                case CAM:
                    fileProcessor = new GerberFileProcessor(fileContent);
                    break;
                case GGT:
                    fileProcessor = new GGTFileProcessor(fileContent);
                    break;
            }


           fileProcessor.startFileProcessing();
            activeFileName.set(file.getName());
            return fileProcessor;


        } catch (IOException e) {
            throw new FileProcessingException(e.getMessage());
        }


    }

    private String readFile(String absolutePath) throws IOException {

        return Files.readString(Paths.get(absolutePath), StandardCharsets.UTF_8);
    }


}
