module tr.com.logidex.cad.logidexcadinterpreter {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.graphics;


    opens tr.com.logidex.cad.logidexcadinterpreter to javafx.fxml;
}