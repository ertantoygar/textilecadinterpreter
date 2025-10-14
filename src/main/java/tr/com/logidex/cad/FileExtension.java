package tr.com.logidex.cad;

public enum FileExtension {
    HPGL("HPGL Files","hpgl"),
    PLT("PLT Files","plt"),
    HPG("HPG Files","hpg"),
    CUT("CUT Files","cut"),
    GGT("GGT Files","ggt"),
    CAM("CAM Files","cam");

    private String description;
    private String extension;

    FileExtension(String description, String extension) {
        this.description = description;
        this.extension = extension;
    }

    public String getDescription() {
        return description;
    }

    public String getExtension() {
        return extension;
    }


}
