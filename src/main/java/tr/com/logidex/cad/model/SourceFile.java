package tr.com.logidex.cad.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import tr.com.logidex.cad.*;
import tr.com.logidex.cad.helper.Util;
import tr.com.logidex.cad.processor.FileProcessor;
import tr.com.logidex.cad.processor.GGTFileProcessor;
import tr.com.logidex.cad.processor.GerberFileProcessor;
import tr.com.logidex.cad.processor.HPGLFileProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Manages CAD source files and delegates processing to appropriate file processors
 * based on file extension.
 */
public class SourceFile {

    private static final String DEFAULT_FILE_NAME = "-----";

    private final StringProperty activeFileName;

    public SourceFile() {
        this.activeFileName = new SimpleStringProperty(DEFAULT_FILE_NAME);
    }

    // ==================== Property Accessors ====================

    public StringProperty activeFileNameProperty() {
        return activeFileName;
    }

    public String getActiveFileName() {
        return activeFileName.get();
    }

    // ==================== File Processing ====================

    /**
     * Reads a CAD file and processes it using the appropriate processor based on file extension.
     *
     * @param file The file to process
     * @param extension The file extension type
     * @param unit The unit system to use for processing (IN or MM)
     * @return The FileProcessor that handled the file
     */
    public FileProcessor readAndSendToTheProcessing(File file, FileExtension extension, Unit unit)
            throws Exception {

        validateInputs(file, extension, unit);

        FileProcessor.unit = unit;

        try {
            String fileContent = readFileContent(file);
            FileProcessor processor = createProcessor(extension, fileContent);

            processor.startFileProcessing();
            activeFileName.set(file.getName());

            return processor;

        } catch (IOException e) {
            throw new Exception("Failed to read file: " + file.getName() + " - " + e.getMessage());
        }
    }

    // ==================== Private Helper Methods ====================

    private void validateInputs(File file, FileExtension extension, Unit unit)  throws Exception{
        if (file == null) {
            throw new Exception("File cannot be null");
        }

        if (!file.exists()) {
            throw new Exception("File does not exist: " + file.getAbsolutePath());
        }

        if (!file.canRead()) {
            throw new Exception("File is not readable: " + file.getAbsolutePath());
        }

        if (extension == null) {
            throw new Exception("File extension cannot be null");
        }

        if (unit == null) {
            throw new Exception("Unit cannot be null");
        }
    }

    private String readFileContent(File file) throws IOException {
        return Util.readFile(file.getAbsolutePath(), StandardCharsets.ISO_8859_1);
    }

    /**
     * Creates the appropriate file processor based on the file extension.
     *
     * @param extension The file extension
     * @param fileContent The content of the file
     * @return The appropriate FileProcessor instance
     */
    private FileProcessor createProcessor(FileExtension extension, String fileContent)
            throws Exception {

        return switch (extension) {
            case HPGL,PLT,HPG ->new HPGLFileProcessor(fileContent);
            case CUT,CAM ->new GerberFileProcessor(fileContent);
            case GGT ->new GGTFileProcessor(fileContent);
        };

    }
}